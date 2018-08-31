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


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Dmitry Solomakha
 */
class TestItemUnsupportedFields {
	private String stringField;

	private NewType unsupportedField;

	public String getStringField() {
		return this.stringField;
	}

	public void setStringField(String stringField) {
		this.stringField = stringField;
	}

	public NewType getUnsupportedField() {
		return this.unsupportedField;
	}

	public void setUnsupportedField(NewType unsupportedField) {
		this.unsupportedField = unsupportedField;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TestItemUnsupportedFields that = (TestItemUnsupportedFields) o;
		return Objects.equals(getStringField(), that.getStringField()) &&
				Objects.equals(getUnsupportedField(), that.getUnsupportedField());
	}

	@Override
	public int hashCode() {

		return Objects.hash(getStringField(), getUnsupportedField());
	}

	static class NewType {
		boolean val;

		NewType(boolean val) {
			this.val = val;
		}

		public boolean isVal() {
			return this.val;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			NewType that = (NewType) o;
			return isVal() == that.isVal();
		}

		@Override
		public int hashCode() {

			return Objects.hash(isVal());
		}
	}

	static class CollectionOfUnsupportedTypes {
		List<NewType> unsupportedElts = new ArrayList<>();

		public List<NewType> getUnsupportedElts() {
			return this.unsupportedElts;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			CollectionOfUnsupportedTypes that = (CollectionOfUnsupportedTypes) o;
			return Objects.equals(getUnsupportedElts(), that.getUnsupportedElts());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getUnsupportedElts());
		}
	}
}
