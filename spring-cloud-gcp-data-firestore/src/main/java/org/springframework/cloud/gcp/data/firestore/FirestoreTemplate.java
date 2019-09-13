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
import java.util.Map;

import com.google.cloud.firestore.PublicClassMapper;
import com.google.firestore.v1.CreateDocumentRequest;
import com.google.firestore.v1.Document;
import com.google.firestore.v1.DocumentMask;
import com.google.firestore.v1.FirestoreGrpc.FirestoreStub;
import com.google.firestore.v1.GetDocumentRequest;
import com.google.firestore.v1.RunQueryRequest;
import com.google.firestore.v1.RunQueryResponse;
import com.google.firestore.v1.StructuredQuery;
import com.google.firestore.v1.Value;
import com.google.firestore.v1.Write;
import com.google.firestore.v1.WriteRequest;
import com.google.firestore.v1.WriteResponse;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreMappingContext;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestorePersistentEntity;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestorePersistentProperty;
import org.springframework.cloud.gcp.data.firestore.util.ObservableReactiveUtil;
import org.springframework.util.Assert;

/**
 * An implementation of {@link FirestoreReactiveOperations}.
 *
 * @author Dmitry Solomakha
 * @author Chengyuan Zhao
 * @since 1.2
 */
public class FirestoreTemplate implements FirestoreReactiveOperations {

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

	private final FirestoreMappingContext mappingContext = new FirestoreMappingContext();

	private Duration writeBufferTimeout = Duration.ofMillis(500);

	private int writeBufferSize = FIRESTORE_WRITE_MAX_SIZE;

	/**
	 * Constructor for FirestoreTemplate.
	 * @param firestore Firestore gRPC stub
	 * @param parent the parent resource. For example:
	 *     projects/{project_id}/databases/{database_id}/documents or
	 *     projects/{project_id}/databases/{database_id}/documents/chatrooms/{chatroom_id}
	 */
	public FirestoreTemplate(FirestoreStub firestore, String parent) {
		this.firestore = firestore;
		this.parent = parent;
		this.databasePath = parent.substring(0, StringUtils.ordinalIndexOf(parent, "/", 4));
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
				.map(document -> PublicClassMapper.convertToCustomClass(document, entityClass));
	}

	@Override
	public <T> Mono<T> save(T entity) {
		return Mono.defer(() -> {
			FirestorePersistentEntity<?> persistentEntity = this.mappingContext.getPersistentEntity(entity.getClass());
			String idVal = getIdValue(entity, persistentEntity);

			Map<String, Value> valuesMap = PublicClassMapper.convertToFirestoreTypes(entity);

			CreateDocumentRequest createDocumentRequest = CreateDocumentRequest.newBuilder()
					.setParent(this.parent)
					.setCollectionId(persistentEntity.collectionName())
					.setDocumentId(idVal)
					.setDocument(Document.newBuilder().putAllFields(valuesMap))
					.build();
			return ObservableReactiveUtil.<Document>unaryCall(
					obs -> this.firestore.createDocument(createDocumentRequest, obs)).then(Mono.just(entity));
		});
	}

	/**
	 * {@inheritdoc}
	 *
	 * <p>The buffer size and buffer timeout settings for {@link #saveAll} can be modified by calling
	 * {@link #setWriteBufferSize} and {@link #setWriteBufferTimeout}.
	 */
	@Override
	public <T> Flux<T> saveAll(Publisher<T> instances) {
		Flux<List<T>> inputs = Flux.from(instances).bufferTimeout(
				this.writeBufferSize, this.writeBufferTimeout);

		return ObservableReactiveUtil.streamingBidirectionalCall(
				this::openWriteStream, inputs, this::buildWriteRequest);
	}

	@Override
	public <T> Flux<T> findAll(Class<T> clazz) {
		return Flux.defer(() ->
				findAllDocuments(clazz)
						.map(document -> PublicClassMapper.convertToCustomClass(document, clazz)));
	}

	@Override
	public <T> Mono<Long> count(Class<T> entityClass) {
		return findAllDocuments(entityClass, ID_PROJECTION, null).count();
	}

	/**
	 * {@inheritdoc}
	 *
	 * <p>The buffer size and buffer timeout settings for {@link #saveAll} can be modified by calling
	 * {@link #setWriteBufferSize} and {@link #setWriteBufferTimeout}.
	 */
	@Override
	public <T> Mono<Long> deleteAll(Class<T> clazz) {
		return deleteDocumentsByName(findAllDocuments(clazz).map(Document::getName)).count();
	}

	/**
	 * {@inheritdoc}
	 *
	 * <p>The buffer size and buffer timeout settings for {@link #saveAll} can be modified by calling
	 * {@link #setWriteBufferSize} and {@link #setWriteBufferTimeout}.
	 */
	@Override
	public <T> Mono<Void> delete(Publisher<T> entityPublisher) {
		return deleteDocumentsByName(Flux.from(entityPublisher).map(this::buildResourceName)).then();
	}

