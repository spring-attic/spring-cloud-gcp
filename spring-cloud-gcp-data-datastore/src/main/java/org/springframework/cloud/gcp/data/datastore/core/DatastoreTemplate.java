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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.datastore.BaseEntity;
import com.google.cloud.datastore.BaseKey;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreReaderWriter;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Entity.Builder;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyValue;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Value;

import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreEntityConverter;
import org.springframework.cloud.gcp.data.datastore.core.convert.ObjectToKeyFactory;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.cloud.gcp.data.datastore.core.util.ValueUtil;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.TypeUtils;

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
	public <T> T save(T instance, Key... ancestors) {
		getDatastoreReadWriter().put(convertToEntityForSave(instance, ancestors));
		return instance;
	}

	@Override
	public <T> Iterable<T> saveAll(Iterable<T> entities, Key... ancestors) {
		getDatastoreReadWriter().put(StreamSupport.stream(entities.spliterator(), false)
				.map(entity -> convertToEntityForSave(entity, ancestors)).toArray(Entity[]::new));
		return entities;
	}

	@Override
	public <T> void deleteById(Object id, Class<T> entityClass) {
		getDatastoreReadWriter().delete(getKeyFromId(id, entityClass));
	}

	@Override
	public <T> void deleteAllById(Iterable<?> ids, Class<T> entityClass) {
		List<Key> keys = getKeysFromIds(ids, entityClass);
		getDatastoreReadWriter().delete(keys.toArray(new Key[keys.size()]));
	}

	@Override
	public <T> void delete(T entity) {
		getDatastoreReadWriter().delete(getKey(entity, false));
	}

	@Override
	public <T> void deleteAll(Iterable<T> entities) {
		getDatastoreReadWriter()
				.delete(StreamSupport.stream(entities.spliterator(), false)
				.map(x -> getKey(x, false)).toArray(Key[]::new));
	}

	@Override
	public long deleteAll(Class<?> entityClass) {
		Key[] keysToDelete = findAllKeys(entityClass);
		getDatastoreReadWriter().delete(keysToDelete);
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
				getDatastoreReadWriter().get(
						keysToFind.toArray(new Key[keysToFind.size()])),
				entityClass);
	}

	@Override
	public <T> Iterable<T> query(Query<? extends BaseEntity> query,
			Class<T> entityClass) {
		return convertEntitiesForRead(getDatastoreReadWriter().run(query), entityClass);
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
		QueryResults results = getDatastoreReadWriter().run(query);
		if (results.getResultClass() == Key.class) {
			return () -> getDatastoreReadWriter().run(query);
		}
		return convertEntitiesForRead(results, entityClass);
	}

	@Override
	public <A, T> Iterable<T> query(Query<A> query, Function<A, T> entityFunc) {
		List<T> results = new ArrayList<>();
		getDatastoreReadWriter().run(query)
				.forEachRemaining(x -> results.add(entityFunc.apply(x)));
		return results;
	}

	@Override
	public Iterable<Key> queryKeys(Query<Key> query) {
		return () -> getDatastoreReadWriter().run(query);
	}

	@Override
	public <T> Collection<T> findAll(Class<T> entityClass) {
		return findAll(entityClass, null);
	}

	@Override
	public <T> Collection<T> findAll(Class<T> entityClass, DatastoreQueryOptions queryOptions) {
		DatastorePersistentEntity<?> persistentEntity = this.datastoreMappingContext.getPersistentEntity(entityClass);
		EntityQuery.Builder builder = Query.newEntityQueryBuilder()
				.setKind(persistentEntity.kindName());
		applyQueryOptions(builder, queryOptions, persistentEntity);

		return convertEntitiesForRead(getDatastoreReadWriter().run(builder.build()), entityClass);
	}

	public static void applyQueryOptions(StructuredQuery.Builder builder, DatastoreQueryOptions queryOptions,
			DatastorePersistentEntity<?> persistentEntity) {
		if (queryOptions == null) {
			return;
		}
		if (queryOptions.getLimit() != null) {
			builder.setLimit(queryOptions.getLimit());
		}
		if (queryOptions.getOffset() != null) {
			builder.setOffset(queryOptions.getOffset());
		}
		if (queryOptions.getSort() != null && persistentEntity != null) {
			queryOptions.getSort().stream()
					.map(order -> createOrderBy(persistentEntity, order))
					.forEachOrdered(orderBy -> builder.addOrderBy(orderBy));
		}
	}

	@Override
	public <T> boolean existsById(Object id, Class<T> entityClass) {
		return findById(id, entityClass) != null;
	}

	@Override
	public <A> A performTransaction(Function<DatastoreOperations, A> operations) {
		if (!(getDatastoreReadWriter() instanceof Datastore)) {
			throw new DatastoreDataException(
					"This DatastoreReadWriter cannot be used to run transactions. A full Datastore service"
							+ " object is required to run functions as transactions. Ensure that this method "
							+ "was not called in an ongoing transaction.");
		}
		return ((Datastore) getDatastoreReadWriter())
				.runInTransaction(
				(DatastoreReaderWriter readerWriter) -> operations.apply(new DatastoreTemplate(readerWriter,
						DatastoreTemplate.this.datastoreEntityConverter,
						DatastoreTemplate.this.datastoreMappingContext,
						DatastoreTemplate.this.objectToKeyFactory)));
	}

	@Override
	public <T> Map<String, T> findByIdAsMap(Key key, Class<T> valueType) {
		Assert.notNull(key, "A non-null Key is required.");
		Assert.notNull(valueType, "A non-null valueType is required.");

		Entity entity = getDatastoreReadWriter().get(key);
		return this.datastoreEntityConverter.readAsMap(String.class, ClassTypeInformation.from(valueType), entity);
	}

	@Override
	public <V> void writeMap(Key datastoreKey, Map<String, V> map) {
		Assert.notNull(datastoreKey, "A non-null Key is required.");
		Assert.notNull(map, "A non-null map is required.");

		Builder builder = Entity.newBuilder(datastoreKey);
		map.forEach(
				(key, value) ->
						builder.set(key, this.datastoreEntityConverter.getConversions().convertOnWriteSingle(value)));
		Entity entity = builder.build();
		getDatastoreReadWriter().put(entity);
	}

	@Override
	public Key createKey(String kind, Object id) {
		return this.objectToKeyFactory.getKeyFromId(id, kind);
	}

	private static StructuredQuery.OrderBy createOrderBy(DatastorePersistentEntity<?> persistentEntity,
			Sort.Order order) {
		return new StructuredQuery.OrderBy(
				persistentEntity.getPersistentProperty(order.getProperty()).getFieldName(),
				order.getDirection() == Sort.Direction.DESC
						? StructuredQuery.OrderBy.Direction.DESCENDING
						: StructuredQuery.OrderBy.Direction.ASCENDING);
	}

	private Entity convertToEntityForSave(Object entity, Key... ancestors) {
		if (ancestors != null) {
			for (Key ancestor : ancestors) {
				validateKey(entity, keyToPathElement(ancestor));
			}
		}
		Key key = getKey(entity, true, ancestors);
		Builder builder = Entity.newBuilder(key);
		this.datastoreEntityConverter.write(entity, builder);
		saveDescendents(entity, key);
		saveReferences(entity, builder);
		return builder.build();
	}

	private void saveReferences(Object entity, Builder builder) {
		DatastorePersistentEntity datastorePersistentEntity = this.datastoreMappingContext
				.getPersistentEntity(entity.getClass());
		datastorePersistentEntity.doWithReferenceProperties(persistentProperty -> {
			PersistentPropertyAccessor accessor = datastorePersistentEntity.getPropertyAccessor(entity);
			Object val = accessor.getProperty(persistentProperty);
			if (val == null) {
				return;
			}
			Value<?> value;
			if (persistentProperty.isCollectionLike()) {
				Iterable<?> iterableVal = (Iterable<?>) ValueUtil.toIterableIfArray(val);
				saveAll(iterableVal);
				List<KeyValue> keyValues = StreamSupport.stream((iterableVal).spliterator(), false)
						.map(o -> KeyValue.of(this.getKey(o, false)))
						.collect(Collectors.toList());
				value = ListValue.of(keyValues);
			}
			else {
				save(val);
				Key key = getKey(val, false);
				value = KeyValue.of(key);
			}
			builder.set(((DatastorePersistentProperty) persistentProperty).getFieldName(), value);
		});
	}

	private void saveDescendents(Object entity, Key key) {
		DatastorePersistentEntity datastorePersistentEntity = this.datastoreMappingContext
				.getPersistentEntity(entity.getClass());
		datastorePersistentEntity.doWithDescendantProperties(
				(PersistentProperty persistentProperty) -> {
					//Convert and write descendants, applying ancestor from parent entry
					PersistentPropertyAccessor accessor = datastorePersistentEntity.getPropertyAccessor(entity);
					Object val = accessor.getProperty(persistentProperty);
					if (val != null) {
						//we can be sure that the property is an array or an iterable,
						//because we check it in isDescendant
						saveAll((Iterable<?>) ValueUtil.toIterableIfArray(val), key);
					}
				});
	}

	public static PathElement keyToPathElement(Key key) {
		Assert.notNull(key, "A non-null key is required");
		return key.getName() != null
				? PathElement.of(key.getKind(), key.getName())
				: PathElement.of(key.getKind(), key.getId());
	}

	private void validateKey(Object entity, PathElement ancestorPE) {
		DatastorePersistentEntity datastorePersistentEntity =
				this.datastoreMappingContext.getPersistentEntity(entity.getClass());
		DatastorePersistentProperty idProp = datastorePersistentEntity.getIdPropertyOrFail();

		if (!TypeUtils.isAssignable(BaseKey.class, idProp.getType())) {
			throw new DatastoreDataException("Only Key types are allowed for descendants id");
		}

		Key key = getKey(entity, false);
		if (key == null || key.getAncestors().stream().anyMatch(pe -> pe.equals(ancestorPE))) {
			return;
		}
		throw new DatastoreDataException("Descendant object has a key without current ancestor");
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

		resolveDescendantProperties(datastorePersistentEntity, entity, convertedObject);
		resolveReferenceProperties(datastorePersistentEntity, entity, convertedObject);
	}

	private <T> void resolveReferenceProperties(DatastorePersistentEntity datastorePersistentEntity,
			BaseEntity entity, T convertedObject) {
		datastorePersistentEntity.doWithReferenceProperties(
				(PropertyHandler<DatastorePersistentProperty>) referencePersistentProperty -> {
					Object referenced = findReferenced(entity, referencePersistentProperty);
					if (referenced != null) {
						datastorePersistentEntity.getPropertyAccessor(convertedObject)
								.setProperty(referencePersistentProperty, referenced);
					}

				});
	}

	private Object findReferenced(BaseEntity entity, DatastorePersistentProperty referencePersistentProperty) {
		String fieldName = referencePersistentProperty.getFieldName();
		try {
			Object referenced;
			if (!entity.contains(fieldName)) {
				referenced = null;
			}
			else if (referencePersistentProperty.isCollectionLike()) {
				Class referencedType = referencePersistentProperty.getComponentType();
				List<Value<Key>> keyValues = entity.getList(fieldName);
				referenced = this.datastoreEntityConverter.getConversions()
						.convertOnRead(
								findAllById(
										keyValues.stream().map(Value::get).collect(Collectors.toList()),
										referencedType),
								referencePersistentProperty.getType(),
								referencedType);
			}
			else {
				referenced = findById(entity.getKey(fieldName), referencePersistentProperty.getType());
			}
			return referenced;
		}
		catch (ClassCastException e) {
				throw new DatastoreDataException(
					"Error loading reference property " + fieldName + "."
							+ "Reference properties must be stored as Keys or lists of Keys"
							+ " in Cloud Datastore for singular or multiple references, respectively.");
			}
	}

	private <T> void resolveDescendantProperties(DatastorePersistentEntity datastorePersistentEntity,
			BaseEntity entity, T convertedObject) {
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
															getDatastoreReadWriter()
																	.run(descendantQuery),
															descendantType),
													descendantPersistentProperty
															.getType(),
													descendantType));
				});
	}

	private Key getKeyFromId(Object id, Class entityClass) {
		return this.objectToKeyFactory.getKeyFromId(id,
				this.datastoreMappingContext.getPersistentEntity(entityClass).kindName());
	}

	private Key getKey(Object entity, boolean allocateKey, Key... ancestors) {
		DatastorePersistentEntity datastorePersistentEntity = this.datastoreMappingContext
				.getPersistentEntity(entity.getClass());
		DatastorePersistentProperty idProp = datastorePersistentEntity
				.getIdPropertyOrFail();
		if (datastorePersistentEntity.getPropertyAccessor(entity).getProperty(idProp) == null && allocateKey) {
			return this.objectToKeyFactory.allocateKeyForObject(entity, datastorePersistentEntity, ancestors);
		}
		return this.objectToKeyFactory.getKeyFromObject(entity, datastorePersistentEntity);
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

	private DatastoreReaderWriter getDatastoreReadWriter() {
		return TransactionSynchronizationManager.isActualTransactionActive()
				? ((DatastoreTransactionManager.Tx) ((DefaultTransactionStatus) TransactionAspectSupport
						.currentTransactionStatus()).getTransaction()).getTransaction()
				: this.datastore;
	}
}
