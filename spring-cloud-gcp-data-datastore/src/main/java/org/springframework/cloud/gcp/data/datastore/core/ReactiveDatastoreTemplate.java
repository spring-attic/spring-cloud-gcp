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

package org.springframework.cloud.gcp.data.datastore.core;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import com.google.cloud.datastore.BaseEntity;
import com.google.cloud.datastore.BaseKey;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.data.domain.Example;
import org.springframework.util.Assert;

/**
 * A reactive template for Cloud Datastore operations.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.2
 */
public class ReactiveDatastoreTemplate {

	private final DatastoreTemplate datastoreTemplate;

	/**
	 * Constructor.
	 * @param datastoreTemplate A standard synchronous Datastore Template used to resolve
	 *     relationships.
	 */
	public ReactiveDatastoreTemplate(DatastoreTemplate datastoreTemplate) {
		Assert.notNull(datastoreTemplate,
				"A non-null DatastoreTemplate is required.");
		this.datastoreTemplate = datastoreTemplate;
	}

	public <T> Mono<T> findById(Object id, Class<T> entityClass) {
		return performFindByKey(Collections.singleton(id), entityClass).next();
	}

	public <T> Mono<T> save(T instance, Key... ancestors) {
		return Mono.fromSupplier(() -> {
			List<T> instances = Collections.singletonList(instance);
			this.datastoreTemplate.saveEntities(instances, ancestors);
			return instance;
		});
	}

	public <T> Flux<T> saveAll(Iterable<T> entities, Key... ancestors) {
		return Flux.defer(() -> {
			this.datastoreTemplate.saveEntities((List<T>) entities, ancestors);
			return Flux.fromIterable(entities);
		});
	}

	public <T> Mono<Void> deleteById(Object id, Class<T> entityClass) {
		return Mono.fromRunnable(() -> this.datastoreTemplate.performDelete(
				new Key[] { this.datastoreTemplate.getKeyFromId(id, entityClass) },
				Collections.singletonList(id), null, entityClass));
	}

	public <T> Mono<Void> deleteAllById(Iterable<?> ids, Class<T> entityClass) {
		return Mono.fromRunnable(() -> this.datastoreTemplate.performDelete(
				this.datastoreTemplate.getKeysFromIds(ids, entityClass).toArray(new Key[0]), ids, null, entityClass));
	}

	public <T> Mono<Void> delete(T entity) {
		return Mono.fromRunnable(() -> this.datastoreTemplate.performDelete(
				new Key[] { this.datastoreTemplate.getKey(entity, false) }, null,
				Collections.singletonList(entity), entity.getClass()));
	}

	public <T> Mono<Void> deleteAll(Iterable<T> entities) {
		return Mono.fromRunnable(() -> this.datastoreTemplate.performDelete(
				StreamSupport.stream(entities.spliterator(), false)
						.map((x) -> this.datastoreTemplate.getKey(x, false)).toArray(Key[]::new),
				null, entities, null));
	}

	public Mono<Void> deleteAll(Class<?> entityClass) {
		return findAllKeys(entityClass).collectList().doOnNext(keys -> this.datastoreTemplate.performDelete(keys.toArray(new Key[0]), null, null, entityClass))
				.then();
	}

	public Mono<Long> count(Class<?> entityClass) {
		return findAllKeys(entityClass).count();
	}

	public <T> Flux<T> findAllById(Iterable<?> ids, Class<T> entityClass) {
		return performFindByKey(ids, entityClass);
	}

	private <T> Flux<T> performFindByKey(Iterable<?> ids, Class<T> entityClass) {
		Set<Key> keys = this.datastoreTemplate.getKeysFromIds(ids, entityClass);
		return findAllById(keys, entityClass, new DatastoreTemplate.ReadContext());
	}

