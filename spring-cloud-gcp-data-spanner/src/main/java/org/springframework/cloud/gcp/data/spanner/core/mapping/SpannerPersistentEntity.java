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

package org.springframework.cloud.gcp.data.spanner.core.mapping;

import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.MutablePersistentEntity;

/**
 * Cloud Spanner specific interface for a {@link MutablePersistentEntity} stored in a Cloud
 * Spanner table.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public interface SpannerPersistentEntity<T> extends
		MutablePersistentEntity<T, SpannerPersistentProperty>, ApplicationContextAware {

	/**
	 * Gets the name of the Cloud Spanner table.
	 * @return the name of the table.
	 */
	String tableName();

	/**
	 * Gets the column names stored for this entity.
	 * @return the column names.
	 */
	Set<String> columns();

	/**
	 * Gets the primary key properties in order. Embedded object properties containing key
	 * parts are represented as a single property.
	 * @return An array of the properties comprising the primary key in order.
	 */
	SpannerPersistentProperty[] getPrimaryKeyProperties();

	/**
	 * Gets the primary key properties in order. Embedded object properties containing key
	 * parts are flattened into their multiple primary key properties.
	 * @return An array of the properties comprising the primary key in order.
	 */
	List<SpannerPersistentProperty> getFlattenedPrimaryKeyProperties();

	/**
	 * Gets the SpannerMappingContext that can be used to create persistent entities of
	 * types that appear as properties of this entity.
	 * @return the SpannerMappingContext instance
	 */
	SpannerMappingContext getSpannerMappingContext();

	@Override
	SpannerCompositeKeyProperty getIdProperty();

	/**
	 * Applies the given {@link PropertyHandler} to all {@link SpannerPersistentProperty}s
	 * contained in this {@link SpannerPersistentProperty} that are collections of child
	 * entities.
	 *
	 * @param handler must not be {@literal null}.
	 */
	void doWithInterleavedProperties(
			PropertyHandler<SpannerPersistentProperty> handler);

	/**
	 * Applies the given {@link PropertyHandler} to all {@link SpannerPersistentProperty}s
	 * contained in this {@link SpannerPersistentProperty} that are stored as columns in
	 * the table for this entity.
	 *
	 * @param handler must not be {@literal null}.
	 */
	void doWithColumnBackedProperties(PropertyHandler<SpannerPersistentProperty> handler);
}
