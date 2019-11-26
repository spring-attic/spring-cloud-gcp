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

package org.springframework.cloud.gcp.data.firestore.transaction;

import java.time.Duration;

import com.google.firestore.v1.BeginTransactionResponse;
import com.google.firestore.v1.CommitRequest;
import com.google.firestore.v1.CommitResponse;
import com.google.firestore.v1.Document;
import com.google.firestore.v1.FirestoreGrpc;
import com.google.firestore.v1.GetDocumentRequest;
import com.google.firestore.v1.RollbackRequest;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.gcp.data.firestore.FirestoreDataException;
import org.springframework.cloud.gcp.data.firestore.FirestoreTemplate;
import org.springframework.cloud.gcp.data.firestore.FirestoreTemplateTests;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreDefaultClassMapper;
import org.springframework.transaction.reactive.TransactionalOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class ReactiveFirestoreTransactionManagerTest {

	private final FirestoreGrpc.FirestoreStub firestoreStub = mock(FirestoreGrpc.FirestoreStub.class);

	private final String parent = "projects/my-project/databases/(default)/documents";

	@Test
	public void triggerCommitCorrectly() {

		FirestoreTemplate template = getFirestoreTemplate();

		ReactiveFirestoreTransactionManager txManager = new ReactiveFirestoreTransactionManager(this.firestoreStub, this.parent);
		TransactionalOperator operator = TransactionalOperator.create(txManager);

		template.findById(Mono.just("e1"), FirestoreTemplateTests.TestEntity.class)
				.concatWith(template.findById(Mono.just("e2"), FirestoreTemplateTests.TestEntity.class))
				.as(operator::transactional)
				.as(StepVerifier::create)
				.expectNext(
						new FirestoreTemplateTests.TestEntity("e1", 100L),
						new FirestoreTemplateTests.TestEntity("e2", 100L))
				.verifyComplete();


		verify(this.firestoreStub).beginTransaction(any(), any());
		verify(this.firestoreStub).commit(any(), any());

		GetDocumentRequest request1 = GetDocumentRequest.newBuilder()
				.setName(this.parent + "/testEntities/" + "e1")
				.setTransaction(ByteString.copyFromUtf8("transaction1"))
				.build();
		verify(this.firestoreStub, times(1)).getDocument(eq(request1), any());

		GetDocumentRequest request2 = GetDocumentRequest.newBuilder()
				.setName(this.parent + "/testEntities/" + "e2")
				.setTransaction(ByteString.copyFromUtf8("transaction1"))
				.build();
		verify(this.firestoreStub, times(1)).getDocument(eq(request2), any());
	}

	@Test
	public void triggerRollbackCorrectly() {

		FirestoreTemplate template = getFirestoreTemplate();

		ReactiveFirestoreTransactionManager txManager = new ReactiveFirestoreTransactionManager(this.firestoreStub, this.parent);
		TransactionalOperator operator = TransactionalOperator.create(txManager);

		template.findById(Mono.defer(() -> {
			throw new FirestoreDataException("BOOM!");
		}), FirestoreTemplateTests.TestEntity.class)
				.as(operator::transactional)
				.as(StepVerifier::create)
				.expectError()
				.verify();

		verify(this.firestoreStub, times(1)).beginTransaction(any(), any());
		verify(this.firestoreStub, times(0)).commit(any(), any());


		verify(this.firestoreStub, times(1)).rollback(any(), any());
	}

	@Test
	public void writeTransaction() {

		FirestoreTemplate template = getFirestoreTemplate();

		ReactiveFirestoreTransactionManager txManager = new ReactiveFirestoreTransactionManager(this.firestoreStub, this.parent);
		TransactionalOperator operator = TransactionalOperator.create(txManager);

		doAnswer(invocation -> {
			CommitRequest commitRequest = invocation.getArgument(0);
			StreamObserver<CommitResponse> streamObserver = invocation.getArgument(1);

			assertThat(commitRequest.getTransaction()).isEqualTo(ByteString.copyFromUtf8("transaction1"));
			assertThat(commitRequest.getWritesList().get(0).getUpdate().getName()).isEqualTo(this.parent + "/testEntities/" + "e2");
			assertThat(commitRequest.getWritesList().get(1).getUpdate().getName()).isEqualTo(this.parent + "/testEntities/" + "e3");
			assertThat(commitRequest.getWritesList().get(2).getDelete()).isEqualTo(this.parent + "/testEntities/" + "e3");

			streamObserver.onNext(CommitResponse.newBuilder().build());

			streamObserver.onCompleted();
			return null;
		}).when(this.firestoreStub).commit(any(), any());

		template.findById(Mono.just("e1"), FirestoreTemplateTests.TestEntity.class)
				.flatMap(testEntity -> template.save(new FirestoreTemplateTests.TestEntity("e2", 100L)))
				.flatMap(testEntity -> template.save(new FirestoreTemplateTests.TestEntity("e3", 100L)))
				.flatMap(testEntity -> template.delete(Mono.just(testEntity)))
				.then()
				.as(operator::transactional)
				.as(StepVerifier::create)
				.verifyComplete();

		verify(this.firestoreStub).beginTransaction(any(), any());
		verify(this.firestoreStub).commit(any(), any());

		GetDocumentRequest request1 = GetDocumentRequest.newBuilder()
				.setName(this.parent + "/testEntities/" + "e1")
				.setTransaction(ByteString.copyFromUtf8("transaction1"))
				.build();
		verify(this.firestoreStub, times(1)).getDocument(eq(request1), any());
	}


	private FirestoreTemplate getFirestoreTemplate() {
		doAnswer(invocation -> {
			StreamObserver<BeginTransactionResponse> streamObserver = invocation.getArgument(1);
			streamObserver.onNext(BeginTransactionResponse.newBuilder()
					.setTransaction(ByteString.copyFromUtf8("transaction1")).build());

			streamObserver.onCompleted();
			return null;
		}).when(this.firestoreStub).beginTransaction(any(), any());

		doAnswer(invocation -> {
			CommitRequest commitRequest = invocation.getArgument(0);
			StreamObserver<CommitResponse> streamObserver = invocation.getArgument(1);

			assertThat(commitRequest.getTransaction()).isEqualTo(ByteString.copyFromUtf8("transaction1"));
			streamObserver.onNext(CommitResponse.newBuilder().build());

			streamObserver.onCompleted();
			return null;
		}).when(this.firestoreStub).commit(any(), any());

		doAnswer(invocation -> {
			RollbackRequest rollbackRequest = invocation.getArgument(0);
			StreamObserver<Empty> streamObserver = invocation.getArgument(1);

			assertThat(rollbackRequest.getTransaction()).isEqualTo(ByteString.copyFromUtf8("transaction1"));

			streamObserver.onCompleted();
			return null;
		}).when(this.firestoreStub).rollback(any(), any());


		doAnswer(invocation -> {
			GetDocumentRequest request = invocation.getArgument(0);
			StreamObserver<Document> streamObserver = invocation.getArgument(1);

			assertThat(request.getTransaction()).isEqualTo(ByteString.copyFromUtf8("transaction1"));

			String name = request.getName();
			streamObserver.onNext(FirestoreTemplateTests.buildDocument(name.substring(name.length() - 2), 100L));
			streamObserver.onCompleted();
			return null;
		}).when(this.firestoreStub).getDocument(any(), any());


		FirestoreTemplate template = new FirestoreTemplate(this.firestoreStub, this.parent,
				new FirestoreDefaultClassMapper());

		StepVerifier.setDefaultTimeout(Duration.ofSeconds(5));
		return template;
	}
}
