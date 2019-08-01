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

import java.util.List;

import com.google.protobuf.Empty;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * An interface of operations that can be done with Cloud Firestore.
 *
 * @author Dmitry Solomakha
 * @since 1.2
 */
public interface FirestoreReactiveOperations {
	/**
	 * Get all the entities of the given domain type.
	 * @param entityClass the domain type to get.
	 * @param <T> the type param of the domain type.
	 * @return the entities that were found.
	 */
	<T> Mono<List<T>> findAll(Class<T> entityClass);

	/**
	 * Delete all entities of a given domain type.
	 * @param entityClass the domain type to delete from Cloud Datastore.
	 * @param <T> the type param of the domain type.
	 * @return the number of entities that were deleted.
	 */
	<T> Flux<Empty> deleteAll(Class<T> entityClass);
}
