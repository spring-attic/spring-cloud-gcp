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

package org.springframework.cloud.gcp.data.spanner.repository.support;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.convert.ConversionUtils;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.repository.SpannerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

/**
 * @author Chengyuan Zhao
 */
public class SimpleSpannerRepository<T, ID> implements SpannerRepository<T, ID> {

	private final SpannerTemplate spannerTemplate;

	private final Class entityType;

	public SimpleSpannerRepository(SpannerTemplate spannerTemplate, Class entityType) {
		Assert.notNull(spannerTemplate, "A valid SpannerTemplate object is required.");
		Assert.notNull(entityType, "A valid entity type is required.");
		this.spannerTemplate = spannerTemplate;
		this.entityType = entityType;
	}

	@Override
	public SpannerOperations getSpannerTemplate() {
		return this.spannerTemplate;
	}

	@Override
	public <A> A performReadOnlyTransaction(
			Function<SpannerRepository<T, ID>, A> operations) {
		return this.spannerTemplate
				.performReadOnlyTransaction(
						transactionSpannerOperations -> operations
								.apply(new SimpleSpannerRepository<T, ID>(
										transactionSpannerOperations, this.entityType)),
						null);
	}

	@Override
	public <A> A performReadWriteTransaction(
			Function<SpannerRepository<T, ID>, A> operations) {
		return this.spannerTemplate
				.performReadWriteTransaction(transactionSpannerOperations -> operations
						.apply(new SimpleSpannerRepository<T, ID>(transactionSpannerOperations,
								this.entityType)));
	}

	@Override
	public Object save(Object entity) {
		Assert.notNull(entity, "A non-null entity is required for saving.");
		this.spannerTemplate.upsert(entity);
		return entity;
	}

	@Override
	public Iterable saveAll(Iterable entities) {
		Assert.notNull(entities, "A non-null list of entities is required for saving.");
		for (Object entity : entities) {
			save(entity);
		}
		return entities;
	}

	@Override
	public Optional findById(Object key) {
		Assert.notNull(key, "A non-null ID is required.");
		return doIfKey(key, k -> Optional
				.ofNullable(this.spannerTemplate.read(this.entityType, k)));
	}

	@Override
	public boolean existsById(Object key) {
		Assert.notNull(key, "A non-null ID is required.");
		return findById(key).isPresent();
	}

	@Override
	public Iterable findAll() {
		return this.spannerTemplate.readAll(this.entityType);
	}

	@Override
	public Iterable findAllById(Iterable iterable) {
		KeySet.Builder builder = KeySet.newBuilder();
		for (Object id : iterable) {
			doIfKey(id, k -> builder.addKey(k));
		}
		return this.spannerTemplate.read(this.entityType, builder.build());
	}

	@Override
	public long count() {
		return this.spannerTemplate.count(this.entityType);
	}

	@Override
	public void deleteById(Object key) {
		Assert.notNull(key, "A non-null ID is required.");
		doIfKey(key, k -> {
			this.spannerTemplate.delete(this.entityType, k);
			return null;
		});
	}

	@Override
	public void delete(Object entity) {
		Assert.notNull(entity, "A non-null entity is required.");
		this.spannerTemplate.delete(entity);
	}

	@Override
	public void deleteAll(Iterable entities) {
		Assert.notNull(entities, "A non-null list of entities is required.");
		this.spannerTemplate.delete(this.entityType, entities);
	}

	@Override
	public void deleteAll() {
		this.spannerTemplate.delete(this.entityType, KeySet.all());
	}

	@Override
	public Iterable findAll(Sort sort) {
		return this.spannerTemplate.queryAll(this.entityType, sort);
	}

	@Override
	public Page findAll(Pageable pageable) {
		return this.spannerTemplate.queryAll(this.entityType, pageable);
	}

	private <T> T doIfKey(Object key, Function<Key, T> operation) {
		Key k;
		boolean isIterable = Iterable.class.isAssignableFrom(key.getClass());
		boolean isArray = Object[].class.isAssignableFrom(key.getClass());
		if (isIterable || isArray) {
			Key.Builder kb = Key.newBuilder();
			for (Object keyPart : (isArray ? (Arrays.asList((Object[]) key))
					: ((Iterable) key))) {
				kb.appendObject(convertKeyPart(keyPart));
			}
			k = kb.build();
			if (k.size() == 0) {
				throw new SpannerDataException(
						"A key must have at least one component, but 0 were given.");
			}
		}
		else {
			k = Key.class.isAssignableFrom(key.getClass()) ? (Key) key
					: Key.of(convertKeyPart(key));
		}
		return operation.apply(k);
	}

	private Object convertKeyPart(Object object) {
		SpannerConverter spannerConverter = this.spannerTemplate.getSpannerConverter();
		if (spannerConverter
				.isValidSpannerKeyType(ConversionUtils.boxIfNeeded(object.getClass()))) {
			return object;
		}
		/*
		 * Iterate through the supported Key component types in the same order as the
		 * write converter. For example, if a type can be converted to both String and
		 * Double, we want both the this key conversion and the write converter to choose
		 * the same.
		 */
		for (Class validKeyType : spannerConverter.directlyWriteableSpannerTypes()) {
			if (!spannerConverter.isValidSpannerKeyType(validKeyType)) {
				continue;
			}
			if (spannerConverter.canConvert(object.getClass(),
					validKeyType)) {
				return spannerConverter.convert(object,
						validKeyType);
			}
		}
		throw new SpannerDataException(
				"The given object type couldn't be built into a Spanner Key: "
						+ object.getClass());
	}
}