	/**
	 * {@inheritdoc}
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
				.map(document -> PublicClassMapper.convertToCustomClass(document, entityType)));
	}

	public FirestoreMappingContext getMappingContext() {
		return this.mappingContext;
	}

	private Flux<String> deleteDocumentsByName(Flux<String> documentNames) {
		return ObservableReactiveUtil.streamingBidirectionalCall(
				this::openWriteStream,
				documentNames.bufferTimeout(this.writeBufferSize, this.writeBufferTimeout),
				this::buildDeleteRequest);
	}

	private WriteRequest buildDeleteRequest(
			List<String> documentIds, WriteResponse writeResponse) {

		WriteRequest.Builder writeRequestBuilder =
				WriteRequest.newBuilder()
						.setStreamId(writeResponse.getStreamId())
						.setStreamToken(writeResponse.getStreamToken());

		for (String documentId : documentIds) {
			Write write = Write.newBuilder()
					.setDelete(documentId)
					.build();

			writeRequestBuilder.addWrites(write);
		}
		return writeRequestBuilder.build();
	}

	private <T> Flux<Document> findAllDocuments(Class<T> clazz) {
		return findAllDocuments(clazz, null, null);
	}

	private <T> Flux<Document> findAllDocuments(Class<T> clazz, StructuredQuery.Projection projection, StructuredQuery.Builder queryBuilder) {
		FirestorePersistentEntity<?> persistentEntity = this.mappingContext.getPersistentEntity(clazz);

		StructuredQuery.Builder builder = queryBuilder != null ? queryBuilder : StructuredQuery.newBuilder();
		builder.addFrom(StructuredQuery.CollectionSelector.newBuilder()
								.setCollectionId(persistentEntity.collectionName()).build());
		if (projection != null) {
			builder.setSelect(projection);
		}
		RunQueryRequest request = RunQueryRequest.newBuilder()
				.setParent(this.parent)
				.setStructuredQuery(builder.build())
				.build();

		return ObservableReactiveUtil
				.<RunQueryResponse>streamingCall(obs -> this.firestore.runQuery(request, obs))
				.filter(RunQueryResponse::hasDocument)
				.map(RunQueryResponse::getDocument);
	}


	private Mono<Document> getDocument(String id, Class aClass, DocumentMask documentMask) {
		FirestorePersistentEntity<?> persistentEntity = this.mappingContext.getPersistentEntity(aClass);
		GetDocumentRequest.Builder builder = GetDocumentRequest.newBuilder()
				.setName(buildResourceName(persistentEntity, id));

		if (documentMask != null) {
			builder.setMask(documentMask);
		}

		return ObservableReactiveUtil.<Document>unaryCall(obs -> this.firestore.getDocument(builder.build(), obs))
				.onErrorResume(throwable -> throwable.getMessage().startsWith(NOT_FOUND_DOCUMENT_MESSAGE),
						throwable -> Mono.empty());
	}

	private StreamObserver<WriteRequest> openWriteStream(StreamObserver<WriteResponse> obs) {
		WriteRequest openStreamRequest =
				WriteRequest.newBuilder().setDatabase(this.databasePath).build();
		StreamObserver<WriteRequest> requestStreamObserver = this.firestore.write(obs);
		requestStreamObserver.onNext(openStreamRequest);
		return requestStreamObserver;
	}

	private <T> WriteRequest buildWriteRequest(List<T> entityList, WriteResponse writeResponse) {
		WriteRequest.Builder writeRequestBuilder =
				WriteRequest.newBuilder()
						.setStreamId(writeResponse.getStreamId())
						.setStreamToken(writeResponse.getStreamToken());

		for (T entity : entityList)	{
			String documentResourceName = buildResourceName(entity);
			Map<String, Value> valuesMap = PublicClassMapper.convertToFirestoreTypes(entity);
			Write write = Write.newBuilder()
					.setUpdate(Document.newBuilder()
							.putAllFields(valuesMap)
							.setName(documentResourceName))
					.build();
			writeRequestBuilder.addWrites(write);
		}

		return writeRequestBuilder.build();
	}

	private <T> String buildResourceName(T entity) {
		FirestorePersistentEntity<?> persistentEntity =
				this.mappingContext.getPersistentEntity(entity.getClass());
		FirestorePersistentProperty idProperty = persistentEntity.getIdPropertyOrFail();
		Object idVal = persistentEntity.getPropertyAccessor(entity).getProperty(idProperty);

		return buildResourceName(persistentEntity, idVal.toString());
	}

	private String buildResourceName(FirestorePersistentEntity<?> persistentEntity, String s) {
		return this.parent + "/" + persistentEntity.collectionName() + "/" + s;
	}

	private String getIdValue(Object entity, FirestorePersistentEntity persistentEntity) {
		FirestorePersistentProperty idProperty = persistentEntity.getIdPropertyOrFail();
		Object idVal = persistentEntity.getPropertyAccessor(entity).getProperty(idProperty);

		return idVal.toString();
	}

}
