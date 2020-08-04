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

package org.springframework.cloud.gcp.data.firestore;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.firestore.v1.Document.Builder;
import com.google.firestore.v1.DocumentMask;
import com.google.firestore.v1.FirestoreGrpc.FirestoreStub;
import com.google.firestore.v1.GetDocumentRequest;
import com.google.firestore.v1.Precondition.ConditionTypeCase;
import com.google.firestore.v1.RunQueryRequest;
import com.google.firestore.v1.RunQueryResponse;
import com.google.firestore.v1.StructuredQuery;
import com.google.firestore.v1.Value;
import com.google.firestore.v1.WriteRequest;
import com.google.firestore.v1.WriteResponse;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreDefaultClassMapper;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreMappingContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Dmitry Solomakha
 * @since 1.2
 */
public class FirestoreTemplateTests {

	private FirestoreTemplate firestoreTemplate;

	private final FirestoreStub firestoreStub = mock(FirestoreStub.class);

	private static final String parent = "projects/my-project/databases/(default)/documents";

	@Before
	public void setup() {
		this.firestoreTemplate = new FirestoreTemplate(this.firestoreStub, this.parent,
				new FirestoreDefaultClassMapper(), new FirestoreMappingContext());
	}

	@Test
	public void excludeStreamTokensTest() {
		firestoreTemplate.setUsingStreamTokens(false);

		WriteResponse writeResponse =
				WriteResponse.newBuilder()
						.setStreamId("test-stream")
						.setStreamToken(ByteString.copyFromUtf8("the-token"))
						.build();

		WriteRequest request = firestoreTemplate.buildWriteRequest(
				Arrays.asList(new TestEntity("e1", 100L), new TestEntity(null, 10L)), writeResponse);

		assertThat(request.getWrites(0).getCurrentDocument().getConditionTypeCase())
						.isEqualTo(ConditionTypeCase.CONDITIONTYPE_NOT_SET);
		assertThat(request.getWrites(1).getCurrentDocument().getConditionTypeCase())
						.isEqualTo(ConditionTypeCase.EXISTS);
		assertThat(request.getWrites(1).getCurrentDocument().getExists()).isFalse();
		assertThat(request.getWrites(1).getUpdate().getName()).matches(".*/testEntities/[a-zA-Z0-9]{20}$");

		assertThat(request.getStreamId()).isEmpty();
		assertThat(request.getStreamToken().toStringUtf8()).isEmpty();

		request = firestoreTemplate.buildDeleteRequest(Arrays.asList("hello"), writeResponse);
		assertThat(request.getStreamId()).isEmpty();
		assertThat(request.getStreamToken().toStringUtf8()).isEmpty();
	}

	@Test
	public void includeStreamTokensTest() {
		WriteResponse writeResponse =
				WriteResponse.newBuilder()
						.setStreamId("test-stream")
						.setStreamToken(ByteString.copyFromUtf8("the-token"))
						.build();

		WriteRequest request = firestoreTemplate.buildWriteRequest(
				Arrays.asList(new TestEntity("e1", 100L)), writeResponse);
		assertThat(request.getStreamId()).isEqualTo("test-stream");
		assertThat(request.getStreamToken().toStringUtf8()).isEqualTo("the-token");

		request = firestoreTemplate.buildDeleteRequest(Arrays.asList("hello"), writeResponse);
		assertThat(request.getStreamId()).isEqualTo("test-stream");
		assertThat(request.getStreamToken().toStringUtf8()).isEqualTo("the-token");
	}

	@Test
	public void testIntegerId() {

		assertThatThrownBy(() -> {
			List<TestEntityIntegerId> entities =
							Collections.singletonList(new TestEntityIntegerId(1, "abc"));
			firestoreTemplate.buildWriteRequest(entities, WriteResponse.newBuilder().build());
		})
						.isInstanceOf(FirestoreDataException.class)
						.hasMessageContaining("An ID property is expected to be of String type; was class java.lang.Integer");
	}

