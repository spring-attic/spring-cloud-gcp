package org.springframework.cloud.gcp.data.firestore.transaction;

import com.google.firestore.v1.BeginTransactionResponse;
import com.google.firestore.v1.CommitResponse;
import com.google.firestore.v1.FirestoreGrpc;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.gcp.data.firestore.FirestoreTemplate;
import org.springframework.cloud.gcp.data.firestore.FirestoreTemplateTests;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
public class ReactiveFirestoreTransactionManagerTest {

	private final FirestoreGrpc.FirestoreStub firestoreStub = mock(FirestoreGrpc.FirestoreStub.class);

	private final String parent = "projects/my-project/databases/(default)/documents";

	@Test
	public void triggerCommitCorrectly() {

		doAnswer(invocation -> {
			StreamObserver<BeginTransactionResponse> streamObserver = invocation.getArgument(1);
			streamObserver.onNext(BeginTransactionResponse.newBuilder()
					.setTransaction(ByteString.copyFromUtf8("transaction1")).build());

			streamObserver.onCompleted();
			return null;
		}).when(this.firestoreStub).beginTransaction(any(), any());

		doAnswer(invocation -> {
			StreamObserver<CommitResponse> streamObserver = invocation.getArgument(1);
			streamObserver.onNext(CommitResponse.newBuilder().build());

			streamObserver.onCompleted();
			return null;
		}).when(this.firestoreStub).commit(any(), any());




		doAnswer(invocation -> {
			StreamObserver<com.google.firestore.v1.Document> streamObserver = invocation.getArgument(1);
			streamObserver.onError(new RuntimeException("NOT_FOUND: Document"));

			streamObserver.onCompleted();
			return null;
		}).when(this.firestoreStub).getDocument(any(), any());

		ReactiveFirestoreTransactionManager txManager = new ReactiveFirestoreTransactionManager(this.firestoreStub);

		FirestoreTemplate template = new FirestoreTemplate(this.firestoreStub, this.parent);

		TransactionalOperator operator = TransactionalOperator.create(txManager, new DefaultTransactionDefinition());


		template.findById(Mono.just("e1"), FirestoreTemplateTests.TestEntity.class)
				.as(operator::transactional)
				.as(StepVerifier::create)
				.verifyComplete();


		verify(this.firestoreStub).beginTransaction(any(), any());
		verify(this.firestoreStub).commit(any(), any());

	}

}
