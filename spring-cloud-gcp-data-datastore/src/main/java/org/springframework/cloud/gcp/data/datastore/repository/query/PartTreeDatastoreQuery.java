/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.datastore.repository.query;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.Builder;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.Filter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreQueryOptions;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreResultsIterable;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.convert.ReadWriteConversions;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.repository.query.parser.PartTree.OrPart;
import org.springframework.util.Assert;

/**
 * Name-based query method for Cloud Datastore.
 *
 * @param <T> the return type of this Query Method
 *
 * @author Chengyuan Zhao
 * @author Dmitry Solomakha
 *
 * @since 1.1
 */
public class PartTreeDatastoreQuery<T> extends AbstractDatastoreQuery<T> {

	private final PartTree tree;

	private final DatastorePersistentEntity datastorePersistentEntity;

	private List<Part> filterParts;

	/**
	 * Constructor.
	 * @param queryMethod the metadata for this query method.
	 * @param datastoreTemplate used to execute the given query.
	 * @param datastoreMappingContext used to provide metadata for mapping results to
	 * objects.
	 * @param entityType the result domain type.
	 */
	public PartTreeDatastoreQuery(DatastoreQueryMethod queryMethod,
			DatastoreTemplate datastoreTemplate,
			DatastoreMappingContext datastoreMappingContext, Class<T> entityType) {
		super(queryMethod, datastoreTemplate, datastoreMappingContext, entityType);
		this.tree = new PartTree(queryMethod.getName(), entityType);
		this.datastorePersistentEntity = this.datastoreMappingContext
				.getPersistentEntity(this.entityType);

		validateAndSetFilterParts();
	}

	private void validateAndSetFilterParts() {
		if (this.tree.isDistinct()) {
			throw new UnsupportedOperationException(
					"Cloud Datastore structured queries do not support the Distinct keyword.");
		}

		List parts = this.tree.get().collect(Collectors.toList());
		if (parts.size() > 0) {
			if (parts.get(0) instanceof OrPart && parts.size() > 1) {
				throw new DatastoreDataException(
						"Cloud Datastore only supports multiple filters combined with AND.");
			}
			this.filterParts = this.tree.getParts().get().collect(Collectors.toList());
		}
		else {
			this.filterParts = Collections.emptyList();
		}
	}

	@Override
	public Object execute(Object[] parameters) {
		Class<?> returnedObjectType = getQueryMethod().getReturnedObjectType();
		if (isPageQuery()) {
			ExecutionResult executionResult =
					(ExecutionResult) execute(parameters, returnedObjectType, List.class, false);

			List<?> resultEntries = (List) executionResult.getPayload();

			ParameterAccessor paramAccessor =
					new ParametersParameterAccessor(getQueryMethod().getParameters(), parameters);

			Pageable pageableParam = paramAccessor.getPageable();

			Long totalCount;
			if (pageableParam instanceof DatastorePageable) {
				Long previousCount = ((DatastorePageable) pageableParam).getTotalCount();
				Assert.notNull(previousCount, "Previous total count can not be null.");

				totalCount = ((DatastorePageable) pageableParam).getTotalCount();
			}
			else {
				totalCount = (Long) execute(parameters, Long.class, null, true);
			}

			Pageable pageable = DatastorePageable.from(pageableParam, executionResult.getCursor(), totalCount);

			return new PageImpl<>(resultEntries, pageable, totalCount);
		}

		if (isSliceQuery()) {
			return executeSliceQuery(parameters);
		}

		Object result = execute(parameters, returnedObjectType,
				((DatastoreQueryMethod) getQueryMethod()).getCollectionReturnType(), false);

		result = result instanceof PartTreeDatastoreQuery.ExecutionResult ? ((ExecutionResult) result).getPayload()
				: result;
		if (result == null) {
			if (((DatastoreQueryMethod) getQueryMethod()).isOptionalReturnType()) {
				return Optional.empty();
			}

			if (!((DatastoreQueryMethod) getQueryMethod()).isNullable()) {
				throw new EmptyResultDataAccessException("Expecting at least 1 result, but none found", 1);
			}
		}
		return result;
	}

