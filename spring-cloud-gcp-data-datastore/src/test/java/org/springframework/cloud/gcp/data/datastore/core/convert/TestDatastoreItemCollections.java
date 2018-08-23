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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableSet;

/**
 * @author Dmitry Solomakha
 */
public class TestDatastoreItemCollections {
	private List<Integer> intList;

	private ImmutableSet<Double> doubleSet;

	private String[] stringArray;

	public TestDatastoreItemCollections(List<Integer> intList, ImmutableSet<Double> doubleSet, String[] stringArray) {
		this.intList = intList;
		this.doubleSet = doubleSet;
		this.stringArray = stringArray;
	}

	public List<Integer> getIntList() {
		return this.intList;
	}

	public ImmutableSet<Double> getDoubleSet() {
		return this.doubleSet;
	}

	public String[] getStringArray() {
		return this.stringArray;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TestDatastoreItemCollections that = (TestDatastoreItemCollections) o;
		return Objects.equals(getIntList(), that.getIntList()) &&
				Objects.equals(getDoubleSet(), that.getDoubleSet()) &&
				Arrays.equals(getStringArray(), that.getStringArray());
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(getIntList(), getDoubleSet());
		result = 31 * result + Arrays.hashCode(getStringArray());
		return result;
	}
}