	@Test
	public void findAllTest() {
		mockRunQueryMethod();

		StepVerifier.create(this.firestoreTemplate.findAll(TestEntity.class))
				.expectNext(new TestEntity("e1", 100L), new TestEntity("e2", 200L))
				.verifyComplete();

		StructuredQuery structuredQuery = StructuredQuery.newBuilder()
				.addFrom(StructuredQuery.CollectionSelector.newBuilder().setCollectionId("testEntities").build())
				.build();
		RunQueryRequest request = RunQueryRequest.newBuilder()
				.setParent(this.parent)
				.setStructuredQuery(structuredQuery)
				.build();

		verify(this.firestoreStub, times(1)).runQuery(eq(request), any());
		verify(this.firestoreStub, times(1)).runQuery(any(), any());
	}

	@Test
	public void findByIdTest() {
		doAnswer(invocation -> {
			StreamObserver<com.google.firestore.v1.Document> streamObserver = invocation.getArgument(1);
			streamObserver.onNext(buildDocument("e1", 100L));

			streamObserver.onCompleted();
			return null;
		}).when(this.firestoreStub).getDocument(any(), any());

		StepVerifier.create(this.firestoreTemplate.findById(Mono.just("e1"), TestEntity.class))
				.expectNext(new TestEntity("e1", 100L))
				.verifyComplete();

		GetDocumentRequest request = GetDocumentRequest.newBuilder()
				.setName(this.parent + "/testEntities/" + "e1")
				.build();

		verify(this.firestoreStub, times(1)).getDocument(eq(request), any());
	}

	@Test
	public void findByIdErrorTest() {
		doAnswer(invocation -> {
			StreamObserver<com.google.firestore.v1.Document> streamObserver = invocation.getArgument(1);
			streamObserver.onError(new RuntimeException("Firestore error"));
			return null;
		}).when(this.firestoreStub).getDocument(any(), any());

		StepVerifier.create(this.firestoreTemplate.findById(Mono.just("e1"), TestEntity.class))
				.expectErrorMatches(e ->
						e instanceof FirestoreDataException
						&& e.getMessage().contains("Firestore error")
						&& e.getMessage().contains("Error while reading entries by id"))
				.verify();

		GetDocumentRequest request = GetDocumentRequest.newBuilder()
				.setName(this.parent + "/testEntities/" + "e1")
				.build();

		verify(this.firestoreStub, times(1)).getDocument(eq(request), any());
	}

	@Test
	public void findByIdNotFoundTest() {
		doAnswer(invocation -> {
			StreamObserver<com.google.firestore.v1.Document> streamObserver = invocation.getArgument(1);
			streamObserver.onError(new RuntimeException("NOT_FOUND: Document"));

			streamObserver.onCompleted();
			return null;
		}).when(this.firestoreStub).getDocument(any(), any());

		StepVerifier.create(this.firestoreTemplate.findById(Mono.just("e1"), TestEntity.class)).verifyComplete();

		GetDocumentRequest request = GetDocumentRequest.newBuilder()
				.setName(this.parent + "/testEntities/" + "e1")
				.build();

		verify(this.firestoreStub, times(1)).getDocument(eq(request), any());
	}

