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

package com.google.cloud.spring.data.firestore;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Internal;
import com.google.cloud.spring.data.firestore.mapping.FirestoreClassMapper;
import com.google.cloud.spring.data.firestore.mapping.FirestoreMappingContext;
import com.google.cloud.spring.data.firestore.mapping.FirestorePersistentEntity;
import com.google.cloud.spring.data.firestore.mapping.FirestorePersistentProperty;
import com.google.cloud.spring.data.firestore.mapping.UpdateTime;
import com.google.cloud.spring.data.firestore.transaction.ReactiveFirestoreResourceHolder;
import com.google.cloud.spring.data.firestore.util.ObservableReactiveUtil;
import com.google.cloud.spring.data.firestore.util.Util;
import com.google.firestore.v1.CommitRequest;
import com.google.firestore.v1.CommitResponse;
import com.google.firestore.v1.Document;
import com.google.firestore.v1.DocumentMask;
import com.google.firestore.v1.FirestoreGrpc.FirestoreStub;
import com.google.firestore.v1.GetDocumentRequest;
import com.google.firestore.v1.Precondition;
import com.google.firestore.v1.RunQueryRequest;
import com.google.firestore.v1.RunQueryResponse;
import com.google.firestore.v1.StructuredQuery;
import com.google.firestore.v1.Write;
import com.google.firestore.v1.Write.Builder;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import org.springframework.transaction.reactive.TransactionContext;
import org.springframework.util.Assert;

/**
 * An implementation of {@link FirestoreReactiveOperations}.
 *
 * @author Dmitry Solomakha
 * @author Chengyuan Zhao
 * @author Mike Eltsufin
 * @since 1.2
 */
public class FirestoreTemplate implements FirestoreReactiveOperations {
	private FirestoreClassMapper classMapper;

	private static final int FIRESTORE_WRITE_MAX_SIZE = 500;

	private static final String NAME_FIELD = "__name__";

	private static final StructuredQuery.Projection ID_PROJECTION = StructuredQuery.Projection.newBuilder()
			.addFields(StructuredQuery.FieldReference.newBuilder().setFieldPath(NAME_FIELD).build())
			.build();

	private static final DocumentMask NAME_ONLY_MASK = DocumentMask.newBuilder().addFieldPaths(NAME_FIELD).build();

	private static final String NOT_FOUND_DOCUMENT_MESSAGE = "NOT_FOUND: Document";

	private final FirestoreStub firestore;

	private final String parent;

	private final String databasePath;

	private final FirestoreMappingContext mappingContext;

	private Duration writeBufferTimeout = Duration.ofMillis(500);

	private int writeBufferSize = FIRESTORE_WRITE_MAX_SIZE;

	/**
	 * Constructor for FirestoreTemplate.
	 * @param firestore Firestore gRPC stub
	 * @param parent the parent resource. For example:
	 *     projects/{project_id}/databases/{database_id}/documents
	 * @param classMapper a {@link FirestoreClassMapper} used for conversion
	 * @param mappingContext Mapping Context
	 */
	public FirestoreTemplate(FirestoreStub firestore, String parent, FirestoreClassMapper classMapper,
			FirestoreMappingContext mappingContext) {
		this.firestore = firestore;
		this.parent = parent;
		this.databasePath = Util.extractDatabasePath(parent);
		this.classMapper = classMapper;
		this.mappingContext = mappingContext;
	}

	/**
	 * Sets the {@link Duration} for how long to wait for the entity buffer to fill before sending
	 * the buffered entities to Firestore for insert/update/delete operations.
	 * @param bufferTimeout duration to wait for entity buffer to fill before sending to Firestore.
	 * 		(default = 500ms)
	 */
	public void setWriteBufferTimeout(Duration bufferTimeout) {
		this.writeBufferTimeout = bufferTimeout;
	}

	public Duration getWriteBufferTimeout() {
		return this.writeBufferTimeout;
	}

	/**
	 * Sets how many entities to include in an insert/update/delete buffered operation.
	 * <p>The maximum buffer size is 500. In most cases users should leave this at the maximum value.
	 * @param bufferWriteSize the entity buffer size for buffered operations (default = 500)
	 */
	public void setWriteBufferSize(int bufferWriteSize) {
		Assert.isTrue(
				bufferWriteSize <= FIRESTORE_WRITE_MAX_SIZE,
				"The FirestoreTemplate buffer write size must be less than " + FIRESTORE_WRITE_MAX_SIZE);
		this.writeBufferSize = bufferWriteSize;
	}

