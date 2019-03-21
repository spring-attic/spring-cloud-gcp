/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.datastore.core.mapping;

import java.util.List;

import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.MutablePersistentEntity;

/**
 * Persistent entity for Google Cloud Datastore.
 *
 * @param <T> the type of the property described by this persistent property
 * @author Chengyuan Zhao
 * @since 1.1
 */
public interface DatastorePersistentEntity<T> extends
		MutablePersistentEntity<T, DatastorePersistentProperty>, ApplicationContextAware {

	/**
	 * Gets the name of the Datastore Kind.
	 * @return the name of the Datastore Kind that stores these entities.
	 */
	String kindName();

	/**
	 * Gets the ID property, and will throw {@link DatastoreDataException} if the entity
	 * does not have an ID property.
	 * @return the ID property.
	 */
	DatastorePersistentProperty getIdPropertyOrFail();

	/**
	 * Get the name of the field for subtype discrimination if there is one.
	 * @return the name of the discrimination field. {@code null} if this persistent
	 * entity doesn't have one.
	 */
	String getDiscriminationFieldName();

	/**
	 * Get the discrimination value corresponding to this persistent entity type.
	 * @return the value or {@code null} if there is no value for this type.
	 */
	String getDiscriminatorValue();

	/**
	 * Get the discrimination values corresponding to this persistent entity ad its
	 * supertypes.
	 * @return the list of values where the first item is this entity's discrimination
	 * value
	 */
	List<String> getCompatibleDiscriminationValues();

	/**
	 * Applies the given {@link PropertyHandler} to all
	 * {@link DatastorePersistentProperty} contained in this
	 * {@link DatastorePersistentEntity} that are stored as columns in the table for this
	 * entity. This means properties backed by descendants or references to entities of
	 * other Kinds are not provided to the {@code handler}.
	 * @param handler must not be {@literal null}.
	 */
	void doWithColumnBackedProperties(
			PropertyHandler<DatastorePersistentProperty> handler);

	/**
	 * Applies the given {@link PropertyHandler} to all
	 * {@link DatastorePersistentProperty} contained in this
	 * {@link DatastorePersistentEntity} that are properties backed by descendants.
	 * @param handler must not be {@literal null}.
	 */
	void doWithDescendantProperties(PropertyHandler<DatastorePersistentProperty> handler);

}
