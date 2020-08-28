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

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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
import com.google.firestore.v1.WriteRequest;
import com.google.firestore.v1.WriteResponse;
import io.grpc.stub.StreamObserver;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreClassMapper;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreMappingContext;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestorePersistentEntity;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestorePersistentProperty;
import org.springframework.cloud.gcp.data.firestore.transaction.ReactiveFirestoreResourceHolder;
import org.springframework.cloud.gcp.data.firestore.util.ObservableReactiveUtil;
import org.springframework.cloud.gcp.data.firestore.util.Util;
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

	private boolean usingStreamTokens = true;

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

	@Override
	public <T> FirestoreReactiveOperations withParent(T parent) {
		FirestoreTemplate firestoreTemplate =
						new FirestoreTemplate(this.firestore, buildResourceName(parent), this.classMapper, this.mappingContext);
		firestoreTemplate.setUsingStreamTokens(this.usingStreamTokens);
		firestoreTemplate.setWriteBufferSize(this.writeBufferSize);
		firestoreTemplate.setWriteBufferTimeout(this.writeBufferTimeout);

		return firestoreTemplate;
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

	/**
	 * Sets whether the {@link FirestoreTemplate} should attach stream resume tokens to write
	 * requests.
	 *
	 * <p>Note that this should always be set to true unless you are using the
	 * Firestore emulator in which case it should be set to false because the emulator
	 * does not support using resume tokens.
	 *
	 * @param usingStreamTokens whether the template should use stream tokens
   * @since 1.2.3
	 */
	public void setUsingStreamTokens(boolean usingStreamTokens) {
		this.usingStreamTokens = usingStreamTokens;
	}

	public boolean isUsingStreamTokens() {
		return usingStreamTokens;
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
				List<Write> writes = holder.getWrites();
				//In a transaction, all write operations should be sent in the commit request, so we just collect them
				return Flux.from(instances).doOnNext(t -> writes.add(createUpdateWrite(t)));
			}

			Flux<List<T>> inputs = Flux.from(instances).bufferTimeout(this.writeBufferSize, this.writeBufferTimeout);
			return ObservableReactiveUtil.streamingBidirectionalCall(
					this::openWriteStream, inputs, this::buildWriteRequest);
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
			return ObservableReactiveUtil.streamingBidirectionalCall(
					this::openWriteStream,
					documentNames.bufferTimeout(this.writeBufferSize, this.writeBufferTimeout),
					this::buildDeleteRequest);
		});
	}

	// Visible for Testing
	WriteRequest buildDeleteRequest(
			List<String> documentIds, WriteResponse writeResponse) {

		WriteRequest.Builder writeRequestBuilder = WriteRequest.newBuilder();

		if (isUsingStreamTokens()) {
			writeRequestBuilder
					.setStreamId(writeResponse.getStreamId())
					.setStreamToken(writeResponse.getStreamToken());
		}

		documentIds.stream().map(this::createDeleteWrite).forEach(writeRequestBuilder::addWrites);

		return writeRequestBuilder.build();
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

			StructuredQuery.Builder builder = queryBuilder != null ? queryBuilder : StructuredQuery.newBuilder();
			builder.addFrom(StructuredQuery.CollectionSelector.newBuilder()
					.setCollectionId(persistentEntity.collectionName()).build());
			if (projection != null) {
				builder.setSelect(projection);
			}
			RunQueryRequest.Builder buider = RunQueryRequest.newBuilder()
					.setParent(this.parent)
					.setStructuredQuery(builder.build());

			doIfTransaction(ctx, resourceHolder -> buider.setTransaction(resourceHolder.getTransactionId()));

			return ObservableReactiveUtil
					.<RunQueryResponse>streamingCall(obs -> this.firestore.runQuery(buider.build(), obs))
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

	private StreamObserver<WriteRequest> openWriteStream(StreamObserver<WriteResponse> obs) {
		WriteRequest openStreamRequest =
				WriteRequest.newBuilder().setDatabase(this.databasePath).build();
		StreamObserver<WriteRequest> requestStreamObserver = this.firestore.write(obs);
		requestStreamObserver.onNext(openStreamRequest);
		return requestStreamObserver;
	}

	// Visible for Testing
	<T> WriteRequest buildWriteRequest(List<T> entityList, WriteResponse writeResponse) {
		WriteRequest.Builder writeRequestBuilder = WriteRequest.newBuilder();

		if (isUsingStreamTokens()) {
			writeRequestBuilder
					.setStreamId(writeResponse.getStreamId())
					.setStreamToken(writeResponse.getStreamToken());
		}

		entityList.stream().map(this::createUpdateWrite).forEach(writeRequestBuilder::addWrites);

		return writeRequestBuilder.build();
	}

	private <T> Write createUpdateWrite(T entity) {
		Builder builder = Write.newBuilder();
		if (getIdValue(entity) == null) {
			builder.setCurrentDocument(Precondition.newBuilder().setExists(false).build());
		}
		String resourceName = buildResourceName(entity);
		Document document = getClassMapper().entityToDocument(entity, resourceName);
		return builder.setUpdate(document).build();
	}

	private <T> String buildResourceName(T entity) {
		FirestorePersistentEntity<?> persistentEntity =
				this.mappingContext.getPersistentEntity(entity.getClass());
		FirestorePersistentProperty idProperty = persistentEntity.getIdPropertyOrFail();
		Object idVal = persistentEntity.getPropertyAccessor(entity).getProperty(idProperty);
		if (idVal == null) {
			//TODO: replace with com.google.cloud.firestore.Internal.autoId() when it is available
			idVal = AutoId.autoId();
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