	private <T> Flux<T> findAllById(Set<Key> keys, Class<T> entityClass, DatastoreTemplate.ReadContext context) {
		if (keys == null || keys.isEmpty()) {
			return Flux.empty();
		}

		/*
		 * We mark each key that will be read as DONE in the read context with a null value. The
		 * null is only a placeholder to prevent circular-dependencies for when the actual entity
		 * is read by the Flux below.
		 */
		for (BaseKey mk : keys) {
			context.putReadEntity(mk, null);
		}

		return convertAndResolveRelationshipsEagerly(
				Flux.fromIterable(
						() -> this.datastoreTemplate.getDatastoreReadWriter().get(keys.toArray(new Key[] {}))),
				entityClass, context);
	}

	public <T> Flux<T> findAll(Class<T> entityClass, DatastoreQueryOptions queryOptions) {
		DatastorePersistentEntity<?> persistentEntity = this.datastoreTemplate.getDatastoreMappingContext()
				.getPersistentEntity(entityClass);
		EntityQuery.Builder builder = Query.newEntityQueryBuilder()
				.setKind(persistentEntity.kindName());
		DatastoreTemplate.applyQueryOptions(builder, queryOptions, persistentEntity);
		Query query = builder.build();

		return convertAndResolveRelationshipsEagerly(
				Flux.fromIterable(() -> this.datastoreTemplate.getDatastoreReadWriter().run(query)), entityClass,
				new DatastoreTemplate.ReadContext());
	}

	public <T> Flux<T> query(Query<? extends BaseEntity> query, Class<T> entityClass) {
		return convertAndResolveRelationshipsEagerly(
				(Flux<Entity>) Flux.fromIterable(() -> this.datastoreTemplate.getDatastoreReadWriter().run(query)),
				entityClass, new DatastoreTemplate.ReadContext());
	}

	public <A, T> Flux<T> query(Query<A> query, Function<A, T> entityFunc) {
		return Flux.fromIterable(() -> this.datastoreTemplate.getDatastoreReadWriter().run(query))
				.map(entityFunc);
	}

	public Flux<Key> queryKeys(Query<Key> query) {
		return Flux.fromIterable(() -> this.datastoreTemplate.getDatastoreReadWriter().run(query));
	}

	public <T> Flux<T> findAll(Class<T> entityClass) {
		return findAll(entityClass, null);
	}

	public <T> Flux<T> queryByExample(Example<T> example, DatastoreQueryOptions queryOptions) {
		return query(this.datastoreTemplate.exampleToQuery(example, queryOptions, false), example.getProbeType());
	}

	public <T> Flux<Key> keyQueryByExample(Example<T> example, DatastoreQueryOptions queryOptions) {
		Query query = this.datastoreTemplate.exampleToQuery(example, queryOptions, true);
		return Flux.fromIterable(() -> this.datastoreTemplate.getDatastoreReadWriter().run(query));
	}

	public <T> Mono<Boolean> existsById(Object id, Class<T> entityClass) {
		return findById(id, entityClass).map(Objects::nonNull);
	}

	private Flux<Key> findAllKeys(Class entityClass) {
		return queryKeys(Query.newKeyQueryBuilder().setKind(
				this.datastoreTemplate.getDatastoreMappingContext()
						.getPersistentEntity(entityClass).kindName())
				.build());
	}

	private <A> Flux<A> convertAndResolveRelationshipsEagerly(Flux<Entity> entities, Class<A> entityClass,
			DatastoreTemplate.ReadContext readContext) {
		DatastorePersistentEntity datastorePersistentEntity = this.datastoreTemplate.getDatastoreMappingContext()
				.getPersistentEntity(entityClass);
		return entities.doOnNext(entity -> readContext.putReadEntity(entity.getKey(), entity))
				.map(entity -> this.datastoreTemplate.convertEntityResolveDescendantsAndReferences(entityClass,
						datastorePersistentEntity,
						entity.getKey(),
						readContext))
				.filter(Objects::nonNull);
	}
}
