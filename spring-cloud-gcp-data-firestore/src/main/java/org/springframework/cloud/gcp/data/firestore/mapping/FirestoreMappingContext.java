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


import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

/**
 * A mapping context for Firestore that provides ways to create persistent entities and
 * properties.
 *
 * @author Dmitry Solomakha
 *
 * @since 1.2
 */
public class FirestoreMappingContext extends
		AbstractMappingContext<FirestorePersistentEntity<?>, FirestorePersistentProperty>
		implements ApplicationContextAware {

	@Override
	protected <T> FirestorePersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {
		return new FirestorePersistentEntityImpl<>(typeInformation);
	}

	@Override
	protected FirestorePersistentProperty createPersistentProperty(Property property,
			FirestorePersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
		return new FirestorePersistentPropertyImpl(property, owner, simpleTypeHolder);
	}
}
