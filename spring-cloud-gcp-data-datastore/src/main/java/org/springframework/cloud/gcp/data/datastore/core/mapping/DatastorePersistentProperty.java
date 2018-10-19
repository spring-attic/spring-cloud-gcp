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

package org.springframework.cloud.gcp.data.datastore.core.mapping;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.TypeInformation;

/**
 * Persistent property for Google Cloud Datastore
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public interface DatastorePersistentProperty
		extends PersistentProperty<DatastorePersistentProperty> {

	/**
	 * Get the name of the field to store this property in Datastore.
	 * @return the string name of the field.
	 */
	String getFieldName();

	/**
	 * True if the property is a POJO and is to be stored in Datastore as an embedded
	 * entity in the field.
	 * @return true if the property is stored in Datastore as an embedded entity.
	 */
	boolean isEmbedded();

	/**
	 * {@code true }if the property is an embedded {@code Map} and that the value type of
	 * the map is also an embedded entity.
	 * @return {@code true} if the embedded map's value type is also embedded.
	 * {@code false} otherwise.
	 */
	boolean embeddedMapValueIsEmbedded();

	/**
	 * True if the property is a POJO and is to be stored in Datastore as a Key of the
	 * POJO, which is a separate entity in Datastore.
	 * @return true if the property is stored in Datastore as a Key.
	 */
	boolean isReference();

	/**
	 * Whether the property contains entities that are related to this entity via the
	 * Cloud Datastore Ancestor relationship and have this entity as their ancestor.
	 * @return {@code true} if the property contains child entities. {@code false}
	 * otherwise.
	 */
	boolean isDescendants();

	/**
	 * True if the property should be excluded from indexes
	 * @return true if if the property should be indexed
	 */
	boolean isUnindexed();

	/**
	 * Get the value type of {@code Map} properties that are stored as embedded entities.
	 * @return the value type of the Map property.
	 */
	TypeInformation getEmbeddedMapValueType();
}
