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
import com.google.firestore.v1.DeleteDocumentRequest;
import com.google.firestore.v1.Document;
import com.google.firestore.v1.FirestoreGrpc.FirestoreStub;
import com.google.firestore.v1.GetDocumentRequest;
import com.google.firestore.v1.RunQueryRequest;
import com.google.firestore.v1.RunQueryResponse;
import com.google.firestore.v1.StructuredQuery;
import com.google.firestore.v1.Value;
import com.google.firestore.v1.Write;
import com.google.firestore.v1.WriteRequest;
import com.google.firestore.v1.WriteResponse;
import com.google.protobuf.Empty;
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

	private final FirestoreStub firestore;

	private final String parent;

	private final String databasePath;

	private final FirestoreMappingContext mappingContext = new FirestoreMappingContext();

	private Duration saveAllBufferTimeout = Duration.ofMillis(500);

	private int saveAllBufferWriteSize = FIRESTORE_WRITE_MAX_SIZE;

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
	 * the buffered entities to Firestore.
	 * @param bufferTimeout duration to wait for entity buffer to fill before sending to Firestore.
	 * 		(default = 500ms)
	 */
	public void setSaveAllBufferTimeoutDuration(Duration bufferTimeout) {
		this.saveAllBufferTimeout = bufferTimeout;
	}

	public Duration getSaveAllBufferTimeoutDuration() {
		return this.saveAllBufferTimeout;
	}

	/**
	 * Sets how many entities to include in an insert/update/delete buffered operation.
	 * <p>The maximum buffer size is 500. In most cases users should leave this at the maximum value.
	 * @param bufferWriteSize the entity buffer size for buffered operations (default = 500)
	 */
	public void setSaveAllBufferWriteSize(int bufferWriteSize) {
		Assert.isTrue(
				bufferWriteSize <= FIRESTORE_WRITE_MAX_SIZE,
				"The FirestoreTemplate buffer write size must be less than " + FIRESTORE_WRITE_MAX_SIZE);
		this.saveAllBufferWriteSize = bufferWriteSize;
	}

	public int getSaveAllBufferWriteSize() {
		return this.saveAllBufferWriteSize;
	}

	public <T> Mono<T> findById(Publisher idPublisher, Class<T> aClass) {
		return findAllById(idPublisher, aClass).next();
	}

	public <T> Flux<T> findAllById(Publisher idPublisher, Class<T> aClass) {
		return ((Flux<String>) Flux.from(idPublisher)).flatMap(id -> {
			FirestorePersistentEntity<?> persistentEntity = this.mappingContext.getPersistentEntity(aClass);
			GetDocumentRequest request = GetDocumentRequest.newBuilder()
					.setName(this.parent + "/" + persistentEntity.collectionName() + "/" + id).build();
			return ObservableReactiveUtil.<Document>unaryCall(obs -> this.firestore.getDocument(request, obs));
		}).onErrorMap(throwable -> new FirestoreDataException("Unable to find an entry by id", throwable))
				.map(document -> PublicClassMapper.convertToCustomClass(document, aClass));
	}

	public <T> Mono<T> save(T entity) {
		return Mono.defer(() -> {
			FirestorePersistentEntity<?> persistentEntity = this.mappingContext.getPersistentEntity(entity.getClass());
			FirestorePersistentProperty idProperty = persistentEntity.getIdPropertyOrFail();
			Object idVal = persistentEntity.getPropertyAccessor(entity).getProperty(idProperty);

			Map<String, Value> valuesMap = PublicClassMapper.convertToFirestoreTypes(entity);

			CreateDocumentRequest createDocumentRequest = CreateDocumentRequest.newBuilder()
					.setParent(this.parent)
					.setCollectionId(persistentEntity.collectionName())
					.setDocumentId(idVal.toString())
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
	 * {@link #setSaveAllBufferWriteSize} and {@link #setSaveAllBufferTimeoutDuration}.
	 */
	@Override
	public <T> Flux<T> saveAll(Publisher<T> instances) {
		Flux<List<T>> inputs = Flux.from(instances).bufferTimeout(
				this.saveAllBufferWriteSize, this.saveAllBufferTimeout);

		return ObservableReactiveUtil.streamingBidirectionalCall(
				this::openWriteStream, inputs, this::buildWriteRequest);
	}

	public <T> Flux<T> findAll(Class<T> clazz) {
		return Flux.defer(() ->
				findAllDocuments(clazz)
						.map(document -> PublicClassMapper.convertToCustomClass(document, clazz)));
	}

	public <T> Mono<Long> deleteAll(Class<T> clazz) {
		return Mono.defer(() ->
			findAllDocuments(clazz).flatMap(this::callDelete).count());
	}

	private Mono<Empty> callDelete(Document doc) {
		DeleteDocumentRequest deleteDocumentRequest = DeleteDocumentRequest.newBuilder().setName(doc.getName())
				.build();
		return ObservableReactiveUtil.unaryCall(
						obs -> this.firestore.deleteDocument(deleteDocumentRequest, obs));
	}

	private <T> Flux<Document> findAllDocuments(Class<T> clazz) {
		FirestorePersistentEntity<?> persistentEntity = this.mappingContext.getPersistentEntity(clazz);
		StructuredQuery structuredQuery = StructuredQuery.newBuilder()
				.addFrom(
						StructuredQuery.CollectionSelector.newBuilder()
								.setCollectionId(persistentEntity.collectionName()).build())
				.build();
		RunQueryRequest request = RunQueryRequest.newBuilder()
				.setParent(this.parent)
				.setStructuredQuery(structuredQuery)
				.build();

		return ObservableReactiveUtil.<RunQueryResponse>streamingCall(obs -> this.firestore.runQuery(request, obs))
				.filter(RunQueryResponse::hasDocument).map(RunQueryResponse::getDocument);
	}

	private <T> String buildResourceName(T entity) {
		FirestorePersistentEntity<?> persistentEntity = this.mappingContext
			.getPersistentEntity(entity.getClass());
		FirestorePersistentProperty idProperty = persistentEntity.getIdPropertyOrFail();
		Object idVal = persistentEntity.getPropertyAccessor(entity).getProperty(idProperty);

		return this.parent + "/" + persistentEntity.collectionName() + "/" + idVal.toString();
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
					.setUpdate(
							Document.newBuilder().putAllFields(valuesMap).setName(documentResourceName))
					.build();
			writeRequestBuilder.addWrites(write);
		}

		return writeRequestBuilder.build();
	}
}
