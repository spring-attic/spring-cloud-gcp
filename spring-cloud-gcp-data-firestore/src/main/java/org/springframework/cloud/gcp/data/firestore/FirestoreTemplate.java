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

import java.util.Map;

import com.google.cloud.firestore.PublicClassMapper;
import com.google.firestore.v1.CreateDocumentRequest;
import com.google.firestore.v1.DeleteDocumentRequest;
import com.google.firestore.v1.Document;
import com.google.firestore.v1.FirestoreGrpc;
import com.google.firestore.v1.RunQueryRequest;
import com.google.firestore.v1.RunQueryResponse;
import com.google.firestore.v1.StructuredQuery;
import com.google.firestore.v1.Value;
import com.google.protobuf.Empty;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreMappingContext;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestorePersistentEntity;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestorePersistentProperty;
import org.springframework.cloud.gcp.data.firestore.util.ObservableReactiveUtil;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * An implementation of FirestoreReactiveOperations.
 *
 * @author Dmitry Solomakha
 * @since 1.2
 */
public class FirestoreTemplate implements FirestoreReactiveOperations {
	private final FirestoreGrpc.FirestoreStub firestore;

	private final String parent;

	private final FirestoreMappingContext mappingContext = new FirestoreMappingContext();

	public FirestoreTemplate(FirestoreGrpc.FirestoreStub firestore, String parent) {
		this.firestore = firestore;
		this.parent = parent;
	}

	public <T> Mono<T> save(T entity) {
		FirestorePersistentEntity<?> persistentEntity = this.mappingContext.getPersistentEntity(entity.getClass());
		FirestorePersistentProperty idProperty = persistentEntity.getIdPropertyOrFail();
		Object idVal = persistentEntity.getPropertyAccessor(entity).getProperty(idProperty);

		Map<String, Value> valuesMap = PublicClassMapper.convertToFirestoreTypes(entity);

		CreateDocumentRequest createDocumentRequest =
				CreateDocumentRequest.newBuilder()
						.setParent(this.parent)
						.setCollectionId(findCollectionName(entity.getClass()))
						.setDocumentId(idVal.toString())
						.setDocument(Document.newBuilder().putAllFields(valuesMap))
						.build();
			return ObservableReactiveUtil.<Document>unaryCall(
					obs -> this.firestore.createDocument(createDocumentRequest, obs)).then(Mono.just(entity));
	}

	public <T> Flux<T> findAll(Class<T> clazz) {
		return findAllDocuments(clazz)
				.map(document -> PublicClassMapper.convertToCustomClass(document, clazz));
	}


	public <T> Mono<Long> deleteAll(Class<T> clazz) {
		return findAllDocuments(clazz).flatMap(this::callDelete).count();
	}

	private Mono<Empty> callDelete(Document doc) {
		DeleteDocumentRequest deleteDocumentRequest = DeleteDocumentRequest.newBuilder().setName(doc.getName())
				.build();
		return ObservableReactiveUtil.unaryCall(
						obs -> this.firestore.deleteDocument(deleteDocumentRequest, obs));
	}

	private <T> String findCollectionName(Class<T> clazz) {
		Entity entity = AnnotationUtils.findAnnotation(clazz, Entity.class);
		String name = (String) AnnotationUtils.getValue(entity, "collectionName");
		if (name == null) {
			throw new FirestoreDataException("Entities should be annotated with @Entity and have a collection name");
		}
		return name;
	}

	private <T> Flux<Document> findAllDocuments(Class<T> clazz) {
		StructuredQuery structuredQuery = StructuredQuery.newBuilder()
				.addFrom(
						StructuredQuery.CollectionSelector.newBuilder()
								.setCollectionId(findCollectionName(clazz)).build())
				.build();
		RunQueryRequest request = RunQueryRequest.newBuilder()
				.setParent(this.parent)
				.setStructuredQuery(structuredQuery)
				.build();

		return ObservableReactiveUtil.<RunQueryResponse>streamingCall(obs -> this.firestore.runQuery(request, obs))
				.filter(RunQueryResponse::hasDocument).map(RunQueryResponse::getDocument);
	}
}
