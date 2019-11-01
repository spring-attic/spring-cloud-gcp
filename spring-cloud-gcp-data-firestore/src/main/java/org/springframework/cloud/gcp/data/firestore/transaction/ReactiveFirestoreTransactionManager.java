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

import com.google.firestore.v1.BeginTransactionRequest;
import com.google.firestore.v1.BeginTransactionResponse;
import com.google.firestore.v1.CommitRequest;
import com.google.firestore.v1.CommitResponse;
import com.google.firestore.v1.FirestoreGrpc;
import com.google.firestore.v1.RollbackRequest;
import com.google.firestore.v1.TransactionOptions;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gcp.data.firestore.util.ObservableReactiveUtil;
import org.springframework.cloud.gcp.data.firestore.util.Util;
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
 * Firestore-specific implementation of {@link org.springframework.transaction.ReactiveTransactionManager}.
 *
 * @author Dmitry Solomakha
 */
public class ReactiveFirestoreTransactionManager extends AbstractReactiveTransactionManager {

	private final FirestoreGrpc.FirestoreStub firestore;

	private final String databasePath;

	/**
	 * Constructor for ReactiveFirestoreTransactionManager.
	 * @param firestore Firestore gRPC stub
	 * @param parent the parent resource. For example:
	 *     projects/{project_id}/databases/{database_id}/documents or
	 *     projects/{project_id}/databases/{database_id}/documents/chatrooms/{chatroom_id}
	 */
	public ReactiveFirestoreTransactionManager(FirestoreGrpc.FirestoreStub firestore, String parent) {
		this.firestore = firestore;
		this.databasePath = Util.extractDatabasePath(parent);
	}

	@Override
	protected Object doGetTransaction(TransactionSynchronizationManager synchronizationManager)
			throws TransactionException {
		ReactiveFirestoreResourceHolder resourceHolder = (ReactiveFirestoreResourceHolder) synchronizationManager
				.getResource(this.firestore);
		return new ReactiveFirestoreTransactionObject(resourceHolder);
	}

	@Override
	protected Mono<Void> doBegin(TransactionSynchronizationManager synchronizationManager, Object transactionObject,
			TransactionDefinition transactionDefinition) throws TransactionException {
		return Mono.defer(() -> {
			Mono<ReactiveFirestoreResourceHolder> holder = startTransaction(transactionDefinition);

			return holder.doOnNext(extractFirestoreTransaction(transactionObject)::setResourceHolder)
					.onErrorMap(
							ex -> new TransactionSystemException("Could not start Firestore transaction", ex))
					.doOnSuccess(resourceHolder -> {
						synchronizationManager.bindResource(this.firestore, resourceHolder);
					})
					.then();
		});
	}

	@Override
	protected Mono<Void> doCommit(TransactionSynchronizationManager transactionSynchronizationManager,
			GenericReactiveTransaction genericReactiveTransaction) throws TransactionException {
		return Mono.defer(() -> {
			ReactiveFirestoreResourceHolder resourceHolder = extractFirestoreTransaction(genericReactiveTransaction)
					.getResourceHolder();

			CommitRequest.Builder builder = CommitRequest.newBuilder()
					.setDatabase(this.databasePath)
					.setTransaction(resourceHolder.getTransactionId());

			resourceHolder.getWrites().forEach(builder::addWrites);

			return ObservableReactiveUtil
					.<CommitResponse>unaryCall(obs -> this.firestore.commit(builder.build(), obs))
					.then();
		});
	}

	@Override
	protected Mono<Void> doRollback(TransactionSynchronizationManager transactionSynchronizationManager,
			GenericReactiveTransaction genericReactiveTransaction) throws TransactionException {

		return ObservableReactiveUtil
				.<Empty>unaryCall(
						obs -> this.firestore.rollback(RollbackRequest.newBuilder()
								.setTransaction(
										extractFirestoreTransaction(genericReactiveTransaction).getTransactionId())
								.build(), obs))
				.then();
	}

	private static ReactiveFirestoreTransactionObject extractFirestoreTransaction(Object transaction) {

		Assert.isInstanceOf(ReactiveFirestoreTransactionObject.class, transaction,
				() -> String.format("Expected to find a %s but it turned out to be %s.",
						ReactiveFirestoreTransactionObject.class,
						transaction.getClass()));

		return (ReactiveFirestoreTransactionObject) transaction;
	}

	private static ReactiveFirestoreTransactionObject extractFirestoreTransaction(
			GenericReactiveTransaction transaction) {

		Assert.isInstanceOf(ReactiveFirestoreTransactionObject.class, transaction.getTransaction(),
				() -> String.format("Expected to find a %s but it turned out to be %s.",
						ReactiveFirestoreTransactionObject.class,
						transaction.getTransaction().getClass()));

		return (ReactiveFirestoreTransactionObject) transaction.getTransaction();
	}

	private Mono<ReactiveFirestoreResourceHolder> startTransaction(TransactionDefinition definition) {
		TransactionOptions.Builder txOptions = definition.isReadOnly()
				? TransactionOptions.newBuilder().setReadOnly(TransactionOptions.ReadOnly.newBuilder().build())
				: TransactionOptions.newBuilder().setReadWrite(TransactionOptions.ReadWrite.newBuilder().build());

		BeginTransactionRequest beginTransactionRequest = BeginTransactionRequest.newBuilder()
				.setOptions(txOptions)
				.setDatabase(this.databasePath)
				.build();
		return ObservableReactiveUtil
				.<BeginTransactionResponse>unaryCall(
						obs -> this.firestore.beginTransaction(beginTransactionRequest, obs))
				.map(beginTransactionResponse -> new ReactiveFirestoreResourceHolder(
						beginTransactionResponse.getTransaction()));
	}

	/**
	 * Firestore specific transaction object, representing a
	 * {@link ReactiveFirestoreResourceHolder}. Used as transaction object by
	 * {@link ReactiveFirestoreTransactionManager}.
	 */
	private class ReactiveFirestoreTransactionObject implements SmartTransactionObject {
		private @Nullable ReactiveFirestoreResourceHolder resourceHolder;

		ReactiveFirestoreTransactionObject(@Nullable ReactiveFirestoreResourceHolder resourceHolder) {
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

		/**
		 * Not supported in Firestore.
		 */
		@Override
		public void flush() {

		}
	}
}
