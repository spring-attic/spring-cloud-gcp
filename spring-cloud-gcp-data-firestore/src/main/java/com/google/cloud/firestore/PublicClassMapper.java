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

package com.google.cloud.firestore;

import java.util.Map;

import com.google.cloud.Timestamp;
import com.google.firestore.v1.Document;
import com.google.firestore.v1.Value;

/**
 *
 * Temporary class to expose package-private methods, will be removed in the future.
 *
 * @author Dmitry Solomakha
 *
 */
public final class PublicClassMapper {

	private static final Internal INTERNAL = new Internal(new FirestoreImpl(FirestoreOptions.newBuilder().build()));

	private PublicClassMapper() {
	}

	public static <T> Map<String, Value> convertToFirestoreTypes(T entity) {
		DocumentSnapshot documentSnapshot = INTERNAL.snapshotFromObject("/not/used/path", entity);
		return documentSnapshot.getProtoFields();
	}

	public static <T> T convertToCustomClass(Document document, Class<T> clazz) {
		DocumentSnapshot documentSnapshot = INTERNAL.snapshotFromProto(Timestamp.now(), document);
		return documentSnapshot.toObject(clazz);
	}
}
