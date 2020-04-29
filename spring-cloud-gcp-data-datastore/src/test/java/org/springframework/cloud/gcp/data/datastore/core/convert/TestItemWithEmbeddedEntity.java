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

package org.springframework.cloud.gcp.data.datastore.core.convert;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.cloud.gcp.data.datastore.core.convert.TestDatastoreItem.Color;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.cloud.gcp.data.datastore.entities.CustomMap;

/**
 * A test entity focused on holding embedded entities.
 *
 * @author Dmitry Solomakha
 * @author Chengyuan Zhao
 */
public class TestItemWithEmbeddedEntity {
	private int intField;

	private EmbeddedEntity embeddedEntityField;

	private List<EmbeddedEntity> listOfEmbeddedEntities;

	private Map<String, String> embeddedMapSimpleValues;

	private Map<String, String[]> embeddedMapListOfValues;

	private Map<String, EmbeddedEntity> embeddedEntityMapEmbeddedEntity;

	private Map<String, List<EmbeddedEntity>> embeddedEntityMapListOfEmbeddedEntities;

	private Map<String, Map<Long, Map<String, String>>> nestedEmbeddedMaps;

	private CustomMap customMap;

	private Map<TestDatastoreItem.Color, String> mapEnumKeys;

	TestItemWithEmbeddedEntity(int intField, EmbeddedEntity embeddedEntityField,
			List<EmbeddedEntity> listOfEmbeddedEntities,
			Map<String, String> embeddedMapSimpleValues,
			Map<String, String[]> embeddedMapListOfValues,
			Map<String, EmbeddedEntity> embeddedEntityMapEmbeddedEntity,
			Map<String, List<EmbeddedEntity>> embeddedEntityMapListOfEmbeddedEntities,
			Map<Color, String> mapEnumKeys, CustomMap customMap) {
		this.intField = intField;
		this.embeddedEntityField = embeddedEntityField;
		this.listOfEmbeddedEntities = listOfEmbeddedEntities;
		this.embeddedMapSimpleValues = embeddedMapSimpleValues;
		this.embeddedMapListOfValues = embeddedMapListOfValues;
		this.embeddedEntityMapEmbeddedEntity = embeddedEntityMapEmbeddedEntity;
		this.embeddedEntityMapListOfEmbeddedEntities = embeddedEntityMapListOfEmbeddedEntities;
		this.mapEnumKeys = mapEnumKeys;
		this.customMap = customMap;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TestItemWithEmbeddedEntity that = (TestItemWithEmbeddedEntity) o;

		boolean mapListValuesEquals = true;
		boolean mapListEmbeddedEntitiesEquals = true;

		for (String key : this.embeddedMapListOfValues.keySet()) {
			mapListValuesEquals = mapListValuesEquals
					&& Arrays.equals(this.embeddedMapListOfValues.get(key),
							that.embeddedMapListOfValues.get(key));
		}

		for (String key : this.embeddedEntityMapListOfEmbeddedEntities.keySet()) {
			mapListEmbeddedEntitiesEquals = mapListEmbeddedEntitiesEquals && Objects
					.equals(this.embeddedEntityMapListOfEmbeddedEntities.get(key),
							that.embeddedEntityMapListOfEmbeddedEntities.get(key));
		}

		return this.intField == that.intField &&
				Objects.equals(this.embeddedEntityField, that.embeddedEntityField) &&
				Objects.equals(this.listOfEmbeddedEntities, that.listOfEmbeddedEntities)
				&& this.embeddedMapSimpleValues.equals(that.embeddedMapSimpleValues)
				&& this.embeddedEntityMapEmbeddedEntity.equals(that.embeddedEntityMapEmbeddedEntity)
				&& Objects.equals(mapEnumKeys, that.mapEnumKeys)
				&& this.customMap.equals(that.customMap);
	}

	@Override
	public int hashCode() {
		return Objects.hash(intField, embeddedEntityField, listOfEmbeddedEntities,
				embeddedMapSimpleValues, embeddedMapListOfValues, embeddedEntityMapEmbeddedEntity,
				embeddedEntityMapListOfEmbeddedEntities, nestedEmbeddedMaps, customMap, mapEnumKeys);
	}

	@Override
	public String toString() {
		return "TestItemWithEmbeddedEntity{" +
				"intField=" + intField +
				", embeddedEntityField=" + embeddedEntityField +
				", listOfEmbeddedEntities=" + listOfEmbeddedEntities +
				", embeddedMapSimpleValues=" + embeddedMapSimpleValues +
				", embeddedMapListOfValues=" + embeddedMapListOfValues +
				", embeddedEntityMapEmbeddedEntity=" + embeddedEntityMapEmbeddedEntity +
				", embeddedEntityMapListOfEmbeddedEntities=" + embeddedEntityMapListOfEmbeddedEntities +
				", nestedEmbeddedMaps=" + nestedEmbeddedMaps +
				", conf=" + customMap +
				", mapEnumKeys=" + mapEnumKeys +
				'}';
	}

	Map<String, Map<Long, Map<String, String>>> getNestedEmbeddedMaps() {
		return this.nestedEmbeddedMaps;
	}

	/**
	 * set the nested embedded maps.
	 * @param nestedEmbeddedMaps the maps to set.
	 */
	public void setNestedEmbeddedMaps(
			Map<String, Map<Long, Map<String, String>>> nestedEmbeddedMaps) {
		this.nestedEmbeddedMaps = nestedEmbeddedMaps;
	}

	/**
	 * an embedded entity class.
	 */
	@Entity
	public static class EmbeddedEntity {
		String stringField;

		public EmbeddedEntity(String stringField) {
			this.stringField = stringField;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			EmbeddedEntity that = (EmbeddedEntity) o;
			return Objects.equals(this.stringField, that.stringField);
		}

		@Override
		public int hashCode() {

			return Objects.hash(this.stringField);
		}
	}
}
