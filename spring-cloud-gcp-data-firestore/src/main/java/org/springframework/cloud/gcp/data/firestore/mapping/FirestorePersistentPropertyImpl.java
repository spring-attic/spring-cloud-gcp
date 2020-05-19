/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.firestore.mapping;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * Persistent property metadata implementation for Firestore.
 *
 * @author Dmitry Solomakha
 *
 * @since 1.2
 */
public class FirestorePersistentPropertyImpl
		extends AnnotationBasedPersistentProperty<FirestorePersistentProperty>
		implements FirestorePersistentProperty {

	/**
	 * Constructor.
	 *
	 * @param property the property to store
	 * @param owner the entity to which this property belongs
	 * @param simpleTypeHolder the type holder
	 */
	FirestorePersistentPropertyImpl(Property property,
			PersistentEntity<?, FirestorePersistentProperty> owner,
			SimpleTypeHolder simpleTypeHolder) {
		super(property, owner, simpleTypeHolder);
	}


	@Override
	protected Association<FirestorePersistentProperty> createAssociation() {
		return new Association<>(this, null);
	}

	@Override
	public boolean isIdProperty() {
		return findAnnotation(DocumentId.class) != null;
	}

	public String getFieldName() {
		PropertyName annotation = findAnnotation(PropertyName.class);
		return annotation != null ? annotation.value() : super.getName();
	}
}
