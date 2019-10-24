package org.springframework.cloud.gcp.data.firestore.transaction;

import com.google.firestore.v1.*;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.springframework.cloud.gcp.data.firestore.FirestoreTemplate;
import org.springframework.cloud.gcp.data.firestore.FirestoreTemplateTests;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

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
			GetDocumentRequest request = invocation.getArgument(0);
			StreamObserver<Document> streamObserver = invocation.getArgument(1);
			String name = request.getName();
			streamObserver.onNext(FirestoreTemplateTests.buildDocument(name.substring(name.length()-2), 100L));
			streamObserver.onCompleted();
			return null;
		}).when(this.firestoreStub).getDocument(any(), any());

		ReactiveFirestoreTransactionManager txManager = new ReactiveFirestoreTransactionManager(this.firestoreStub);

		FirestoreTemplate template = new FirestoreTemplate(this.firestoreStub, this.parent);

		TransactionalOperator operator = TransactionalOperator.create(txManager, new DefaultTransactionDefinition());


		StepVerifier.setDefaultTimeout(Duration.ofSeconds(5));
		Hooks.onOperatorDebug();


		Object result = template.findById(Mono.just("e1"), FirestoreTemplateTests.TestEntity.class).log()
				.concatWith(template.findById(Mono.just("e2"), FirestoreTemplateTests.TestEntity.class))
				.as(operator::transactional)
				//.timeout(Duration.ofSeconds(20))
				//.block();
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
}
