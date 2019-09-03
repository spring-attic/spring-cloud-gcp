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

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * @author Dmitry Solomakha
 *
 * @since 1.2
 */
public class FirestoreReactiveRepository<T> implements ReactiveCrudRepository<T, String> {

	private FirestoreTemplate firestoreTemplate;

	private Class type;

	public FirestoreReactiveRepository(FirestoreTemplate firestoreTemplate, Class type) {
		this.firestoreTemplate = firestoreTemplate;
		this.type = type;
	}

	@Override
	public <S extends T> Mono<S> save(S entity) {
		return this.firestoreTemplate.save(entity);
	}

	@Override
	public <S extends T> Flux<S> saveAll(Iterable<S> entities) {
		return this.firestoreTemplate.saveAll(Flux.fromIterable(entities));
	}

	@Override
	public <S extends T> Flux<S> saveAll(Publisher<S> entityStream) {
		return this.firestoreTemplate.saveAll(entityStream);
	}

	@Override
	public Mono<T> findById(String id) {
		return findById(Mono.just(id));
	}

	@Override
	public Mono<T> findById(Publisher<String> idPublisher) {
		return this.firestoreTemplate.findById(idPublisher, this.type);
	}

	@Override
	public Mono<Boolean> existsById(String id) {
		return existsById(Mono.just(id));
	}

	@Override
	public Mono<Boolean> existsById(Publisher idPublisher) {
		return this.firestoreTemplate.existsById(idPublisher, this.type);
	}

	@Override
	public Flux<T> findAll() {
		return this.firestoreTemplate.findAll(this.type);
	}

	@Override
	public Flux<T> findAllById(Iterable<String> iterable) {
		return findAllById(Flux.fromIterable(iterable));
	}

	@Override
	public Flux<T> findAllById(Publisher<String> idStream) {
		return this.firestoreTemplate.findAllById(idStream, this.type);
	}

	@Override
	public Mono<Long> count() {
		return this.firestoreTemplate.count(this.type);
	}

	@Override
	public Mono<Void> deleteById(String id) {
		return deleteById(Mono.just(id));
	}

	@Override
	public Mono<Void> deleteById(Publisher idPublisher) {
		return this.firestoreTemplate.deleteById(idPublisher, this.type);
	}

	@Override
	public Mono<Void> delete(Object entity) {
		return this.firestoreTemplate.delete(Mono.just(entity));
	}

	@Override
	public Mono<Void> deleteAll(Iterable entities) {
		return this.firestoreTemplate.delete(Flux.fromIterable(entities));
	}

	@Override
	public Mono<Void> deleteAll(Publisher entityStream) {
		return this.firestoreTemplate.delete(entityStream);
	}

	@Override
	public Mono<Void> deleteAll() {
		return this.firestoreTemplate.deleteAll(this.type);
	}
}
