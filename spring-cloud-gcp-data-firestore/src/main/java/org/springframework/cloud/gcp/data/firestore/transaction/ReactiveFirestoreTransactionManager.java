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

package org.springframework.cloud.gcp.data.firestore.transaction;

import com.google.firestore.v1.BeginTransactionRequest;
import com.google.firestore.v1.BeginTransactionResponse;
import com.google.firestore.v1.CommitRequest;
import com.google.firestore.v1.CommitResponse;
import com.google.firestore.v1.FirestoreGrpc;
import com.google.firestore.v1.RollbackRequest;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gcp.data.firestore.util.ObservableReactiveUtil;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.reactive.AbstractReactiveTransactionManager;
import org.springframework.transaction.reactive.GenericReactiveTransaction;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.util.Assert;

/**
 * Firestore-specific implementation of {@link org.springframework.transaction.ReactiveTransactionManager}
 *
 * @author Dmitry Solomakha
 */
public class ReactiveFirestoreTransactionManager extends AbstractReactiveTransactionManager {

	private final FirestoreGrpc.FirestoreStub firestore;

	public ReactiveFirestoreTransactionManager(FirestoreGrpc.FirestoreStub firestore) {
		this.firestore = firestore;
	}

	@Override
	protected Object doGetTransaction(TransactionSynchronizationManager synchronizationManager) throws TransactionException {
		ReactiveFirestoreResourceHolder resourceHolder = (ReactiveFirestoreResourceHolder) synchronizationManager
				.getResource(getRequiredDatabase());
		return new ReactiveFirestoreTransactionObject(resourceHolder);
	}

	@Override
	protected Mono<Void> doBegin(TransactionSynchronizationManager synchronizationManager, Object o, TransactionDefinition transactionDefinition) throws TransactionException {
		return Mono.defer(() -> {
			ReactiveFirestoreTransactionObject transactionObject = extractFirestoreTransaction(o);
			Mono<ReactiveFirestoreResourceHolder> holder = newResourceHolder(transactionDefinition);

			return holder.doOnNext(transactionObject::setResourceHolder)
					.onErrorMap(
							ex -> new TransactionSystemException("Could not start Firestore transaction", ex))
					.doOnSuccess(resourceHolder -> {
						synchronizationManager.bindResource(getRequiredDatabase(), resourceHolder);
					})
					.then();
		});
	}

	@Override
	protected Mono<Void> doCommit(TransactionSynchronizationManager transactionSynchronizationManager,
			GenericReactiveTransaction genericReactiveTransaction) throws TransactionException {
		ReactiveFirestoreTransactionObject transactionObject = extractFirestoreTransaction(genericReactiveTransaction);

		return ObservableReactiveUtil
				.<CommitResponse>unaryCall(
						obs -> this.firestore.commit(
								CommitRequest.newBuilder().setTransaction(transactionObject.getTransactionId()).build(),
								obs))
				.then();
	}

	@Override
	protected Mono<Void> doRollback(TransactionSynchronizationManager transactionSynchronizationManager,
			GenericReactiveTransaction genericReactiveTransaction) throws TransactionException {
		ReactiveFirestoreTransactionObject transactionObject = extractFirestoreTransaction(genericReactiveTransaction);

		return ObservableReactiveUtil
				.<Empty>unaryCall(
						obs -> this.firestore.rollback(RollbackRequest.newBuilder()
								.setTransaction(transactionObject.getTransactionId()).build(), obs))
				.then();
	}

	private FirestoreGrpc.FirestoreStub getRequiredDatabase() {
		return this.firestore;
	}

	private static ReactiveFirestoreTransactionObject extractFirestoreTransaction(Object transaction) {

		Assert.isInstanceOf(ReactiveFirestoreTransactionObject.class, transaction,
				() -> String.format("Expected to find a %s but it turned out to be %s.",
						ReactiveFirestoreTransactionObject.class,
						transaction.getClass()));

		return (ReactiveFirestoreTransactionObject) transaction;
	}

	private static ReactiveFirestoreTransactionObject extractFirestoreTransaction(GenericReactiveTransaction status) {

		Assert.isInstanceOf(ReactiveFirestoreTransactionObject.class, status.getTransaction(),
				() -> String.format("Expected to find a %s but it turned out to be %s.",
						ReactiveFirestoreTransactionObject.class,
						status.getTransaction().getClass()));

		return (ReactiveFirestoreTransactionObject) status.getTransaction();
	}

	private Mono<ReactiveFirestoreResourceHolder> newResourceHolder(TransactionDefinition definition) {
		return ObservableReactiveUtil
				.<BeginTransactionResponse>unaryCall(
						obs -> this.firestore.beginTransaction(BeginTransactionRequest.getDefaultInstance(), obs))
				.map(beginTransactionResponse -> new ReactiveFirestoreResourceHolder(beginTransactionResponse.getTransaction()));
	}

	/**
	 * MongoDB specific transaction object, representing a {@link ReactiveFirestoreResourceHolder}. Used as transaction object by
	 * {@link ReactiveFirestoreTransactionManager}.
	 */
	private class ReactiveFirestoreTransactionObject  implements SmartTransactionObject {
		private @Nullable ReactiveFirestoreResourceHolder resourceHolder;

		public ReactiveFirestoreTransactionObject(@Nullable ReactiveFirestoreResourceHolder resourceHolder) {
			this.resourceHolder = resourceHolder;
		}

		@Nullable
		public ByteString getTransactionId() {
			return this.resourceHolder != null ? this.resourceHolder.getTransactionId() : null;
		}

		@Nullable
		public ReactiveFirestoreResourceHolder getResourceHolder() {
			return this.resourceHolder;
		}

		public void setResourceHolder(@Nullable ReactiveFirestoreResourceHolder resourceHolder) {
			this.resourceHolder = resourceHolder;
		}

		@Override
		public boolean isRollbackOnly() {
			return false;
		}

		@Override
		public void flush() {

		}
	}
}
