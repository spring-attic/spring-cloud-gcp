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

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.util.StringUtils;

/**
 * Persistent property metadata implementation for Datastore.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class DatastorePersistentPropertyImpl
		extends AnnotationBasedPersistentProperty<DatastorePersistentProperty>
		implements DatastorePersistentProperty {

	private final FieldNamingStrategy fieldNamingStrategy;

	/**
	 * Constructor
	 *
	 * @param property the property to store
	 * @param owner the entity to which this property belongs
	 * @param simpleTypeHolder the type holder
	 * @param fieldNamingStrategy the naming strategy used to get the column name of this
	 * property
	 */
	DatastorePersistentPropertyImpl(Property property,
			PersistentEntity<?, DatastorePersistentProperty> owner,
			SimpleTypeHolder simpleTypeHolder, FieldNamingStrategy fieldNamingStrategy) {
		super(property, owner, simpleTypeHolder);
		this.fieldNamingStrategy = fieldNamingStrategy == null
				? PropertyNameFieldNamingStrategy.INSTANCE
				: fieldNamingStrategy;
		verify();
	}

	private void verify() {
		if (hasFieldAnnotation()
				&& (isDescendants() || isReference())) {
			throw new DatastoreDataException(
					"Property cannot be annotated as @Field if it is annotated @Descendants or @Reference: "
							+ getFieldName());
		}
		if (isDescendants() && isReference()) {
			throw new DatastoreDataException(
					"Property cannot be annotated both @Descendants and @Reference: "
							+ getFieldName());
		}
		if (isDescendants() && !isCollectionLike()) {
			throw new DatastoreDataException(
					"Only collection-like properties can contain the "
							+ "descendant entity objects can be annotated @Descendants.");
		}
	}

	@Override
	public String getFieldName() {
		if (StringUtils.hasText(getAnnotatedFieldName())) {
			return getAnnotatedFieldName();
		}
		return this.fieldNamingStrategy.getFieldName(this);
	}

	private boolean hasFieldAnnotation() {
		return findAnnotation(Field.class) != null;
	}

	@Override
	public boolean isReference() {
		return findAnnotation(Reference.class) != null;
	}

	@Override
	public boolean isDescendants() {
		return findAnnotation(Descendants.class) != null;
	}

	@Override
	public boolean isUnindexed() {
		return findAnnotation(Unindexed.class) != null;
	}

	@Override
	public boolean isColumnBacked() {
		return !isDescendants() && !isReference();
	}

	@Override
	public EmbeddedType getEmbeddedType() {
		return EmbeddedType.of(getTypeInformation());
	}

	@Override
	protected Association<DatastorePersistentProperty> createAssociation() {
		return new Association<>(this, null);
	}

	private String getAnnotatedFieldName() {

		Field annotation = findAnnotation(Field.class);

		if (annotation != null && StringUtils.hasText(annotation.name())) {
			return annotation.name();
		}

		return null;
	}
}