	protected boolean isPageQuery() {
		return getQueryMethod().isPageQuery();
	}

	protected boolean isSliceQuery() {
		return getQueryMethod().isSliceQuery();
	}

	private Object execute(Object[] parameters, Class returnedElementType, Class<?> collectionType, boolean total) {
		Supplier<StructuredQuery.Builder<?>> queryBuilderSupplier = StructuredQuery::newKeyQueryBuilder;
		Function<T, ?> mapper = Function.identity();

		boolean returnedTypeIsNumber = Number.class.isAssignableFrom(returnedElementType)
				|| returnedElementType == int.class || returnedElementType == long.class;

		boolean isCountingQuery = this.tree.isCountProjection()
				|| (this.tree.isDelete() && returnedTypeIsNumber) || total;

		Collector<?, ?, ?> collector = Collectors.toList();
		if (isCountingQuery) {
			collector = Collectors.counting();
		}
		else if (this.tree.isExistsProjection()) {
			collector = Collectors.collectingAndThen(Collectors.counting(), (count) -> count > 0);
		}
		else if (!returnedTypeIsNumber) {
			queryBuilderSupplier = StructuredQuery::newEntityQueryBuilder;
			mapper = this::processRawObjectForProjection;
		}

		StructuredQuery.Builder<?> structredQueryBuilder = queryBuilderSupplier.get();
		structredQueryBuilder.setKind(this.datastorePersistentEntity.kindName());

		boolean singularResult = !isCountingQuery && collectionType == null;
		DatastoreResultsIterable rawResults = getDatastoreTemplate()
				.queryKeysOrEntities(
						applyQueryBody(parameters, structredQueryBuilder, total, singularResult, null),
						this.entityType);

		Object result = StreamSupport.stream(rawResults.spliterator(), false).map(mapper).collect(collector);

		if (this.tree.isDelete()) {
			deleteFoundEntities(returnedTypeIsNumber, rawResults);
		}
		boolean countingOrExistsQuery = this.tree.isExistsProjection() || isCountingQuery;
		if (!countingOrExistsQuery) {
			return new ExecutionResult(convertResultCollection(result, collectionType), rawResults.getCursor());
		}
		else {
			return result;
		}
	}

	private Slice executeSliceQuery(Object[] parameters) {
		EntityQuery.Builder builder = StructuredQuery.newEntityQueryBuilder()
				.setKind(this.datastorePersistentEntity.kindName());
		StructuredQuery query = applyQueryBody(parameters, builder, false, false, null);
		DatastoreResultsIterable<?> resultList = this.datastoreTemplate.queryKeysOrEntities(query, this.entityType);

		ParameterAccessor paramAccessor = new ParametersParameterAccessor(getQueryMethod().getParameters(), parameters);

		Pageable pageable = DatastorePageable.from(paramAccessor.getPageable(), resultList.getCursor(), null);

		EntityQuery.Builder builderNext = StructuredQuery.newEntityQueryBuilder()
				.setKind(this.datastorePersistentEntity.kindName());
		StructuredQuery queryNext = applyQueryBody(parameters, builderNext, false, true, resultList.getCursor());
		List datastoreResultsList = this.datastoreTemplate.query(queryNext, x -> x);

		return new SliceImpl(StreamSupport.stream(resultList.spliterator(), false).collect(Collectors.toList()),
				pageable, !datastoreResultsList.isEmpty());
	}

	Object convertResultCollection(Object result, Class<?> collectionType) {
		if (collectionType == null) {
			List list = (List) result;
			return list.isEmpty() ? null : list.get(0);
		}
		return getDatastoreTemplate().getDatastoreEntityConverter().getConversions()
				.convertOnRead(result, collectionType, getQueryMethod().getReturnedObjectType());
	}

	private void deleteFoundEntities(boolean returnedTypeIsNumber, Iterable rawResults) {
		if (returnedTypeIsNumber) {
			getDatastoreTemplate().deleteAllById(rawResults, this.entityType);
		}
		else {
			getDatastoreTemplate().deleteAll(rawResults);
		}
	}

