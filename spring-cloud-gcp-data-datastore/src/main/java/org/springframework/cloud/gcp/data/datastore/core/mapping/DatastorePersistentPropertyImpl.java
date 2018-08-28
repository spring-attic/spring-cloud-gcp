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

import java.util.List;

import org.springframework.data.annotation.Reference;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;
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
		if (isEmbedded() && isReference()) {
			throw new DatastoreDataException(
					"Property cannot be annotated both Embedded and Reference: "
							+ getFieldName());
		}
	}

	@Override
	public String getFieldName() {
		if (StringUtils.hasText(getAnnotatedFieldName())) {
			return getAnnotatedFieldName();
		}
		return this.fieldNamingStrategy.getFieldName(this);
	}

	@Override
	public Class<?> getIterableInnerType() {
		TypeInformation<?> ti = getTypeInformation();
		List<TypeInformation<?>> typeParams = ti.getTypeArguments();
		if (typeParams.size() != 1) {
			throw new DatastoreDataException("in field '" + getFieldName()
					+ "': Unsupported number of type parameters found: "
					+ typeParams.size()
					+ " Only collections of exactly 1 type parameter are supported.");
		}
		return typeParams.get(0).getType();
	}

	@Override
	public boolean isIterable() {
		return Iterable.class.isAssignableFrom(getType());
	}

	@Override
	public boolean isReference() {
		return findAnnotation(Reference.class) != null;
	}

	@Override
	public boolean isEmbedded() {
		return findAnnotation(Embedded.class) != null;
	}

	@Override
	public boolean isUnindexed() {
		return findAnnotation(Unindexed.class) != null;
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
