/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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

import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.Builder;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.Filter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreQueryOptions;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreNativeTypes;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.repository.query.parser.PartTree.OrPart;

/**
 * Name-based query method for Cloud Datastore.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class PartTreeDatastoreQuery<T> extends AbstractDatastoreQuery<T> {

	private final PartTree tree;

	private final DatastorePersistentEntity datastorePersistentEntity;

	private List<Part> filterParts;

	/**
	 * Constructor
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

		List parts = this.tree.getParts().get().collect(Collectors.toList());
		if (parts.size() > 0) {
			if (parts.get(0) instanceof OrPart) {
				throw new DatastoreDataException(
						"Cloud Datastore only supports multiple filters combined with AND.");
			}
			this.filterParts = parts;
		}
		else {
			this.filterParts = Collections.emptyList();
		}
	}

	@Override
	public Object execute(Object[] parameters) {
		Supplier<StructuredQuery.Builder<?>> queryBuilderSupplier = StructuredQuery::newKeyQueryBuilder;
		Function<T, ?> mapper = Function.identity();
		Class returnedType = this.queryMethod.getReturnedObjectType();

		boolean returnedTypeIsNumber = Number.class.isAssignableFrom(returnedType)
				|| returnedType == int.class || returnedType == long.class;

		boolean isCountingQuery = this.tree.isCountProjection()
				|| (this.tree.isDelete() && returnedTypeIsNumber);

		Collector<?, ?, ?> collector = Collectors.toList();
		if (isCountingQuery) {
			collector = Collectors.counting();
		}
		else if (this.tree.isExistsProjection()) {
			collector = Collectors.collectingAndThen(Collectors.counting(), count -> count > 0);
		}
		else if (!returnedTypeIsNumber) {
			queryBuilderSupplier = StructuredQuery::newEntityQueryBuilder;
			mapper = this::processRawObjectForProjection;
		}

		StructuredQuery.Builder<?> structredQueryBuilder = queryBuilderSupplier.get();
		structredQueryBuilder.setKind(this.datastorePersistentEntity.kindName());
		Iterable rawResults = this.datastoreTemplate
				.queryKeysOrEntities(applyQueryBody(parameters, structredQueryBuilder), this.entityType);

		Object result = StreamSupport.stream(rawResults.spliterator(), false).map(mapper)
						.collect(collector);

		if (this.tree.isDelete()) {
			deleteFoundEntities(returnedTypeIsNumber, rawResults);
		}

		return this.tree.isExistsProjection() || isCountingQuery ? result
				: convertResultCollection(result);
	}

	private Object convertResultCollection(Object result) {
		return this.datastoreTemplate.getDatastoreEntityConverter().getConversions()
				.convertOnRead(result, this.queryMethod.getCollectionReturnType(),
						this.queryMethod.getReturnedObjectType());
	}

	private void deleteFoundEntities(boolean returnedTypeIsNumber, Iterable rawResults) {
		if (returnedTypeIsNumber) {
			this.datastoreTemplate.deleteAllById(rawResults, this.entityType);
		}
		else {
			this.datastoreTemplate.deleteAll(rawResults);
		}
	}

	private StructuredQuery applyQueryBody(Object[] parameters,
			StructuredQuery.Builder builder) {
		if (this.tree.hasPredicate()) {
			applySelectWithFilter(parameters, builder);
		}

		Integer limit = null;
		if (this.tree.isExistsProjection()) {
			limit = 1;
		}
		else if (this.tree.isLimiting()) {
			limit = this.tree.getMaxResults();
		}
		this.datastoreTemplate.applyQueryOptions(
				builder, new DatastoreQueryOptions(limit, null, this.tree.getSort()), this.datastorePersistentEntity);
		return builder.build();
	}

	private void applySelectWithFilter(Object[] parameters, Builder builder) {
		Iterator it = Arrays.asList(parameters).iterator();
		Set<String> equalityComparedFields = new HashSet<>();
		Filter[] filters = this.filterParts.stream().map(part -> {
			Filter filter;
			String fieldName = ((DatastorePersistentProperty) this.datastorePersistentEntity
					.getPersistentProperty(part.getProperty().getSegment()))
							.getFieldName();
			try {
				switch (part.getType()) {
				case IS_NULL:
					filter = PropertyFilter.isNull(fieldName);
					break;
				case SIMPLE_PROPERTY:
					filter = PropertyFilter.eq(fieldName,
							DatastoreNativeTypes.wrapValue(it.next()));
					equalityComparedFields.add(fieldName);
					break;
				case GREATER_THAN_EQUAL:
					filter = PropertyFilter.ge(fieldName,
							DatastoreNativeTypes.wrapValue(it.next()));
					break;
				case GREATER_THAN:
					filter = PropertyFilter.gt(fieldName,
							DatastoreNativeTypes.wrapValue(it.next()));
					break;
				case LESS_THAN_EQUAL:
					filter = PropertyFilter.le(fieldName,
							DatastoreNativeTypes.wrapValue(it.next()));
					break;
				case LESS_THAN:
					filter = PropertyFilter.lt(fieldName,
							DatastoreNativeTypes.wrapValue(it.next()));
					break;
				default:
					throw new DatastoreDataException(
							"Unsupported predicate keyword: " + part.getProperty());

				}
				return filter;
			}
			catch (NoSuchElementException e) {
				throw new DatastoreDataException(
						"Too few parameters are provided for query method: "
								+ this.queryMethod.getName());
			}
		}).toArray(Filter[]::new);

		builder.setFilter(
				filters.length > 1
				? CompositeFilter.and(filters[0],
						Arrays.copyOfRange(filters, 1, filters.length))
				: filters[0]);
	}

}
