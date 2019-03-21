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

package org.springframework.cloud.gcp.autoconfigure.spanner;

import java.io.Serializable;
import java.util.StringJoiner;

import com.google.cloud.spanner.Key;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter;

/**
 * Settings for key converter used in REST repositories.
 *
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
public class SpannerKeyIdConverter implements BackendIdConverter {

	private SpannerMappingContext mappingContext;

	public SpannerKeyIdConverter(SpannerMappingContext mappingContext) {
		this.mappingContext = mappingContext;
	}

	@Override
	public Serializable fromRequestId(String id, Class<?> entityType) {
		if (id == null) {
			return null;
		}
		Object[] parts = id.split(getUrlIdSeparator());
		return Key.of(parts);
	}

	protected String getUrlIdSeparator() {
		return ",";
	}

	@Override
	public String toRequestId(Serializable source, Class<?> entityType) {
		Key id = (Key) source;
		StringJoiner stringJoiner = new StringJoiner(getUrlIdSeparator());
		id.getParts().forEach((p) -> stringJoiner.add(p.toString()));
		return stringJoiner.toString();
	}

	@Override
	public boolean supports(Class<?> type) {
		SpannerPersistentEntity<?> persistentEntity = this.mappingContext
				.getPersistentEntity(type);

		return persistentEntity != null && persistentEntity.getIdProperty() != null
				&& persistentEntity.getIdProperty().getActualType().equals(Key.class);
	}

}
