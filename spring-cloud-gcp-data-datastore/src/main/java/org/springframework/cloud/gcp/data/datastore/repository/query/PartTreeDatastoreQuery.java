/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.Builder;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.Filter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.common.annotations.VisibleForTesting;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreQueryOptions;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.convert.ReadWriteConversions;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
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
			List<?> resultEntries = (List) execute(parameters, returnedObjectType, List.class, false);
			Long totalCount = (Long) execute(parameters, Long.class, null, true);

			ParameterAccessor paramAccessor =
					new ParametersParameterAccessor(getQueryMethod().getParameters(), parameters);

			return new PageImpl<>(resultEntries, paramAccessor.getPageable(), totalCount);
		}

		if (isSliceQuery()) {
			return executeSliceQuery(parameters);
		}

		return execute(parameters, returnedObjectType,
				((DatastoreQueryMethod) getQueryMethod()).getCollectionReturnType(), false);
	}

	@VisibleForTesting
	protected boolean isPageQuery() {
		return getQueryMethod().isPageQuery();
	}

	@VisibleForTesting
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

		Iterable rawResults = getDatastoreTemplate()
				.queryKeysOrEntities(applyQueryBody(parameters, structredQueryBuilder, total), this.entityType);

		Object result = StreamSupport.stream(rawResults.spliterator(), false).map(mapper).collect(collector);

		if (this.tree.isDelete()) {
			deleteFoundEntities(returnedTypeIsNumber, rawResults);
		}

		return (this.tree.isExistsProjection() || isCountingQuery) ? result
				: convertResultCollection(result, collectionType);
	}

	private Slice executeSliceQuery(Object[] parameters) {
		EntityQuery.Builder builder = StructuredQuery.newEntityQueryBuilder()
				.setKind(this.datastorePersistentEntity.kindName());
		StructuredQuery query = applyQueryBody(parameters, builder, false);
		List items = this.datastoreTemplate.query((query), (x) -> x);
		Integer limit = (query.getLimit() == null) ? null : query.getLimit() - 1;

		boolean exceedsLimit = false;
		if (limit != null) {
			//for slice queries we retrieve one additional item to check if the next slice exists
			//the additional item will not be converted on read
			exceedsLimit = items.size() > limit;
			if (exceedsLimit) {
				items = items.subList(0, limit);
			}
		}

		ParameterAccessor paramAccessor = new ParametersParameterAccessor(getQueryMethod().getParameters(), parameters);
		Pageable pageable = paramAccessor.getPageable();
		List entities = (List) this.datastoreTemplate
				.convertEntitiesForRead(items.iterator(), this.entityType).stream()
				.map((o) -> this.processRawObjectForProjection((T) o)).collect(Collectors.toList());
		return new SliceImpl(entities, pageable, exceedsLimit);
	}

	@VisibleForTesting
	protected Object convertResultCollection(Object result, Class<?> collectionType) {
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
			StructuredQuery.Builder builder, boolean total) {
		ParameterAccessor paramAccessor = new ParametersParameterAccessor(getQueryMethod().getParameters(), parameters);
		if (this.tree.hasPredicate()) {
			applySelectWithFilter(parameters, builder);
		}

		Integer limit = null;
		Integer offset = null;
		if (this.tree.isExistsProjection()) {
			limit = 1;
		}
		else if (this.tree.isLimiting()) {
			limit = this.tree.getMaxResults();
		}

		Sort sort = this.tree.getSort();
		if (getQueryMethod().getParameters().hasPageableParameter()) {
			sort = sort.and(paramAccessor.getPageable().getSort());
		}
		if (getQueryMethod().getParameters().hasSortParameter()) {
			sort = sort.and(paramAccessor.getSort());
		}

		if (paramAccessor.getPageable().isPaged() && !total) {
			//for slice queries we retrieve one additional item to check if the next slice exists
			limit = paramAccessor.getPageable().getPageSize() + (isSliceQuery() ? 1 : 0);
			offset = (int) paramAccessor.getPageable().getOffset();
		}

		DatastoreTemplate.applyQueryOptions(
				builder, new DatastoreQueryOptions(limit, offset, sort), this.datastorePersistentEntity);
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
				? CompositeFilter.and(filters[0],
						Arrays.copyOfRange(filters, 1, filters.length))
				: filters[0]);
	}
}
