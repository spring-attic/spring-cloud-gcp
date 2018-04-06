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

package org.springframework.cloud.gcp.data.spanner.core.admin;

import com.google.cloud.spanner.Key;

import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.util.Assert;

/**
 *	Contains functions related to the table schema of entities.
 *
 * @author Chengyuan Zhao
 */
public class SpannerSchemaUtils {

	private final SpannerMappingContext mappingContext;

	private final SpannerConverter spannerConverter;

	public SpannerSchemaUtils(SpannerMappingContext mappingContext,
			SpannerConverter spannerConverter) {
		Assert.notNull(mappingContext,
				"A valid mapping context for Spanner is required.");
		Assert.notNull(spannerConverter,
				"A valid results mapper for Spanner is required.");
		this.mappingContext = mappingContext;
		this.spannerConverter = spannerConverter;
	}

	/**
	 * Gets the key for the given object.
	 * @param object
	 * @return
	 */
	public Key getId(Object object) {
		SpannerPersistentEntity persistentEntity = this.mappingContext
				.getPersistentEntity(object.getClass());
		return (Key) persistentEntity.getPropertyAccessor(object)
				.getProperty(persistentEntity.getIdProperty());
	}
}
