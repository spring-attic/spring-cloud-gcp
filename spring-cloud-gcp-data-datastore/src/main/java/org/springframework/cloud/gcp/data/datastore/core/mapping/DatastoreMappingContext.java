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

import com.google.common.annotations.VisibleForTesting;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

/**
 * A mapping context for Datastore that provides ways to create persistent entities and
 * properties.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class DatastoreMappingContext extends
		AbstractMappingContext<DatastorePersistentEntity<?>, DatastorePersistentProperty>
		implements ApplicationContextAware {

	private static final FieldNamingStrategy DEFAULT_NAMING_STRATEGY = PropertyNameFieldNamingStrategy.INSTANCE;

	private final FieldNamingStrategy fieldNamingStrategy = DEFAULT_NAMING_STRATEGY;

	private ApplicationContext applicationContext;

	public DatastoreMappingContext() {

	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@VisibleForTesting
	protected <T> DatastorePersistentEntityImpl<T> constructPersistentEntity(
			TypeInformation<T> typeInformation) {
		return new DatastorePersistentEntityImpl<>(typeInformation);
	}

	@Override
	protected <T> DatastorePersistentEntity<?> createPersistentEntity(
			TypeInformation<T> typeInformation) {
		DatastorePersistentEntityImpl<T> persistentEntity = constructPersistentEntity(
				typeInformation);
		if (this.applicationContext != null) {
			persistentEntity.setApplicationContext(this.applicationContext);
		}
		return persistentEntity;
	}

	@Override
	protected DatastorePersistentProperty createPersistentProperty(Property property,
			DatastorePersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
		return new DatastorePersistentPropertyImpl(property, owner, simpleTypeHolder,
				this.fieldNamingStrategy);
	}
}
