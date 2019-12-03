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

import java.util.function.Consumer;

import com.google.firestore.v1.StructuredQuery;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gcp.data.firestore.FirestoreDataException;
import org.springframework.cloud.gcp.data.firestore.FirestoreTemplate;
import org.springframework.cloud.gcp.data.firestore.User;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreClassMapper;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreDefaultClassMapper;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreMappingContext;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PartTreeFirestoreQueryTests {
	private FirestoreClassMapper classMapper = new FirestoreDefaultClassMapper();

	private static final User DUMMY_USER = new User("Hello", 23);
	private static final Consumer<InvocationOnMock> NOOP = invocation -> { };

	private FirestoreTemplate firestoreTemplate = mock(FirestoreTemplate.class);

	private FirestoreQueryMethod queryMethod = mock(FirestoreQueryMethod.class);

	@Test
	public void testPartTreeQuery() {
		PartTreeFirestoreQuery partTreeFirestoreQuery = createPartTreeQuery("findByAgeAndNameIsNull", invocation -> {
			StructuredQuery.Builder actualBuilder = invocation.getArgument(0);
			Class clazz = invocation.getArgument(1);

			StructuredQuery.Builder builder = StructuredQuery.newBuilder();

			StructuredQuery.CompositeFilter.Builder compositeFilter = StructuredQuery.CompositeFilter.newBuilder();
			compositeFilter.setOp(StructuredQuery.CompositeFilter.Operator.AND);

			StructuredQuery.Filter.Builder filterAge = StructuredQuery.Filter.newBuilder();
			filterAge.getFieldFilterBuilder().setField(StructuredQuery.FieldReference.newBuilder()
					.setFieldPath("age").build())
					.setOp(StructuredQuery.FieldFilter.Operator.EQUAL)
					.setValue(this.classMapper.toFirestoreValue(22));

			compositeFilter.addFilters(filterAge.build());

			StructuredQuery.Filter.Builder filterName = StructuredQuery.Filter.newBuilder();
			filterName.getUnaryFilterBuilder().setField(StructuredQuery.FieldReference.newBuilder()
					.setFieldPath("name").build())
					.setOp(StructuredQuery.UnaryFilter.Operator.IS_NULL);

			compositeFilter.addFilters(filterName.build());

			builder.setWhere(StructuredQuery.Filter.newBuilder().setCompositeFilter(compositeFilter.build()));
			assertThat(actualBuilder.build()).isEqualTo(builder.build());

			assertThat(clazz).isEqualTo(User.class);
		});

		partTreeFirestoreQuery.execute(new Object[]{22});
	}

	@Test
	public void testPartTreeQueryCount() {
		PartTreeFirestoreQuery partTreeFirestoreQuery = setUpPartTreeFirestoreQuery("countByAgeGreaterThan");

		when(this.firestoreTemplate.count(any(), any())).thenAnswer(invocation -> {
			StructuredQuery.Builder actualBuilder = invocation.getArgument(1);
			Class clazz = invocation.getArgument(0);

			StructuredQuery.Builder builder = StructuredQuery.newBuilder();

			StructuredQuery.CompositeFilter.Builder compositeFilter = StructuredQuery.CompositeFilter.newBuilder();
			compositeFilter.setOp(StructuredQuery.CompositeFilter.Operator.AND);

			StructuredQuery.Filter.Builder filterAge = StructuredQuery.Filter.newBuilder();
			filterAge.getFieldFilterBuilder().setField(StructuredQuery.FieldReference.newBuilder()
					.setFieldPath("age").build())
					.setOp(StructuredQuery.FieldFilter.Operator.GREATER_THAN)
					.setValue(this.classMapper.toFirestoreValue(22));

			compositeFilter.addFilters(filterAge.build());
			builder.setWhere(StructuredQuery.Filter.newBuilder().setCompositeFilter(compositeFilter.build()));

			assertThat(actualBuilder.build()).isEqualTo(builder.build());
			assertThat(clazz).isEqualTo(User.class);

			return Mono.just(3L);
		});

		Mono<Long> count = (Mono<Long>) partTreeFirestoreQuery.execute(new Object[] { 22 });
		assertThat(count.block()).isEqualTo(3L);
	}

	@Test
	public void testPartTreeQueryParameterException() {
		PartTreeFirestoreQuery partTreeFirestoreQuery =
				createPartTreeQuery("findByAge");
		assertThatThrownBy(() -> partTreeFirestoreQuery.execute(new Object[] {}))
				.isInstanceOf(FirestoreDataException.class)
				.hasMessage("Too few parameters are provided for query method: findByAge");
	}

	@Test
	public void testPartTreeQueryFilterException() {
		PartTreeFirestoreQuery partTreeFirestoreQuery =
				createPartTreeQuery("findByAgeBetween");
		assertThatThrownBy(() -> partTreeFirestoreQuery.execute(new Object[]{0, 100}))
				.isInstanceOf(FirestoreDataException.class)
				.hasMessage("Unsupported predicate keyword: BETWEEN (2): [IsBetween, Between]");
	}

	@Test
	public void testPartTreeQueryOrException() {
		PartTreeFirestoreQuery partTreeFirestoreQuery =
				createPartTreeQuery("findByAgeOrName");
		assertThatThrownBy(() -> partTreeFirestoreQuery.execute(new Object[]{0, "Abc"}))
				.isInstanceOf(FirestoreDataException.class)
				.hasMessage("Cloud Firestore only supports multiple filters combined with AND.");
	}

	private PartTreeFirestoreQuery createPartTreeQuery(String methodName) {
		return createPartTreeQuery(methodName, NOOP);
	}

	private PartTreeFirestoreQuery createPartTreeQuery(String methodName, Consumer<InvocationOnMock> validator) {
		when(this.firestoreTemplate.execute(any(), any())).thenAnswer(invocation -> {
			validator.accept(invocation);
			return Flux.just(DUMMY_USER);
		});

		return setUpPartTreeFirestoreQuery(methodName);
	}

	private PartTreeFirestoreQuery setUpPartTreeFirestoreQuery(String methodName) {
		Parameters parametersMock = mock(Parameters.class);
		when(parametersMock.isEmpty()).thenReturn(true);
		when(this.queryMethod.getParameters()).thenReturn(parametersMock);

		when(this.queryMethod.getName()).thenReturn(methodName);
		ReturnedType returnedType = mock(ReturnedType.class);
		when(returnedType.getDomainType()).thenAnswer(invocation -> User.class);
		ResultProcessor resultProcessor = mock(ResultProcessor.class);
		when(resultProcessor.getReturnedType()).thenReturn(returnedType);
		when(this.queryMethod.getResultProcessor()).thenReturn(resultProcessor);

		return new PartTreeFirestoreQuery(this.queryMethod,
				this.firestoreTemplate, new FirestoreMappingContext(), this.classMapper);
	}
}
