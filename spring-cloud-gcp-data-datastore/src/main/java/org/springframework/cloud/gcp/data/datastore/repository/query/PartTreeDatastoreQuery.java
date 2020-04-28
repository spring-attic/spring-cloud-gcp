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

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.KeyValue;
import com.google.cloud.datastore.ProjectionEntityQuery;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.Builder;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.Filter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Value;

import org.springframework.cloud.gcp.core.util.MapBuilder;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreQueryOptions;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreResultsIterable;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.repository.query.parser.PartTree.OrPart;
import org.springframework.util.Assert;

import static com.google.cloud.datastore.Query.newEntityQueryBuilder;
import static org.springframework.data.repository.query.parser.Part.Type.GREATER_THAN;
import static org.springframework.data.repository.query.parser.Part.Type.GREATER_THAN_EQUAL;
import static org.springframework.data.repository.query.parser.Part.Type.LESS_THAN;
import static org.springframework.data.repository.query.parser.Part.Type.LESS_THAN_EQUAL;
import static org.springframework.data.repository.query.parser.Part.Type.SIMPLE_PROPERTY;

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

	private final ProjectionFactory projectionFactory;

	private List<Part> filterParts;

	private  static final Map<Part.Type, BiFunction<String, Value, PropertyFilter>> FILTER_FACTORIES =
			new MapBuilder<Part.Type, BiFunction<String, Value, PropertyFilter>>()
					.put(SIMPLE_PROPERTY, PropertyFilter::eq)
					.put(GREATER_THAN_EQUAL, PropertyFilter::ge)
					.put(GREATER_THAN, PropertyFilter::gt)
					.put(LESS_THAN_EQUAL, PropertyFilter::le)
					.put(LESS_THAN, PropertyFilter::lt)
					.build();

	/**
	 * Constructor.
	 * @param queryMethod the metadata for this query method.
	 * @param datastoreTemplate used to execute the given query.
	 * @param datastoreMappingContext used to provide metadata for mapping results to objects.
	 * @param entityType the result domain type.
	 * @param projectionFactory the projection factory that is used to get projection information.
	 */
	public PartTreeDatastoreQuery(DatastoreQueryMethod queryMethod,
			DatastoreTemplate datastoreTemplate,
			DatastoreMappingContext datastoreMappingContext, Class<T> entityType, ProjectionFactory projectionFactory) {
		super(queryMethod, datastoreTemplate, datastoreMappingContext, entityType);
		this.tree = new PartTree(queryMethod.getName(), entityType);
		this.datastorePersistentEntity = this.datastoreMappingContext
				.getPersistentEntity(this.entityType);

		this.projectionFactory = projectionFactory;
		validateAndSetFilterParts();
	}

	private void validateAndSetFilterParts() {
		if (this.tree.isDistinct()) {
			throw new UnsupportedOperationException(
					"Cloud Datastore structured queries do not support the Distinct keyword.");
		}

		List<OrPart> parts = this.tree.get().collect(Collectors.toList());
		if (parts.size() > 0) {
			if (parts.size() > 1) {
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
			ExecutionResult executionResult = (ExecutionResult) execute(parameters, returnedObjectType, List.class,
					false);

			List<?> resultEntries = (List) executionResult.getPayload();

			ParameterAccessor paramAccessor = new ParametersParameterAccessor(getQueryMethod().getParameters(),
					parameters);

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
		if (result == null && ((DatastoreQueryMethod) getQueryMethod()).isOptionalReturnType()) {
			return Optional.empty();
		}
		return result;
	}

	private Object execute(Object[] parameters, Class returnedElementType, Class<?> collectionType, boolean total) {
		Function<T, ?> mapper = Function.identity();

		boolean returnedTypeIsNumber = Number.class.isAssignableFrom(returnedElementType)
				|| returnedElementType == int.class || returnedElementType == long.class;

		boolean isCountingQuery = this.tree.isCountProjection()
				|| (this.tree.isDelete() && returnedTypeIsNumber) || total;

		Collector<?, ?, ?> collector  = Collectors.toList();
		StructuredQuery.Builder<?> structuredQueryBuilder = null;

		if (isCountingQuery && !this.tree.isDelete()) {
			structuredQueryBuilder = StructuredQuery.newKeyQueryBuilder();
			collector = Collectors.counting();
		}
		else if (this.tree.isExistsProjection()) {
			structuredQueryBuilder = StructuredQuery.newKeyQueryBuilder();
			collector = Collectors.collectingAndThen(Collectors.counting(), (count) -> count > 0);
		}
		else if (!returnedTypeIsNumber) {
			structuredQueryBuilder = getEntityOrProjectionQueryBuilder();
			mapper = this::processRawObjectForProjection;
		}

		structuredQueryBuilder =
				structuredQueryBuilder != null ? structuredQueryBuilder : StructuredQuery.newKeyQueryBuilder();

		structuredQueryBuilder.setKind(this.datastorePersistentEntity.kindName());

		boolean singularResult = (!isCountingQuery && collectionType == null) && !this.tree.isDelete();
		DatastoreResultsIterable rawResults = getDatastoreTemplate()
				.queryKeysOrEntities(
						applyQueryBody(parameters, structuredQueryBuilder, total, singularResult, null),
						this.entityType);

		Object result = StreamSupport.stream(rawResults.spliterator(), false).map(mapper).collect(collector);

		if (this.tree.isDelete()) {
			deleteFoundEntities(returnedTypeIsNumber, (Iterable) result);
		}

		if (!this.tree.isExistsProjection() && !isCountingQuery) {
			return new ExecutionResult(convertResultCollection(result, collectionType), rawResults.getCursor());
		}
		else if (isCountingQuery && this.tree.isDelete()) {
			return ((List) result).size();
		}
		else {
			return result;
		}
	}

	/**
	 * In case the return type is a closed projection (only the projection fields are necessary
	 * to construct an entity object), this method returns a {@link ProjectionEntityQuery.Builder}
	 * with projection fields only.
	 * Otherwise returns a {@link EntityQuery.Builder}.
	 */
	private Builder<?> getEntityOrProjectionQueryBuilder() {
		ProjectionInformation projectionInformation =
				this.projectionFactory.getProjectionInformation(this.queryMethod.getReturnedObjectType());

		if (projectionInformation != null &&
				projectionInformation.getType() != this.entityType
				&& projectionInformation.isClosed()) {
			ProjectionEntityQuery.Builder projectionEntityQueryBuilder = Query.newProjectionEntityQueryBuilder();
			projectionInformation.getInputProperties().forEach(propertyDescriptor -> {
				projectionEntityQueryBuilder.addProjection(mapToFieldName(propertyDescriptor));
			});
			return projectionEntityQueryBuilder;
		}
		return StructuredQuery.newEntityQueryBuilder();
	}


	private String mapToFieldName(PropertyDescriptor propertyDescriptor) {
		String name = propertyDescriptor.getName();
		DatastorePersistentProperty persistentProperty =
				(DatastorePersistentProperty) this.datastorePersistentEntity.getPersistentProperty(name);
		return persistentProperty.getFieldName();
	}

	private Slice executeSliceQuery(Object[] parameters) {
		StructuredQuery.Builder builder = getEntityOrProjectionQueryBuilder()
				.setKind(this.datastorePersistentEntity.kindName());
		StructuredQuery query = applyQueryBody(parameters, builder, false, false, null);
		DatastoreResultsIterable<?> resultList = this.datastoreTemplate.queryKeysOrEntities(query, this.entityType);

		ParameterAccessor paramAccessor = new ParametersParameterAccessor(getQueryMethod().getParameters(), parameters);

		Pageable pageable = DatastorePageable.from(paramAccessor.getPageable(), resultList.getCursor(), null);

		EntityQuery.Builder builderNext = newEntityQueryBuilder().setKind(this.datastorePersistentEntity.kindName());
		StructuredQuery queryNext = applyQueryBody(parameters, builderNext, false, true, resultList.getCursor());
		List datastoreResultsList = this.datastoreTemplate.query(queryNext, x -> x);

		List<Object> result =
				StreamSupport.stream(resultList.spliterator(), false).collect(Collectors.toList());

		return (Slice) this.processRawObjectForProjection(
				new SliceImpl(result, pageable, !datastoreResultsList.isEmpty()));
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
			cursorToApply = ((DatastorePageable) pageable).toCursor();
		}
		DatastoreTemplate.applyQueryOptions(
				builder, new DatastoreQueryOptions.Builder().setLimit(limit).setOffset(offset).setSort(sort)
						.setCursor(cursorToApply).build(),
				this.datastorePersistentEntity);
		return builder.build();
	}

	private void applySelectWithFilter(Object[] parameters, Builder builder) {
		Iterator it = Arrays.asList(parameters).iterator();
		Filter[] filters = this.filterParts.stream().map((part) -> {
			//build properties chain for nested properties
			//if the property is not nested, the list would contain only one property
			List<DatastorePersistentProperty> propertiesChain = getPropertiesChain(part);

			String fieldName = propertiesChain.stream().map(DatastorePersistentProperty::getFieldName)
					.collect(Collectors.joining("."));

			if (part.getType() == Part.Type.IS_NULL) {
				return PropertyFilter.isNull(fieldName);
			}

			BiFunction<String, Value, PropertyFilter> filterFactory = FILTER_FACTORIES.get(part.getType());
			if (filterFactory == null) {
				throw new DatastoreDataException("Unsupported predicate keyword: " + part.getType());
			}
			if (!it.hasNext()) {
				throw new DatastoreDataException(
						"Too few parameters are provided for query method: " + getQueryMethod().getName());
			}

			Value convertedValue = convertParam(propertiesChain.get(propertiesChain.size() - 1), it.next());

			return filterFactory.apply(fieldName, convertedValue);
		}).toArray(Filter[]::new);

		builder.setFilter(
				(filters.length > 1)
						? CompositeFilter.and(filters[0], Arrays.copyOfRange(filters, 1, filters.length))
						: filters[0]);
	}

	private List<DatastorePersistentProperty> getPropertiesChain(Part part) {
		Iterable<PropertyPath> iterable = () -> part.getProperty().iterator();

		return StreamSupport.stream(iterable.spliterator(), false)
				.map(propertyPath ->
						this.datastoreMappingContext
								.getPersistentEntity(propertyPath.getOwningType())
								.getPersistentProperty(propertyPath.getSegment())
				).collect(Collectors.toList());
	}

	private Value convertParam(DatastorePersistentProperty persistentProperty, Object val) {
		//persistentProperty.isAssociation() is true if the property is annotated with @Reference,
		// which means that we store keys there
		if (persistentProperty.isAssociation()
				&& this.datastoreMappingContext.hasPersistentEntityFor(val.getClass())) {
			return KeyValue.of(this.datastoreTemplate.getKey(val));
		}
		if (persistentProperty.isIdProperty()) {
			return KeyValue.of(this.datastoreTemplate.createKey(this.datastorePersistentEntity.kindName(), val));
		}
		return this.datastoreTemplate.getDatastoreEntityConverter().getConversions().convertOnWriteSingle(val);
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
