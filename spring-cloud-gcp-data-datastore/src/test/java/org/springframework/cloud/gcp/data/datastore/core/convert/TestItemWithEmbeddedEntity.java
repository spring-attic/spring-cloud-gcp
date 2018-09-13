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

package org.springframework.cloud.gcp.data.datastore.core.convert;

import java.util.List;
import java.util.Objects;

import org.springframework.cloud.gcp.data.datastore.core.mapping.Embedded;

/**
 * @author Dmitry Solomakha
 */
public class TestItemWithEmbeddedEntity {
	private int intField;

	@Embedded
	private EmbeddedEtity embeddedEntityField;

	@Embedded
	private List<EmbeddedEtity> listOfEmbeddedEntities;

	public TestItemWithEmbeddedEntity(int intField, EmbeddedEtity embeddedEntityField,
			List<EmbeddedEtity> listOfEmbeddedEntities) {
		this.intField = intField;
		this.embeddedEntityField = embeddedEntityField;
		this.listOfEmbeddedEntities = listOfEmbeddedEntities;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TestItemWithEmbeddedEntity item = (TestItemWithEmbeddedEntity) o;
		return this.intField == item.intField &&
				Objects.equals(this.embeddedEntityField, item.embeddedEntityField) &&
				Objects.equals(this.listOfEmbeddedEntities, item.listOfEmbeddedEntities);
	}

	@Override
	public int hashCode() {

		return Objects.hash(this.intField, this.embeddedEntityField, this.listOfEmbeddedEntities);
	}

	public static class EmbeddedEtity {
		String stringField;

		public EmbeddedEtity(String stringField) {
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
			EmbeddedEtity that = (EmbeddedEtity) o;
			return Objects.equals(this.stringField, that.stringField);
		}

		@Override
		public int hashCode() {

			return Objects.hash(this.stringField);
		}
	}
}