	public int getWriteBufferSize() {
		return this.writeBufferSize;
	}

	@Override
	public <T> Mono<Boolean> existsById(Publisher<String> idPublisher, Class<T> entityClass) {
		return Flux.from(idPublisher).next()
				.flatMap(id -> getDocument(id, entityClass, NAME_ONLY_MASK))
				.map(d -> true)
				.switchIfEmpty(Mono.just(false))
				.onErrorMap(
						throwable -> new FirestoreDataException("Unable to determine if document exists", throwable));
	}

	@Override
	public <T> Mono<T> findById(Publisher<String> idPublisher, Class<T> entityClass) {
		return findAllById(idPublisher, entityClass).next();
	}

	@Override
	public <T> Flux<T> findAllById(Publisher<String> idPublisher, Class<T> entityClass) {
		return Flux.from(idPublisher)
				.flatMap(id -> getDocument(id, entityClass, null))
				.onErrorMap(throwable -> new FirestoreDataException("Error while reading entries by id", throwable))
				.map(document -> getClassMapper().documentToEntity(document, entityClass));
	}

	@Override
	public <T> Mono<T> save(T entity) {
		return saveAll(Mono.just(entity)).next();
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The buffer size and buffer timeout settings for {@link #saveAll} can be modified by calling
	 * {@link #setWriteBufferSize} and {@link #setWriteBufferTimeout}.
	 */
	@Override
	public <T> Flux<T> saveAll(Publisher<T> instances) {
		return Mono.subscriberContext().flatMapMany(ctx -> {
			Optional<TransactionContext> transactionContext = ctx.getOrEmpty(TransactionContext.class);
			if (transactionContext.isPresent()) {
				ReactiveFirestoreResourceHolder holder = (ReactiveFirestoreResourceHolder) transactionContext.get()
						.getResources().get(this.firestore);
				//In a transaction, all write operations should be sent in the commit request, so we just collect them
				return Flux.from(instances).doOnNext(t -> {
					holder.getWrites().add(createUpdateWrite(t));
					holder.getEntities().add(t);
				});
			}
			return commitWrites(instances, this::createUpdateWrite, true);
		});
	}

	@Override
	public <T> Flux<T> findAll(Class<T> clazz) {
		return Flux.defer(() ->
				findAllDocuments(clazz)
						.map(document -> getClassMapper().documentToEntity(document, clazz)));
	}

	@Override
	public <T> Mono<Long> count(Class<T> entityClass) {
		return count(entityClass, null);
	}

	@Override
	public <T> Mono<Long> count(Class<T> entityClass, StructuredQuery.Builder queryBuilder) {
		return findAllDocuments(entityClass, ID_PROJECTION, queryBuilder).count();
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The buffer size and buffer timeout settings for {@link #saveAll} can be modified by calling
	 * {@link #setWriteBufferSize} and {@link #setWriteBufferTimeout}.
	 */
	@Override
	public <T> Mono<Long> deleteAll(Class<T> clazz) {
		return deleteDocumentsByName(findAllDocuments(clazz).map(Document::getName)).count();
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The buffer size and buffer timeout settings for {@link #saveAll} can be modified by calling
	 * {@link #setWriteBufferSize} and {@link #setWriteBufferTimeout}.
	 */
	@Override
	public <T> Mono<Void> delete(Publisher<T> entityPublisher) {
		return deleteDocumentsByName(Flux.from(entityPublisher).map(this::buildResourceName)).then();
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>The buffer size and buffer timeout settings for {@link #saveAll} can be modified by calling
	 * {@link #setWriteBufferSize} and {@link #setWriteBufferTimeout}.
	 */
	@Override
	public <T> Mono<Void> deleteById(Publisher<String> idPublisher, Class entityClass) {
		return Mono.defer(() -> {
			FirestorePersistentEntity<?> persistentEntity =
					this.mappingContext.getPersistentEntity(entityClass);
			return deleteDocumentsByName(
					Flux.from(idPublisher).map(id -> buildResourceName(persistentEntity, id))).then();
		});
	}

	@Override
	public <T> Flux<T> execute(StructuredQuery.Builder builder, Class<T> entityType) {
		return Flux.defer(() ->
				findAllDocuments(entityType, null, builder)
				.map(document -> getClassMapper().documentToEntity(document, entityType)));
	}

	@Override
	public FirestoreReactiveOperations withParent(Object id, Class<?> entityClass) {
		return withParent(buildResourceName(id, entityClass));
	}

	@Override
	public <T> FirestoreReactiveOperations withParent(T parent) {
		return withParent(buildResourceName(parent));
	}

	private FirestoreReactiveOperations withParent(String resourceName) {
		FirestoreTemplate firestoreTemplate =
				new FirestoreTemplate(this.firestore, resourceName, this.classMapper, this.mappingContext);
		firestoreTemplate.setWriteBufferSize(this.writeBufferSize);
		firestoreTemplate.setWriteBufferTimeout(this.writeBufferTimeout);

		return firestoreTemplate;
	}

	public FirestoreMappingContext getMappingContext() {
		return this.mappingContext;
	}

	private Flux<String> deleteDocumentsByName(Flux<String> documentNames) {
		return Mono.subscriberContext().flatMapMany(ctx -> {
			Optional<TransactionContext> transactionContext = ctx.getOrEmpty(TransactionContext.class);
			if (transactionContext.isPresent()) {
				ReactiveFirestoreResourceHolder holder = (ReactiveFirestoreResourceHolder) transactionContext.get()
						.getResources().get(this.firestore);
				List<Write> writes = holder.getWrites();
				//In a transaction, all write operations should be sent in the commit request, so we just collect them
				return Flux.from(documentNames).doOnNext(t -> writes.add(createDeleteWrite(t)));
			}
			return commitWrites(documentNames, this::createDeleteWrite, false);
		});
	}

	private <T> Flux<T> commitWrites(Publisher<T> instances, Function<T, Write> converterToWrite,
			boolean setUpdateTime) {
		return Flux.from(instances).bufferTimeout(this.writeBufferSize, this.writeBufferTimeout)
				.flatMap(batch -> {
					CommitRequest.Builder builder = CommitRequest.newBuilder()
							.setDatabase(this.databasePath);

					batch.forEach(e -> builder.addWrites(converterToWrite.apply(e)));

					return ObservableReactiveUtil
							.<CommitResponse>unaryCall(obs -> this.firestore.commit(builder.build(), obs))
							.flatMapMany(
									response -> {
										if (setUpdateTime) {
											for (T entity : batch) {
												getClassMapper()
														.setUpdateTime(entity, Timestamp.fromProto(response.getCommitTime()));
											}
										}
										return Flux.fromIterable(batch);
									});
				});
	}

	private Write createDeleteWrite(String documentId) {
		return Write.newBuilder().setDelete(documentId).build();
	}

	private <T> Flux<Document> findAllDocuments(Class<T> clazz) {
		return findAllDocuments(clazz, null, null);
	}

	private <T> Flux<Document> findAllDocuments(Class<T> clazz, StructuredQuery.Projection projection,
			StructuredQuery.Builder queryBuilder) {
		return Mono.subscriberContext().flatMapMany(ctx -> {
			FirestorePersistentEntity<?> persistentEntity = this.mappingContext.getPersistentEntity(clazz);

			StructuredQuery.Builder builder = queryBuilder != null ? queryBuilder.clone() : StructuredQuery.newBuilder();
			builder.addFrom(StructuredQuery.CollectionSelector.newBuilder()
					.setCollectionId(persistentEntity.collectionName()).build());
			if (projection != null) {
				builder.setSelect(projection);
			}
			RunQueryRequest.Builder requestBuilder = RunQueryRequest.newBuilder()
					.setParent(this.parent)
					.setStructuredQuery(builder.build());

			doIfTransaction(ctx, resourceHolder -> requestBuilder.setTransaction(resourceHolder.getTransactionId()));

			return ObservableReactiveUtil
					.<RunQueryResponse>streamingCall(obs -> this.firestore.runQuery(requestBuilder.build(), obs))
					.filter(RunQueryResponse::hasDocument)
					.map(RunQueryResponse::getDocument);
		});
	}

	private Mono<Document> getDocument(String id, Class aClass, DocumentMask documentMask) {
		return Mono.subscriberContext().flatMap(ctx -> {
			FirestorePersistentEntity<?> persistentEntity = this.mappingContext.getPersistentEntity(aClass);
			GetDocumentRequest.Builder builder = GetDocumentRequest.newBuilder()
					.setName(buildResourceName(persistentEntity, id));

			doIfTransaction(ctx, holder -> builder.setTransaction(holder.getTransactionId()));

			if (documentMask != null) {
				builder.setMask(documentMask);
			}

			return ObservableReactiveUtil.<Document>unaryCall(obs -> this.firestore.getDocument(builder.build(), obs))
					.onErrorResume(throwable -> throwable.getMessage().startsWith(NOT_FOUND_DOCUMENT_MESSAGE),
							throwable -> Mono.empty());
		});
	}

	private void doIfTransaction(Context ctx, Consumer<ReactiveFirestoreResourceHolder> holderConsumer) {
		Optional<TransactionContext> transactionContext = ctx.getOrEmpty(TransactionContext.class);
		transactionContext.ifPresent(transactionCtx -> {
			ReactiveFirestoreResourceHolder holder = (ReactiveFirestoreResourceHolder) transactionCtx.getResources()
					.get(this.firestore);
			if (!holder.getWrites().isEmpty()) {
				throw new FirestoreDataException(
						"Read operations are only allowed before write operations in a transaction");
			}
			holderConsumer.accept(holder);
		});
	}

	private <T> Write createUpdateWrite(T entity) {
		Builder builder = Write.newBuilder();
		if (getIdValue(entity) == null) {
			builder.setCurrentDocument(Precondition.newBuilder().setExists(false).build());
		}
		String resourceName = buildResourceName(entity);
		Document document = getClassMapper().entityToDocument(entity, resourceName);
		FirestorePersistentEntity<?> persistentEntity =
				this.mappingContext.getPersistentEntity(entity.getClass());
		FirestorePersistentProperty updateTimeProperty =
				Objects.requireNonNull(persistentEntity).getUpdateTimeProperty();
		if (updateTimeProperty != null
				&& Objects.requireNonNull(updateTimeProperty.findAnnotation(UpdateTime.class)).version()) {
			Object version = persistentEntity.getPropertyAccessor(entity).getProperty(updateTimeProperty);
			if (version != null) {
				builder.setCurrentDocument(
						Precondition.newBuilder().setUpdateTime(((Timestamp) version).toProto()).build());
			}
			else {
				//If an entity with an empty update time field is being saved, it must be new.
				//Otherwise it will overwrite an existing document.
				builder.setCurrentDocument(Precondition.newBuilder().setExists(false).build());
			}
		}
		return builder.setUpdate(document).build();
	}

	private <T> String buildResourceName(Object entityId, Class<T> entityClass) {
		FirestorePersistentEntity<?> persistentEntity =
				this.mappingContext.getPersistentEntity(entityClass);

		if (persistentEntity == null) {
			throw new IllegalArgumentException(entityClass.toString() + " is not a valid Firestore entity class.");
		}

		return buildResourceName(persistentEntity, entityId.toString());
	}

	private <T> String buildResourceName(T entity) {
		FirestorePersistentEntity<?> persistentEntity =
				this.mappingContext.getPersistentEntity(entity.getClass());

		if (persistentEntity == null) {
			throw new IllegalArgumentException(entity.getClass().toString() + " is not a valid Firestore entity class.");
		}

		FirestorePersistentProperty idProperty = persistentEntity.getIdPropertyOrFail();
		Object idVal = persistentEntity.getPropertyAccessor(entity).getProperty(idProperty);
		if (idVal == null) {
			idVal = Internal.autoId();
			persistentEntity.getPropertyAccessor(entity).setProperty(idProperty, idVal);
		}
		return buildResourceName(persistentEntity, idVal.toString());
	}

	private String buildResourceName(FirestorePersistentEntity<?> persistentEntity, String s) {
		return this.parent + "/" + persistentEntity.collectionName() + "/" + s;
	}

	private Object getIdValue(Object entity) {
		FirestorePersistentEntity<?> persistentEntity =
						this.mappingContext.getPersistentEntity(entity.getClass());
		FirestorePersistentProperty idProperty = persistentEntity.getIdPropertyOrFail();

		return persistentEntity.getPropertyAccessor(entity).getProperty(idProperty);
	}

	public FirestoreClassMapper getClassMapper() {
		return this.classMapper;
	}
}