	@Test
	public void findAllByIdTest() {
		GetDocumentRequest request1 = GetDocumentRequest.newBuilder()
				.setName(this.parent + "/testEntities/e1")
				.build();

		GetDocumentRequest request2 = GetDocumentRequest.newBuilder()
				.setName(this.parent + "/testEntities/e2")
				.build();

		GetDocumentRequest request3 = GetDocumentRequest.newBuilder()
				.setName(this.parent + "/testEntities/e3")
				.build();

		doAnswer(invocation -> {
			StreamObserver<com.google.firestore.v1.Document> streamObserver = invocation.getArgument(1);
			streamObserver.onNext(buildDocument("e1", 100L));

			streamObserver.onCompleted();
			return null;
		}).when(this.firestoreStub).getDocument(eq(request1), any());

		doAnswer(invocation -> {
			StreamObserver<com.google.firestore.v1.Document> streamObserver = invocation.getArgument(1);
			streamObserver.onNext(buildDocument("e2", 200L));

			streamObserver.onCompleted();
			return null;
		}).when(this.firestoreStub).getDocument(eq(request2), any());

		doAnswer(invocation -> {
			StreamObserver<com.google.firestore.v1.Document> streamObserver = invocation.getArgument(1);
			streamObserver.onError(new RuntimeException("NOT_FOUND: Document"));

			streamObserver.onCompleted();
			return null;
		}).when(this.firestoreStub).getDocument(eq(request3), any());

		StepVerifier.create(this.firestoreTemplate.findAllById(Flux.just("e1", "e2"), TestEntity.class))
				.expectNext(new TestEntity("e1", 100L), new TestEntity("e2", 200L))
				.verifyComplete();

		verify(this.firestoreStub, times(1)).getDocument(eq(request1), any());
	}

	@Test
	public void countTest() {
		mockRunQueryMethod();

		StepVerifier.create(this.firestoreTemplate.count(TestEntity.class)).expectNext(2L).verifyComplete();

		StructuredQuery structuredQuery = StructuredQuery.newBuilder()
				.addFrom(
						StructuredQuery.CollectionSelector.newBuilder()
								.setCollectionId("testEntities").build())
				.setSelect(
						StructuredQuery.Projection.newBuilder()
						.addFields(StructuredQuery.FieldReference.newBuilder().setFieldPath("__name__").build())
						.build())
				.build();
		RunQueryRequest request = RunQueryRequest.newBuilder()
				.setParent(this.parent)
				.setStructuredQuery(structuredQuery)
				.build();

		verify(this.firestoreStub, times(1)).runQuery(eq(request), any());
		verify(this.firestoreStub, times(1)).runQuery(any(), any());
	}

	@Test
	public void countWithQueryTest() {
		mockRunQueryMethod();

		StructuredQuery.Builder builder = StructuredQuery.newBuilder();
		addWhere(builder);

		StepVerifier.create(this.firestoreTemplate.count(TestEntity.class, builder)).expectNext(2L).verifyComplete();

		StructuredQuery.Builder expectedBuilder = StructuredQuery.newBuilder()
				.addFrom(
						StructuredQuery.CollectionSelector.newBuilder()
								.setCollectionId("testEntities").build())
				.setSelect(
						StructuredQuery.Projection.newBuilder()
								.addFields(StructuredQuery.FieldReference.newBuilder().setFieldPath("__name__").build())
								.build());
		addWhere(expectedBuilder);

		RunQueryRequest request = RunQueryRequest.newBuilder()
				.setParent(this.parent)
				.setStructuredQuery(expectedBuilder)
				.build();

		verify(this.firestoreStub, times(1)).runQuery(eq(request), any());
		verify(this.firestoreStub, times(1)).runQuery(any(), any());
	}

	private void addWhere(StructuredQuery.Builder builder) {
		StructuredQuery.CompositeFilter.Builder compositeFilter = StructuredQuery.CompositeFilter.newBuilder();
		compositeFilter.setOp(StructuredQuery.CompositeFilter.Operator.AND);
		StructuredQuery.Filter.Builder filter = StructuredQuery.Filter.newBuilder();
		StructuredQuery.FieldReference fieldReference = StructuredQuery.FieldReference.newBuilder()
				.setFieldPath("field_path").build();
		filter.getUnaryFilterBuilder().setField(fieldReference)
				.setOp(StructuredQuery.UnaryFilter.Operator.IS_NULL);
		compositeFilter.addFilters(filter.build());
		builder.setWhere(StructuredQuery.Filter.newBuilder().setCompositeFilter(compositeFilter.build()));
	}

