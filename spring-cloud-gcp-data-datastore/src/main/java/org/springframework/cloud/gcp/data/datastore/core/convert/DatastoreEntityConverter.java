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

package org.springframework.cloud.gcp.data.datastore.core.convert;

import java.util.Map;

import com.google.cloud.datastore.BaseEntity;

import org.springframework.data.convert.EntityReader;
import org.springframework.data.convert.EntityWriter;
import org.springframework.data.util.TypeInformation;

/**
 * An interface for converting objects to Datastore Entities and vice versa.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public interface DatastoreEntityConverter extends
		EntityReader<Object, BaseEntity>, EntityWriter<Object, BaseEntity.Builder> {

	/**
	 * Get the {@link ReadWriteConversions} used in this converter.
	 * @return the conversions used.
	 */
	ReadWriteConversions getConversions();

	/**
	 * Read the entity as a {@link Map}.
	 * @param keyType the key type of the map to be read.
	 * @param componentType The value type of the map, into which each field value will be
	 * converted.
	 * @param entity The entity from Cloud Datastore.
	 * @return a Map where the key values are the field names and the values the field
	 * values.
	 */
	<T, R> Map<T, R> readAsMap(Class<T> keyType, TypeInformation<R> componentType,
			BaseEntity entity);
}
