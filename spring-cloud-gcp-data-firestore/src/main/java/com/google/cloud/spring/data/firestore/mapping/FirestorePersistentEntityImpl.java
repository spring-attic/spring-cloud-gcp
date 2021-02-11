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

package com.google.cloud.spring.data.firestore.mapping;

import com.google.cloud.Timestamp;
import com.google.cloud.spring.data.firestore.Document;
import com.google.cloud.spring.data.firestore.FirestoreDataException;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;

/**
 * Metadata class for entities stored in Datastore.
 * @param <T> the type of the persistent entity
 *
 * @author Dmitry Solomakha
 * @since 1.2
 */
public class FirestorePersistentEntityImpl<T>
		extends BasicPersistentEntity<T, FirestorePersistentProperty>
		implements FirestorePersistentEntity<T> {

	private final String collectionName;

	private FirestorePersistentProperty updateTimeProperty;

	public FirestorePersistentEntityImpl(TypeInformation<T> information) {
		super(information);
		this.collectionName = getEntityCollectionName(information);
	}

	@Override
	public String collectionName() {
		return this.collectionName;
	}

	@Override
	public FirestorePersistentProperty getIdPropertyOrFail() {
		FirestorePersistentProperty idProperty = getIdProperty();
		if (idProperty == null) {
			throw new FirestoreDataException(
					"An ID property was required but does not exist for the type: "
							+ getType());
		}
		if (idProperty.getType() != String.class) {
			throw new FirestoreDataException(
							"An ID property is expected to be of String type; was " + idProperty.getType());
		}
		return idProperty;
	}

	@Override
	public FirestorePersistentProperty getUpdateTimeProperty() {
		return updateTimeProperty;
	}

	private static <T> String getEntityCollectionName(TypeInformation<T> typeInformation) {
		Document document = AnnotationUtils.findAnnotation(typeInformation.getType(), Document.class);
		String collectionName = (String) AnnotationUtils.getValue(document, "collectionName");

		if (StringUtils.isEmpty(collectionName)) {
			// Infer the collection name as the uncapitalized document name.
			return StringUtils.uncapitalize(typeInformation.getType().getSimpleName());
		}
		else {
			return collectionName;
		}
	}

	@Override
	public void addPersistentProperty(FirestorePersistentProperty property) {
		super.addPersistentProperty(property);
		if (property.findAnnotation(UpdateTime.class) != null) {
			if (property.getActualType() != Timestamp.class) {
				throw new FirestoreDataException("@UpdateTime annotated field should be of com.google.cloud.Timestamp type");
			}
			updateTimeProperty = property;
		}
	}
}
