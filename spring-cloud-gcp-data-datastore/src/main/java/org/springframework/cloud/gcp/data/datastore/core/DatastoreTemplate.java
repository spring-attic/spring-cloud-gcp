/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.data.datastore.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.datastore.BaseEntity;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreReaderWriter;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Entity.Builder;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Value;

import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreEntityConverter;
import org.springframework.cloud.gcp.data.datastore.core.convert.ObjectToKeyFactory;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.util.Assert;

/**
 * An implementation of {@link DatastoreOperations}.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class DatastoreTemplate implements DatastoreOperations {

	private final DatastoreReaderWriter datastore;

	private final DatastoreEntityConverter datastoreEntityConverter;

	private final DatastoreMappingContext datastoreMappingContext;

	private final ObjectToKeyFactory objectToKeyFactory;

	public DatastoreTemplate(DatastoreReaderWriter datastore,
			DatastoreEntityConverter datastoreEntityConverter,
			DatastoreMappingContext datastoreMappingContext,
			ObjectToKeyFactory objectToKeyFactory) {
		Assert.notNull(datastore, "A non-null Datastore service object is required.");
		Assert.notNull(datastoreEntityConverter,
				"A non-null DatastoreEntityConverter is required.");
		Assert.notNull(datastoreMappingContext,
				"A non-null DatastoreMappingContext is required.");
		Assert.notNull(objectToKeyFactory,
				"A non-null Object to Key factory is required.");
		this.datastore = datastore;
		this.datastoreEntityConverter = datastoreEntityConverter;
		this.datastoreMappingContext = datastoreMappingContext;
		this.objectToKeyFactory = objectToKeyFactory;
	}

	/**
	 * Get the {@link DatastoreEntityConverter} used by this template.
	 * @return the converter.
	 */
	public DatastoreEntityConverter getDatastoreEntityConverter() {
		return this.datastoreEntityConverter;
	}

	@Override
	public <T> T findById(Object id, Class<T> entityClass) {
		Iterator<T> results = findAllById(Collections.singleton(id), entityClass)
				.iterator();
		return results.hasNext() ? results.next() : null;
	}

	@Override
	public <T> T save(T instance) {
		this.datastore.put(convertToEntityForSave(instance));
		return instance;
	}

	@Override
	public <T> Iterable<T> saveAll(Iterable<T> entities) {
		this.datastore.put(StreamSupport.stream(entities.spliterator(), false)
				.map(this::convertToEntityForSave).toArray(Entity[]::new));
		return entities;
	}

	@Override
	public <T> void deleteById(Object id, Class<T> entityClass) {
		this.datastore.delete(getKeyFromId(id, entityClass));
	}

	@Override
	public <T> void deleteAllById(Iterable<?> ids, Class<T> entityClass) {
		List<Key> keys = getKeysFromIds(ids, entityClass);
		this.datastore.delete(keys.toArray(new Key[keys.size()]));
	}

	@Override
	public <T> void delete(T entity) {
		this.datastore.delete(getKey(entity, false));
	}

	@Override
	public <T> void deleteAll(Iterable<T> entities) {
		this.datastore.delete(StreamSupport.stream(entities.spliterator(), false)
				.map(x -> getKey(x, false)).toArray(Key[]::new));
	}

	@Override
	public long deleteAll(Class<?> entityClass) {
		Key[] keysToDelete = findAllKeys(entityClass);
		this.datastore.delete(keysToDelete);
		return keysToDelete.length;
	}

	@Override
	public long count(Class<?> entityClass) {
		return findAllKeys(entityClass).length;
	}

	@Override
	public <T> Collection<T> findAllById(Iterable<?> ids, Class<T> entityClass) {
		List<Key> keysToFind = getKeysFromIds(ids, entityClass);
		return convertEntitiesForRead(
				this.datastore.get(keysToFind.toArray(new Key[keysToFind.size()])),
				entityClass);
	}

	@Override
	public <T> Iterable<T> query(Query<? extends BaseEntity> query,
			Class<T> entityClass) {
		return convertEntitiesForRead(this.datastore.run(query), entityClass);
	}

	/**
	 * Finds objects by using a Cloud Datastore query. If the query is a key-query, then keys are
	 * returned.
	 * @param query the query to execute.
	 * @param entityClass the type of object to retrieve.
	 * @param <T> the type of object to retrieve.
	 * @return a list of the objects found. If no keys could be found the list will be
	 * empty.
	 */
	public <T> Iterable<?> queryKeysOrEntities(Query query, Class<T> entityClass) {
		QueryResults results = this.datastore.run(query);
		if (results.getResultClass() == Key.class) {
			return () -> this.datastore.run(query);
		}
		return convertEntitiesForRead(results, entityClass);
	}

	@Override
	public <A, T> Iterable<T> query(Query<A> query, Function<A, T> entityFunc) {
		List<T> results = new ArrayList<>();
		this.datastore.run(query).forEachRemaining(x -> results.add(entityFunc.apply(x)));
		return results;
	}

	@Override
	public Iterable<Key> queryKeys(Query<Key> query) {
		return () -> this.datastore.run(query);
	}

	@Override
	public <T> Collection<T> findAll(Class<T> entityClass) {
		return convertEntitiesForRead(
				this.datastore.run(Query.newEntityQueryBuilder()
						.setKind(this.datastoreMappingContext
								.getPersistentEntity(entityClass).kindName())
						.build()),
				entityClass);
	}

	@Override
	public <T> boolean existsById(Object id, Class<T> entityClass) {
		return findById(id, entityClass) != null;
	}

	@Override
	public <A> A performTransaction(Function<DatastoreOperations, A> operations) {
		if (!(this.datastore instanceof Datastore)) {
			throw new DatastoreDataException(
					"This DatastoreReadWriter cannot be used to run transactions. A full Datastore service"
							+ " object is required to run functions as transactions.");
		}
		return ((Datastore) this.datastore).runInTransaction(
				(DatastoreReaderWriter readerWriter) -> operations.apply(new DatastoreTemplate(readerWriter,
						DatastoreTemplate.this.datastoreEntityConverter,
						DatastoreTemplate.this.datastoreMappingContext,
						DatastoreTemplate.this.objectToKeyFactory)));
	}

	private Entity convertToEntityForSave(Object entity) {
		Builder builder = Entity.newBuilder(getKey(entity, true));
		this.datastoreEntityConverter.write(entity, builder);
		return builder.build();
	}

	private <T> Collection<T> convertEntitiesForRead(
			Iterator<? extends BaseEntity> entities,
			Class<T> entityClass) {
		List<T> results = new ArrayList<>();
		if (entities == null) {
			return results;
		}

		DatastorePersistentEntity datastorePersistentEntity = this.datastoreMappingContext
				.getPersistentEntity(entityClass);

		entities.forEachRemaining(entity -> convertEntityResolveDescendantsAndReferences(
				entityClass,
				results, datastorePersistentEntity, entity));
		return results;
	}

	private <T> void convertEntityResolveDescendantsAndReferences(Class<T> entityClass,
			List<T> results, DatastorePersistentEntity datastorePersistentEntity,
			BaseEntity entity) {
		T convertedObject = this.datastoreEntityConverter.read(entityClass, entity);
		results.add(convertedObject);

		datastorePersistentEntity
				.doWithDescendantProperties(descendantPersistentProperty -> {

					Class descendantType = descendantPersistentProperty
							.getComponentType();

					EntityQuery descendantQuery = Query.newEntityQueryBuilder()
							.setKind(this.datastoreMappingContext
									.getPersistentEntity(descendantType).kindName())
							.setFilter(PropertyFilter.hasAncestor((Key) entity.getKey()))
							.build();

					datastorePersistentEntity.getPropertyAccessor(convertedObject)
							.setProperty(descendantPersistentProperty,
									// Converting the collection type.
									this.datastoreEntityConverter.getConversions()
											.convertOnRead(
													convertEntitiesForRead(
															this.datastore
																	.run(descendantQuery),
															descendantType),
													descendantPersistentProperty
															.getType(),
													descendantType));
				});

		datastorePersistentEntity.doWithReferenceProperties(
				(PropertyHandler<DatastorePersistentProperty>) referencePersistentProperty -> {
					String fieldName = referencePersistentProperty.getFieldName();
					try {
						Object referenced;
						if (referencePersistentProperty.isCollectionLike()) {
							Class referencedType = referencePersistentProperty.getComponentType();
							List<Value<Key>> keyValues = entity.getList(fieldName);
							referenced = this.datastoreEntityConverter.getConversions()
									.convertOnRead(
											findAllById(
													keyValues.stream().map(x -> x.get())
															.collect(Collectors.toList()),
													referencedType),
											referencePersistentProperty.getType(),
											referencedType);
						}
						else {
							referenced = findById(entity.getKey(fieldName),
									referencePersistentProperty.getType());
						}
						datastorePersistentEntity.getPropertyAccessor(convertedObject)
								.setProperty(referencePersistentProperty, referenced);
					}
					catch (ClassCastException e) {
						throw new DatastoreDataException(
								"Reference properties must be stored Keys or lists of Keys"
										+ " in Cloud Datastore for singular or multiple references, respectively.");
					}
				});
	}

	private Key getKeyFromId(Object id, Class entityClass) {
		return this.objectToKeyFactory.getKeyFromId(id,
				this.datastoreMappingContext.getPersistentEntity(entityClass).kindName());
	}

	private Key getKey(Object entity, boolean allocateKey) {
		DatastorePersistentEntity datastorePersistentEntity = this.datastoreMappingContext
				.getPersistentEntity(entity.getClass());
		DatastorePersistentProperty idProp = datastorePersistentEntity
				.getIdPropertyOrFail();
		return datastorePersistentEntity.getPropertyAccessor(entity)
				.getProperty(idProp) == null && allocateKey
						? this.objectToKeyFactory.allocateKeyForObject(entity,
								datastorePersistentEntity)
						: this.objectToKeyFactory.getKeyFromObject(entity,
								datastorePersistentEntity);
	}

	private Key[] findAllKeys(Class entityClass) {
		Iterable<Key> keysFound = queryKeys(Query.newKeyQueryBuilder().setKind(
				this.datastoreMappingContext
						.getPersistentEntity(entityClass).kindName())
				.build());
		return StreamSupport.stream(keysFound.spliterator(),
				false).toArray(Key[]::new);
	}

	private <T> List<Key> getKeysFromIds(Iterable<?> ids, Class<T> entityClass) {
		List<Key> keys = new ArrayList<>();
		ids.forEach(x -> keys.add(getKeyFromId(x, entityClass)));
		return keys;
	}
}
