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

import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Mutation.Op;
import com.google.cloud.spanner.Mutation.WriteBuilder;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerObjectMapper;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.util.Assert;

/**
 * @author Chengyuan Zhao
 */
public class SpannerMutationFactoryImpl implements SpannerMutationFactory {

	private final SpannerObjectMapper spannerObjectMapper;

	private final SpannerMappingContext spannerMappingContext;

	/**
	 * Constructor
	 * @param spannerObjectMapper The object mapper used to convert between objects and
	 * Spanner data types.
	 * @param spannerMappingContext The mapping context used to get metadata from entity
	 * types.
	 */
	public SpannerMutationFactoryImpl(SpannerObjectMapper spannerObjectMapper,
			SpannerMappingContext spannerMappingContext) {
		Assert.notNull(spannerObjectMapper,
				"A valid results mapper for Spanner is required.");
		Assert.notNull(spannerMappingContext,
				"A valid mapping context for Spanner is required.");
		this.spannerObjectMapper = spannerObjectMapper;
		this.spannerMappingContext = spannerMappingContext;
	}

	@Override
	public <T> Mutation insert(T object) {
		return saveObject(Op.INSERT, object);
	}

	@Override
	public <T> Mutation upsert(T object) {
		return saveObject(Op.INSERT_OR_UPDATE, object);
	}

	@Override
	public <T> Mutation update(T object) {
		return saveObject(Op.UPDATE, object);
	}

	@Override
	public <T> Mutation delete(Class<T> entityClass, Iterable<? extends T> entities) {
		return deleteKeySetFromTable(entityClass, (persistentEntity) -> {
			KeySet.Builder builder = KeySet.newBuilder();
			for (T entity : entities) {
				final PersistentPropertyAccessor accessor = persistentEntity
						.getPropertyAccessor(entity);
				PersistentProperty idProperty = persistentEntity.getIdProperty();
				Object value = accessor.getProperty(idProperty);

				builder.addKey(Key.of(value));
			}
			return builder.build();
		});
	}

	@Override
	public <T> Mutation delete(T object) {
		return deleteKeyFromTable(object.getClass(), (persistentEntity) -> {
			PersistentPropertyAccessor accessor = persistentEntity
					.getPropertyAccessor(object);

			PersistentProperty idProperty = persistentEntity.getIdProperty();
			Object value = accessor.getProperty(idProperty);
			return Key.of(value);
		});
	}

	@Override
	public Mutation delete(Class entityClass, KeySet keys) {
		return deleteKeySetFromTable(entityClass, (unused) -> keys);
	}

	@Override
	public Mutation delete(Class entityClass, Key key) {
		return deleteKeyFromTable(entityClass, (unused) -> key);
	}

	private <T> Mutation createMutationWithClass(Class entityClass,
			Function<SpannerPersistentEntity, T> function,
			BiFunction<String, T, Mutation> tableNameMutationFunction) {
		SpannerPersistentEntity<?> persistentEntity = this.spannerMappingContext
				.getPersistentEntity(entityClass);
		return tableNameMutationFunction.apply(persistentEntity.tableName(),
				function.apply(persistentEntity));
	}

	private Mutation deleteKeyFromTable(Class entityClass,
			Function<SpannerPersistentEntity, Key> keyFunction) {
		return createMutationWithClass(entityClass, keyFunction, Mutation::delete);
	}

	private Mutation deleteKeySetFromTable(Class entityClass,
			Function<SpannerPersistentEntity, KeySet> keyFunction) {
		return createMutationWithClass(entityClass, keyFunction, Mutation::delete);
	}

	private <T> Mutation saveObject(Op op, T object) {
		SpannerPersistentEntity<?> persistentEntity = this.spannerMappingContext
				.getPersistentEntity(object.getClass());
		Mutation.WriteBuilder writeBuilder = writeBuilder(op,
				persistentEntity.tableName());
		this.spannerObjectMapper.write(object, writeBuilder);
		return writeBuilder.build();
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
		case REPLACE:
			builder = Mutation.newReplaceBuilder(tableName);
			break;
		}
		if (builder == null) {
			throw new IllegalArgumentException(
					"Unsupported save-mutation operation: " + op);
		}
		return builder;
	}
}