	private StructuredQuery applyQueryBody(Object[] parameters,
			Builder builder, boolean total, boolean singularResult, Cursor cursor) {
		ParameterAccessor paramAccessor = new ParametersParameterAccessor(getQueryMethod().getParameters(), parameters);
		if (this.tree.hasPredicate()) {
			applySelectWithFilter(parameters, builder);
		}

		Pageable pageable = paramAccessor.getPageable();
		Integer limit = null;
		Integer offset = null;
		if (singularResult || this.tree.isExistsProjection()) {
			limit = 1;
		}
		else if (this.tree.isLimiting()) {
			limit = this.tree.getMaxResults();
		}

		if (!singularResult && !total && pageable.isPaged()) {
			limit = pageable.getPageSize();
		}

		Sort sort = this.tree.getSort();
		if (getQueryMethod().getParameters().hasPageableParameter()) {
			sort = sort.and(pageable.getSort());
		}
		if (getQueryMethod().getParameters().hasSortParameter()) {
			sort = sort.and(paramAccessor.getSort());
		}

		if (pageable.isPaged() && !total) {
			offset = (int) pageable.getOffset();
		}

		Cursor cursorToApply = null;
		if (cursor != null) {
			cursorToApply = cursor;
		}
		else if (pageable instanceof DatastorePageable) {
			cursorToApply = ((DatastorePageable) pageable).getCursor();
		}
		DatastoreTemplate.applyQueryOptions(
				builder, new DatastoreQueryOptions.Builder().setLimit(limit).setOffset(offset).setSort(sort)
						.setCursor(cursorToApply).build(),
				this.datastorePersistentEntity);
		return builder.build();
	}

	private void applySelectWithFilter(Object[] parameters, Builder builder) {
		Iterator it = Arrays.asList(parameters).iterator();
		Set<String> equalityComparedFields = new HashSet<>();
		Filter[] filters = this.filterParts.stream().map((part) -> {
			Filter filter;
			String fieldName = ((DatastorePersistentProperty) this.datastorePersistentEntity
					.getPersistentProperty(part.getProperty().getSegment()))
					.getFieldName();
			try {

				ReadWriteConversions converter = this.datastoreTemplate.getDatastoreEntityConverter().getConversions();

				switch (part.getType()) {
					case IS_NULL:
						filter = PropertyFilter.isNull(fieldName);
						break;
					case SIMPLE_PROPERTY:
						filter = PropertyFilter.eq(fieldName,
								converter.convertOnWriteSingle(it.next()));
						equalityComparedFields.add(fieldName);
						break;
					case GREATER_THAN_EQUAL:
						filter = PropertyFilter.ge(fieldName,
								converter.convertOnWriteSingle(it.next()));
						break;
					case GREATER_THAN:
						filter = PropertyFilter.gt(fieldName,
								converter.convertOnWriteSingle(it.next()));
						break;
					case LESS_THAN_EQUAL:
						filter = PropertyFilter.le(fieldName,
								converter.convertOnWriteSingle(it.next()));
						break;
					case LESS_THAN:
						filter = PropertyFilter.lt(fieldName,
								converter.convertOnWriteSingle(it.next()));
						break;
					default:
						throw new DatastoreDataException(
								"Unsupported predicate keyword: " + part.getType());

				}
				return filter;
			}
			catch (NoSuchElementException ex) {
				throw new DatastoreDataException(
						"Too few parameters are provided for query method: "
								+ getQueryMethod().getName());
			}
		}).toArray(Filter[]::new);

		builder.setFilter(
				(filters.length > 1)
						? CompositeFilter.and(filters[0], Arrays.copyOfRange(filters, 1, filters.length))
						: filters[0]);
	}

	private static class ExecutionResult {
		Object payload;
		Cursor cursor;

		ExecutionResult(Object result, Cursor cursor) {
			this.payload = result;
			this.cursor = cursor;
		}

		public Object getPayload() {
			return this.payload;
		}

		public Cursor getCursor() {
			return this.cursor;
		}
	}
}
