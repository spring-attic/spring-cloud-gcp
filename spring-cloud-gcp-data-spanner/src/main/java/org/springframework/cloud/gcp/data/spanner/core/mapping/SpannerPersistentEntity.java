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

import org.springframework.data.mapping.model.MutablePersistentEntity;

/**
 * Spanner specific interface for a {@link MutablePersistentEntity} stored
 * in a Google Spanner table.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 */
public interface SpannerPersistentEntity<T>
		extends MutablePersistentEntity<T, SpannerPersistentProperty> {

	/**
	 * Gets the name of the Spanner table.
	 * @return the name of the table.
	 */
	String tableName();

	/**
	 * Gets the property corresponding to the given column name.
	 * @param columnName the name of the column corresponding to a stored property.
	 * @return the property.
	 */
	SpannerPersistentProperty getPersistentPropertyByColumnName(String columnName);

	/**
	 * Gets the column names stored for this entity.
	 * @return the column names.
	 */
	Iterable<String> columns();
}
