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
	 * @return true if the property should be indexed
	 */
	boolean isUnindexed();

	/**
	 * Get the {@link EmbeddedType} of the property indicating what what type of embedding
	 * pathway will be used to store the property.
	 * @return the embedded type.
	 */
	EmbeddedType getEmbeddedType();

	/**
	 * True if the property is stored within Datastore entity
	 * @return true if the property is stored within Datastore entity
	 */
	boolean isColumnBacked();
}
