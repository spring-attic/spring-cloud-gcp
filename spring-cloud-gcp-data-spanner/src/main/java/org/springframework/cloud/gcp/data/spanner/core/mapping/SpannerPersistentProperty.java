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

import org.springframework.data.mapping.PersistentProperty;

/**
 * Interface for a {@link PersistentProperty} of a {@link SpannerPersistentEntity}
 * to be stored in a Google Spanner table.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 */
public interface SpannerPersistentProperty
		extends PersistentProperty<SpannerPersistentProperty> {

	/**
	 * Gets the name of the column in the Google Spanner table mapped to this property.
	 *
	 * @return the name of the column.
	 */
	String getColumnName();

	/**
	 * Gets the inner type of the column, which is meaningful for columns of type ARRAY in Google
	 * Spanner.
	 * @return the inner type of the column. Returns null if no inner type is specified by annotation.
	 */
	Class getColumnInnerType();

	/**
	 * Gets the order of this column if it is part of the table's primary key. Will be
	 * empty or null if this column is not part of the primary key.
	 * @return
	 */
	OptionalInt getPrimaryKeyOrder();

	/**
	 * True if property corresponds to a column a Spanner table or child entities stored in Spanner.
	 * False otherwise.
	 * @return True if this property will be mapped to and from Spanner. False otherwise.
	 */
	boolean isMapped();
}
