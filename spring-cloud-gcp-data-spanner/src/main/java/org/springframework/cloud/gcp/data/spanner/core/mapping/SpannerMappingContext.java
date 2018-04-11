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

package org.springframework.cloud.gcp.data.spanner.core.mapping;

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
 * A mapping context for Google Spanner that provides ways to create persistent entities
 * and properties.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 * @author Balint Pato
 */
public class SpannerMappingContext extends
		AbstractMappingContext<SpannerPersistentEntity<?>, SpannerPersistentProperty> implements
		ApplicationContextAware {

	private static final FieldNamingStrategy DEFAULT_NAMING_STRATEGY = PropertyNameFieldNamingStrategy.INSTANCE;

	private FieldNamingStrategy fieldNamingStrategy = DEFAULT_NAMING_STRATEGY;

	private ApplicationContext applicationContext;

	public SpannerMappingContext() {

	}

	/**
	 * Set the field naming strategy used when creating persistent properties.
	 * @param fieldNamingStrategy the field naming strategy passed used by created persistent
	 * properties get column names.
	 */
	public void setFieldNamingStrategy(FieldNamingStrategy fieldNamingStrategy) {
		this.fieldNamingStrategy = fieldNamingStrategy == null ? DEFAULT_NAMING_STRATEGY
				: fieldNamingStrategy;
	}

	/**
	 * Gets the field naming strategy used by this mapping context.
	 * @return
	 */
	public FieldNamingStrategy getFieldNamingStrategy() {
		return this.fieldNamingStrategy;
	}

	@Override
	protected <T> SpannerPersistentEntity<T> createPersistentEntity(
			TypeInformation<T> typeInformation) {
		SpannerPersistentEntityImpl<T> persistentEntity = constructPersistentEntity(typeInformation);
		if (this.applicationContext != null) {
			persistentEntity.setApplicationContext(this.applicationContext);
		}
		return persistentEntity;
	}

	@VisibleForTesting
	protected <T> SpannerPersistentEntityImpl<T> constructPersistentEntity(
			TypeInformation<T> typeInformation) {
		return new SpannerPersistentEntityImpl<>(typeInformation);
	}

	@Override
	protected SpannerPersistentProperty createPersistentProperty(Property property,
			SpannerPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
		return new SpannerPersistentPropertyImpl(property, owner, simpleTypeHolder,
				this.fieldNamingStrategy);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
