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

package org.springframework.cloud.gcp.data.firestore.mapping;

import java.util.Map;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.Internal;
import com.google.firestore.v1.Document;
import com.google.firestore.v1.Value;

import org.springframework.cloud.gcp.core.util.MapBuilder;

/**
 *
 * Uses Firestore client library to provide  object mapping functionality.
 *
 * @author Dmitry Solomakha
 * @author Mike Eltsufin
 * @since 1.2.2
 */
public final class FirestoreDefaultClassMapper implements FirestoreClassMapper {

	private static final Internal INTERNAL = new Internal(
			FirestoreOptions.newBuilder().setProjectId("dummy-project-id").build(), null);

	private static final String VALUE_FIELD_NAME = "value";

	private static final String NOT_USED_PATH = "/not/used/path";

	public FirestoreDefaultClassMapper() {
	}

	public <T> Value toFirestoreValue(T sourceValue) {
		DocumentSnapshot documentSnapshot = INTERNAL.snapshotFromMap(NOT_USED_PATH,
				new MapBuilder<String, Object>().put(VALUE_FIELD_NAME, sourceValue).build());
		return INTERNAL.protoFromSnapshot(documentSnapshot).get(VALUE_FIELD_NAME);
	}

	public <T> Document entityToDocument(T entity, String documentResourceName) {
		DocumentSnapshot documentSnapshot = INTERNAL.snapshotFromObject(NOT_USED_PATH, entity);
		Map<String, Value> valuesMap = INTERNAL.protoFromSnapshot(documentSnapshot);
		return Document.newBuilder()
				.putAllFields(valuesMap)
				.setName(documentResourceName).build();
	}

	public <T> T documentToEntity(Document document, Class<T> clazz) {
		DocumentSnapshot documentSnapshot = INTERNAL.snapshotFromProto(Timestamp.now(), document);
		return documentSnapshot.toObject(clazz);
	}
}
