/*
 * Copyright 2019-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.firestore.repository.query;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.cloud.firestore.PublicClassMapper;
import com.google.firestore.v1.StructuredQuery;

import org.springframework.cloud.gcp.core.util.MapBuilder;
import org.springframework.cloud.gcp.data.firestore.FirestoreDataException;
import org.springframework.cloud.gcp.data.firestore.FirestoreReactiveOperations;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreMappingContext;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestorePersistentEntity;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestorePersistentProperty;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

import static org.springframework.data.repository.query.parser.Part.Type.GREATER_THAN;
import static org.springframework.data.repository.query.parser.Part.Type.GREATER_THAN_EQUAL;
import static org.springframework.data.repository.query.parser.Part.Type.LESS_THAN;
import static org.springframework.data.repository.query.parser.Part.Type.LESS_THAN_EQUAL;
import static org.springframework.data.repository.query.parser.Part.Type.SIMPLE_PROPERTY;

/**
 * @author Dmitry Solomakha
 * @author Chengyuan Zhao
 *
 * @since 1.2
 */
public class PartTreeFirestoreQuery implements RepositoryQuery {
	private final PartTree tree;

	private final QueryMethod queryMethod;

	private final FirestoreReactiveOperations reactiveOperations;

	private final FirestorePersistentEntity<?> persistentEntity;

	private  static final Map<Part.Type, StructuredQuery.FieldFilter.Operator> PART_TO_FILTER_OP =
			new MapBuilder<Part.Type, StructuredQuery.FieldFilter.Operator>()
					.put(SIMPLE_PROPERTY, StructuredQuery.FieldFilter.Operator.EQUAL)
					.put(GREATER_THAN_EQUAL, StructuredQuery.FieldFilter.Operator.GREATER_THAN_OR_EQUAL)
					.put(GREATER_THAN, StructuredQuery.FieldFilter.Operator.GREATER_THAN)
					.put(LESS_THAN_EQUAL, StructuredQuery.FieldFilter.Operator.LESS_THAN_OR_EQUAL)
					.put(LESS_THAN, StructuredQuery.FieldFilter.Operator.LESS_THAN)
					.build();

	public PartTreeFirestoreQuery(QueryMethod queryMethod, FirestoreReactiveOperations reactiveOperations,
			FirestoreMappingContext mappingContext) {
		this.queryMethod = queryMethod;
		this.reactiveOperations = reactiveOperations;
		ReturnedType returnedType = queryMethod.getResultProcessor().getReturnedType();
		this.tree = new PartTree(queryMethod.getName(), returnedType.getDomainType());
		this.persistentEntity = mappingContext.getPersistentEntity(returnedType.getDomainType());
	}

	@Override
	public Object execute(Object[] parameters) {
		List parts = this.tree.get().collect(Collectors.toList());
		if (parts.size() > 1 && parts.get(0) instanceof PartTree.OrPart) {
				throw new FirestoreDataException(
						"Cloud Firestore only supports multiple filters combined with AND.");
		}

		StructuredQuery.Builder builder = createBuilderWithFilter(parameters);
		return this.reactiveOperations.execute(builder, this.persistentEntity.getType());
	}

	private StructuredQuery.Builder createBuilderWithFilter(Object[] parameters) {
		StructuredQuery.Builder builder = StructuredQuery.newBuilder();

		Iterator it = Arrays.asList(parameters).iterator();

		StructuredQuery.CompositeFilter.Builder compositeFilter = StructuredQuery.CompositeFilter.newBuilder();
		compositeFilter.setOp(StructuredQuery.CompositeFilter.Operator.AND);

		this.tree.getParts().forEach(part -> {
			FirestorePersistentProperty persistentProperty = this.persistentEntity
					.getPersistentProperty(part.getProperty().getSegment());
			StructuredQuery.FieldReference fieldReference = StructuredQuery.FieldReference.newBuilder()
					.setFieldPath(persistentProperty.getName()).build();
			StructuredQuery.Filter.Builder filter = StructuredQuery.Filter.newBuilder();

			if (part.getType() == Part.Type.IS_NULL) {
				filter.getUnaryFilterBuilder().setField(fieldReference)
						.setOp(StructuredQuery.UnaryFilter.Operator.IS_NULL);
			}
			else {
				if (!it.hasNext()) {
					throw new FirestoreDataException(
							"Too few parameters are provided for query method: " + getQueryMethod().getName());
				}
				StructuredQuery.FieldFilter.Operator filterOp = PART_TO_FILTER_OP.get(part.getType());
				if (filterOp == null) {
					throw new FirestoreDataException("Unsupported predicate keyword: " + part.getType());
				}
				filter.getFieldFilterBuilder().setField(fieldReference)
						.setOp(filterOp)
						.setValue(PublicClassMapper.convertToFirestoreValue(it.next()));
			}
			compositeFilter.addFilters(filter.build());
		});

		builder.setWhere(StructuredQuery.Filter.newBuilder().setCompositeFilter(compositeFilter.build()));
		return builder;
	}

	@Override
	public QueryMethod getQueryMethod() {
		return this.queryMethod;
	}
}
