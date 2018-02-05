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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;

/**
 * Represents a Google Spanner table and its columns' mapping to fields within an entity type.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 */
public class SpannerPersistentEntityImpl<T>
		extends BasicPersistentEntity<T, SpannerPersistentProperty>
		implements SpannerPersistentEntity<T> {

	private final String tableName;

	private final Set<String> columnNames = new HashSet<>();

	private final Map<String, String> columnNameToPropertyName = new HashMap<>();

	/**
	 * Creates a {@link SpannerPersistentEntityImpl}
	 * @param information type information about the underlying entity type.
	 */
	public SpannerPersistentEntityImpl(TypeInformation<T> information) {
		super(information);

		Class<?> rawType = information.getType();
		String fallback = StringUtils.uncapitalize(rawType.getSimpleName());

		SpannerTable table = this.findAnnotation(SpannerTable.class);
		this.tableName = table != null && StringUtils.hasText(table.name()) ? table.name() : fallback;
	}

	@Override
	public void addPersistentProperty(SpannerPersistentProperty property) {
		addPersistentPropertyToPersistentEntity(property);
		this.columnNames.add(property.getColumnName());
		this.columnNameToPropertyName.put(property.getColumnName(), property.getName());
	}

	private void addPersistentPropertyToPersistentEntity(SpannerPersistentProperty property) {
		super.addPersistentProperty(property);
	}

	@Override
	public String tableName() {
		return this.tableName;
	}

	@Override
	public SpannerPersistentProperty getPersistentPropertyByColumnName(
			String columnName) {
		return getPersistentProperty(this.columnNameToPropertyName.get(columnName));
	}

	@Override
	public Iterable<String> columns() {
		return Collections.unmodifiableSet(this.columnNames);
	}
}