	@Test
	public void existsByIdTest() {
		GetDocumentRequest request = GetDocumentRequest.newBuilder()
				.setName(this.parent + "/testEntities/" + "e1")
				.setMask(DocumentMask.newBuilder().addFieldPaths("__name__").build())
				.build();

		doAnswer(invocation -> {
			StreamObserver<com.google.firestore.v1.Document> streamObserver = invocation.getArgument(1);
			streamObserver.onNext(buildDocument("e1", 100L));

			streamObserver.onCompleted();
			return null;
		}).when(this.firestoreStub).getDocument(eq(request), any());

		StepVerifier.create(this.firestoreTemplate.existsById(Mono.just("e1"), TestEntity.class))
				.expectNext(Boolean.TRUE).verifyComplete();

		verify(this.firestoreStub, times(1)).getDocument(eq(request), any());
		verify(this.firestoreStub, times(1)).getDocument(any(), any());
	}

	@Test
	public void existsByIdNotFoundTest() {
		GetDocumentRequest request = GetDocumentRequest.newBuilder()
				.setName(this.parent + "/testEntities/" + "e1")
				.setMask(DocumentMask.newBuilder().addFieldPaths("__name__").build())
				.build();

		doAnswer(invocation -> {
			StreamObserver<com.google.firestore.v1.Document> streamObserver = invocation.getArgument(1);
			streamObserver.onError(new RuntimeException("NOT_FOUND: Document"));

			streamObserver.onCompleted();
			return null;
		}).when(this.firestoreStub).getDocument(eq(request), any());

		StepVerifier.create(this.firestoreTemplate.existsById(Mono.just("e1"), TestEntity.class))
				.expectNext(Boolean.FALSE).verifyComplete();

		verify(this.firestoreStub, times(1)).getDocument(eq(request), any());
		verify(this.firestoreStub, times(1)).getDocument(any(), any());

	}

	private static Map<String, Value> createValuesMap(String test_entity_1, long value) {
		Map<String, Value> valuesMap = new HashMap<>();
		valuesMap.put("longField", Value.newBuilder().setIntegerValue(value).build());
		return valuesMap;
	}

	public static com.google.firestore.v1.Document buildDocument(String name, long l) {
		Builder documentBuilder = com.google.firestore.v1.Document.newBuilder();
		if (name != null) {
			documentBuilder.setName(parent + "/testEntities/" + name);
		}
		return documentBuilder
				.putAllFields(createValuesMap(name, l)).build();
	}

	private void mockRunQueryMethod() {
		doAnswer(invocation -> {
			StreamObserver<RunQueryResponse> streamObserver = invocation.getArgument(1);
			streamObserver.onNext(RunQueryResponse.newBuilder()
					.setDocument(buildDocument("e1", 100L)).build());

			streamObserver.onNext(RunQueryResponse.newBuilder()
					.setDocument(buildDocument("e2", 200L)).build());

			streamObserver.onCompleted();
			return null;
		}).when(this.firestoreStub).runQuery(any(), any());
	}

	@Document(collectionName = "testEntities")
	public static class TestEntity {
		@DocumentId
		String idField;

		Long longField;

		TestEntity() {
		}

		public TestEntity(String idField, Long longField) {
			this.idField = idField;
			this.longField = longField;
		}

		public String getIdField() {
			return this.idField;
		}

		public void setIdField(String idField) {
			this.idField = idField;
		}

		public Long getLongField() {
			return this.longField;
		}

		public void setLongField(Long longField) {
			this.longField = longField;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			TestEntity that = (TestEntity) o;
			return Objects.equals(getIdField(), that.getIdField()) &&
					Objects.equals(getLongField(), that.getLongField());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getIdField(), getLongField());
		}
	}

	@Document
	class TestEntityIntegerId {
		@DocumentId
		public Integer id;

		public String value;

		TestEntityIntegerId(Integer id, String value) {
			this.id = id;
			this.value = value;
		}
	}
}

