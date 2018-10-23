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

package org.springframework.cloud.gcp.data.spanner.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Mutation.Op;
import com.google.cloud.spanner.Mutation.WriteBuilder;

import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerEntityProcessor;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.util.Assert;

/**
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class SpannerMutationFactoryImpl implements SpannerMutationFactory {

	private final SpannerEntityProcessor spannerEntityProcessor;

	private final SpannerMappingContext spannerMappingContext;

	private final SpannerSchemaUtils spannerSchemaUtils;

	/**
	 * Constructor
	 * @param spannerEntityProcessor The object mapper used to convert between objects and Spanner
	 * data types.
	 * @param spannerMappingContext The mapping context used to get metadata from entity
	 * types.
	 */
	public SpannerMutationFactoryImpl(SpannerEntityProcessor spannerEntityProcessor,
			SpannerMappingContext spannerMappingContext,
			SpannerSchemaUtils spannerSchemaUtils) {
		Assert.notNull(spannerEntityProcessor,
				"A valid results mapper for Cloud Spanner is required.");
		Assert.notNull(spannerMappingContext,
				"A valid mapping context for Cloud Spanner is required.");
		Assert.notNull(spannerSchemaUtils,
				"A valid schema utils for Cloud Spanner is required.");
		this.spannerEntityProcessor = spannerEntityProcessor;
		this.spannerMappingContext = spannerMappingContext;
		this.spannerSchemaUtils = spannerSchemaUtils;
	}

	@Override
	public List<Mutation> insert(Object object) {
		return saveObject(Op.INSERT, object, null);
	}

	@Override
	public List<Mutation> upsert(Object object, Set<String> includeProperties) {
		return saveObject(Op.INSERT_OR_UPDATE, object,
				includeProperties == null ? null : includeProperties);
	}

	@Override
	public List<Mutation> update(Object object, Set<String> includeProperties) {
		return saveObject(Op.UPDATE, object,
				includeProperties == null ? null : includeProperties);
	}

	@Override
	public <T> Mutation delete(Class<T> entityClass, Iterable<? extends T> entities) {
		SpannerPersistentEntity<?> persistentEntity = this.spannerMappingContext
				.getPersistentEntity(entityClass);
		KeySet.Builder builder = KeySet.newBuilder();
		for (T entity : entities) {
			PersistentPropertyAccessor accessor = persistentEntity
					.getPropertyAccessor(entity);
			PersistentProperty idProperty = persistentEntity.getIdProperty();
			Key value = (Key) accessor.getProperty(idProperty);

			builder.addKey(value);
		}
		return delete(entityClass, builder.build());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Mutation delete(T object) {
		return delete((Class<T>) object.getClass(), Collections.singletonList(object));
	}

	@Override
	public Mutation delete(Class entityClass, KeySet keys) {
		SpannerPersistentEntity<?> persistentEntity = this.spannerMappingContext
				.getPersistentEntity(entityClass);
		return Mutation.delete(persistentEntity.tableName(), keys);
	}

	@Override
	public Mutation delete(Class entityClass, Key key) {
		return delete(entityClass, KeySet.singleKey(key));
	}

	private List<Mutation> saveObject(Op op, Object object,
			Set<String> includeProperties) {
		SpannerPersistentEntity<?> persistentEntity = this.spannerMappingContext
				.getPersistentEntity(object.getClass());
		List<Mutation> mutations = new ArrayList<>();
		Mutation.WriteBuilder writeBuilder = writeBuilder(op,
				persistentEntity.tableName());
		this.spannerEntityProcessor.write(object, writeBuilder::set, includeProperties);
		mutations.add(writeBuilder.build());

		persistentEntity.doWithInterleavedProperties(spannerPersistentProperty -> {
			if (includeProperties == null
					|| includeProperties.contains(spannerPersistentProperty.getName())) {

				Iterable kids = (Iterable) persistentEntity.getPropertyAccessor(object)
						.getProperty(spannerPersistentProperty);
				if (kids != null) {
					for (Object child : kids) {
						verifyChildHasParentId(persistentEntity, object,
								this.spannerMappingContext.getPersistentEntity(
										spannerPersistentProperty.getColumnInnerType()),
								child);
						mutations.addAll(saveObject(op, child, includeProperties));
					}
				}
			}
		});
		return mutations;
	}

	private void verifyChildHasParentId(SpannerPersistentEntity parentEntity,
			Object parentObject, SpannerPersistentEntity childEntity,
			Object childObject) {
		Iterator parentKeyParts = this.spannerSchemaUtils.getKey(parentObject).getParts()
				.iterator();
		Iterator childKeyParts = this.spannerSchemaUtils.getKey(childObject).getParts()
				.iterator();
		int partNum = 1;
		while (parentKeyParts.hasNext()) {
			if (!childKeyParts.hasNext()
					|| !parentKeyParts.next().equals(childKeyParts.next())) {
				throw new SpannerDataException(
						"A child entity's common primary key parts with its parent must "
								+ "have the same values. Primary key component " + partNum
								+ " does not match for entities: "
								+ parentEntity.getType() + " " + childEntity.getType());
			}
			partNum++;
		}
	}

	private WriteBuilder writeBuilder(Op op, String tableName) {
		Mutation.WriteBuilder builder = null;
		switch (op) {
		case INSERT:
			builder = Mutation.newInsertBuilder(tableName);
			break;
		case INSERT_OR_UPDATE:
			builder = Mutation.newInsertOrUpdateBuilder(tableName);
			break;
		case UPDATE:
			builder = Mutation.newUpdateBuilder(tableName);
			break;
		}
		if (builder == null) {
			throw new IllegalArgumentException(
					"Unsupported save-mutation operation: " + op);
		}
		return builder;
	}
}
