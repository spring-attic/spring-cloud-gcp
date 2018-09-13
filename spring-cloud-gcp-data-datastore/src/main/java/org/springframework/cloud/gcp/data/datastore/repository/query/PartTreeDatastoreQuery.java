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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.datastore.ProjectionEntityQuery;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.Builder;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.Filter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreOperations;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreNativeTypes;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.repository.query.QueryMethod;
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
	 * @param datastoreOperations used to execute the given query.
	 * @param datastoreMappingContext used to provide metadata for mapping results to
	 * objects.
	 * @param entityType the result domain type.
	 * @param runAsProjectionQuery if this query  method should be performed as a Cloud Datastore
	 * Projection query. Regardless of this setting the method will be treated as a Projection query
	 * if the Distinct keyword appears in the query method's name.
	 */
	public PartTreeDatastoreQuery(QueryMethod queryMethod,
			DatastoreOperations datastoreOperations,
			DatastoreMappingContext datastoreMappingContext, Class<T> entityType,
			boolean runAsProjectionQuery) {
		super(queryMethod, datastoreOperations, datastoreMappingContext, entityType,
				runAsProjectionQuery);
		this.tree = new PartTree(queryMethod.getName(), entityType);
		this.datastorePersistentEntity = this.datastoreMappingContext
				.getPersistentEntity(this.entityType);

		validateAndSetFilterParts();
	}

	private void validateAndSetFilterParts() {
		if (this.tree.isDelete()) {
			throw new UnsupportedOperationException(
					"Delete queries are not supported in Cloud Datastore: "
							+ this.queryMethod.getName());
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
		List<T> results = executeRawResult(parameters);
		if (this.tree.isCountProjection()) {
			return results.size();
		}
		else if (this.tree.isExistsProjection()) {
			return !results.isEmpty();
		}
		else {
			return applyProjection(results);
		}
	}

	@Override
	protected List<T> executeRawResult(Object[] parameters) {
		Iterable<T> found = this.datastoreOperations.query(getQuery(parameters),
				this.entityType);
		return found == null ? Collections.emptyList()
				: StreamSupport.stream(found.spliterator(), false)
						.collect(Collectors.toList());
	}

	private StructuredQuery getQuery(Object[] parameters) {
		StructuredQuery.Builder structredQueryBuilder;

		if (this.tree.isExistsProjection() || this.tree.isCountProjection()) {
			structredQueryBuilder = StructuredQuery.newKeyQueryBuilder();
		}
		else if (!this.runAsProjectionQuery && !this.tree.isDistinct()) {
			structredQueryBuilder = StructuredQuery.newEntityQueryBuilder();
		}
		else {
			structredQueryBuilder = StructuredQuery.newProjectionEntityQueryBuilder();
		}
		structredQueryBuilder.setKind(this.datastorePersistentEntity.kindName());

		return applyQueryBody(parameters, structredQueryBuilder);
	}

	private StructuredQuery applyQueryBody(Object[] parameters,
			StructuredQuery.Builder builder) {
		if (this.tree.hasPredicate()) {
			applySelectWithFilter(parameters, builder);
		}

		if (!this.tree.getSort().isUnsorted()) {
			applySort(builder);
		}

		if (this.tree.isExistsProjection()) {
			builder.setLimit(1);
		}
		else if (this.tree.isLimiting()) {
			builder.setLimit(this.tree.getMaxResults());
		}

		return builder.build();
	}

	private void applySort(Builder builder) {
		this.tree.getSort().get().forEach(sort -> {
			String fieldName = ((DatastorePersistentProperty) this.datastorePersistentEntity
					.getPersistentProperty(sort.getProperty())).getFieldName();
			builder.addOrderBy(sort.isAscending() ? OrderBy.asc(fieldName)
					: OrderBy.desc(fieldName));
		});
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

		if (builder instanceof ProjectionEntityQuery.Builder) {
			ProjectionEntityQuery.Builder projectionBuilder = (ProjectionEntityQuery.Builder) builder;
			this.datastorePersistentEntity.doWithProperties(
					(PropertyHandler<DatastorePersistentProperty>) datastorePersistentProperty -> {
						String fieldName = datastorePersistentProperty.getFieldName();
						if (!equalityComparedFields.contains(fieldName)) {
							projectionBuilder.addProjection(fieldName);
							if (this.tree.isDistinct()) {
								projectionBuilder.addDistinctOn(fieldName);
							}
						}
					});
		}

		builder.setFilter(
				filters.length > 1
				? CompositeFilter.and(filters[0],
						Arrays.copyOfRange(filters, 1, filters.length))
				: filters[0]);
	}

}
