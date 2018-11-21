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

import java.util.OptionalInt;
import java.util.OptionalLong;

import com.google.cloud.spanner.Type.Code;

import org.springframework.data.mapping.PersistentProperty;

/**
 * Interface for a {@link PersistentProperty} of a {@link SpannerPersistentEntity}
 * to be stored in a Google Cloud Spanner table.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public interface SpannerPersistentProperty
		extends PersistentProperty<SpannerPersistentProperty> {

	/**
	 * Gets the name of the column in the Google Cloud Spanner table mapped to this property.
	 *
	 * @return the name of the column.
	 */
	String getColumnName();

	/**
	 * Gets the inner type of the column, which is meaningful for columns of type ARRAY in
	 * Google Cloud Spanner.
	 * @return the inner type of the column.
	 */
	Class<?> getColumnInnerType();

	/**
	 * Gets the order of this column if it is part of the table's primary key. Will be
	 * empty or null if this column is not part of the primary key.
	 * @return An optional that is empty if no primary key order exists for the property.
	 */
	OptionalInt getPrimaryKeyOrder();

	/**
	 * Gets the maximum data length of the column if provided.
	 * @return An optional that is empty if no maximum length was provided.
	 */
	OptionalLong getMaxColumnLength();

	/**
	 * True if property corresponds to a column a Cloud Spanner
	 * table or child entities stored in Cloud Spanner.
	 * False otherwise.
	 * @return True if this property will be mapped to and from Cloud Spanner. False otherwise.
	 */
	boolean isMapped();

	/**
	 * True if the property is an embedded class containing more columns. False otherwise.
	 * @return true if the property is embedded.
	 */
	boolean isEmbedded();

	/**
	 * True if property is a collection of child entities. False otherwise.
	 * @return True if property is a collection of child entities corresponding to an interleaved
	 * child table. False otherwise.
	 */
	boolean isInterleaved();

	/**
	 * If the column's schema should be NOT NULL when generating a schema based on an
	 * entity class.
	 * @return {@code true} if the column should be NOT NULL in generated DDL.
	 * {@code false } otherwise.
	 */
	boolean isGenerateSchemaNotNull();

	/**
	 * Optionally directly specify the column type in Cloud Spanner. For ARRAY columns
	 * this refers to type of the item the array holds. If this is not specified then it
	 * is inferred.
	 * @return The user-specified column item type.
	 */
	Code getAnnotatedColumnItemType();
}
