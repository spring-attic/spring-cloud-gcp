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

import com.google.cloud.datastore.Value;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;

/**
 * An interface for type conversions on read and on write
 *
 * @author Dmitry Solomakha
 *
 * @since 1.1
 */
public interface ReadWriteConversions {
	/**
	 * Converts a Cloud Datastore {@link Value} to an object of a target type
	 * @param val Cloud Datastore Value.
	 * @param persistentProperty the target field information.
	 * @return an object of a target type.
	 */
	<T> T convertOnRead(Value val, DatastorePersistentProperty persistentProperty);

	/**
	 * Converts an object to a Cloud Datastore {@link Value}
	 * @param obj the objects to convert.
	 * @param persistentProperty the source field information.
	 * @return a Cloud Datastore value.
	 */
	Value convertOnWrite(Object obj, DatastorePersistentProperty persistentProperty);

	/**
	 * Registers {@link DatastoreEntityConverter} to be used for embedded entities
	 * @param datastoreEntityConverter the DatastoreEntityConverter.
	 */
	void registerEntityConverter(DatastoreEntityConverter datastoreEntityConverter);
}
