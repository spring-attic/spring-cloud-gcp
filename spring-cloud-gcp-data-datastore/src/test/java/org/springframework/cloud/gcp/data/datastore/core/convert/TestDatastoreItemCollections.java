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

package org.springframework.cloud.gcp.data.datastore.core.convert;

import java.beans.beancontext.BeanContextSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.util.CollectionUtils;

/**
 * A test entity focused on collections.
 *
 * @author Dmitry Solomakha
 */
public class TestDatastoreItemCollections {
	private List<Integer> intList;

	private ComparableBeanContextSupport beanContext;

	private String[] stringArray;

	private boolean[] boolArray;

	private byte[][] bytes;

	private List<byte[]> listByteArray;

	public TestDatastoreItemCollections(List<Integer> intList, ComparableBeanContextSupport beanContext, String[] stringArray,
			boolean[] boolArray, byte[][] bytes, List<byte[]> listByteArray) {
		this.intList = intList;
		this.beanContext = beanContext;
		this.stringArray = stringArray;
		this.boolArray = boolArray;
		this.bytes = bytes;
		this.listByteArray = listByteArray;
	}

	public List<Integer> getIntList() {
		return this.intList;
	}

	public BeanContextSupport getBeanContext() {
		return this.beanContext;
	}

	public String[] getStringArray() {
		return this.stringArray;
	}

	public boolean[] getBoolArray() {
		return this.boolArray;
	}

	public byte[][] getBytes() {
		return this.bytes;
	}

	public List<byte[]> getListByteArray() {
		return this.listByteArray;
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
				Objects.equals(getBeanContext(), that.getBeanContext()) &&
				Arrays.equals(getStringArray(), that.getStringArray()) &&
				Arrays.equals(getBoolArray(), that.getBoolArray()) &&
				equal(getBytes(), that.getBytes()) &&
				equal(getListByteArray(), that.getListByteArray());
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(getIntList(), getBeanContext(), getListByteArray());
		result = 31 * result + Arrays.hashCode(getStringArray());
		result = 31 * result + Arrays.hashCode(getBoolArray());
		result = 31 * result + Arrays.hashCode(getBytes());
		return result;
	}

	private List<List<Byte>> arraysToLists(Object[] arrays) {
		List<List<Byte>> result = new ArrayList<>();
		for (Object e : arrays) {
			result.add((List<Byte>) CollectionUtils.arrayToList(e));
		}
		return result;
	}

	private boolean equal(Object a, Object b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}

		Object valA = a;
		Object valB = b;
		if (a instanceof List) {
			valA = ((List<byte[]>) a).toArray();
			valB = ((List<byte[]>) b).toArray();
		}
		return arraysToLists((Object[]) valA).equals(arraysToLists((Object[]) valB));
	}

	/**
	 * BeanContextSupport does not provide an equals() implementation.
	 * This subclass overrides {@code equals/hashCode} and keeps a simple list of values
	 * to enable test verification.
	 */
	static class ComparableBeanContextSupport extends BeanContextSupport {
		private Set<Object> values = new HashSet<>();

		@Override
		public boolean add(Object o) {
			this.values.add(o);
			return super.add(o);
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || !(o instanceof ComparableBeanContextSupport)) {
				return false;
			}
			ComparableBeanContextSupport cbcs = (ComparableBeanContextSupport) o;
			return cbcs.values.equals(this.values);
		}

		@Override
		public int hashCode() {
			// Abides by hashCode contract, while being perfectly useless.
			return 1;
		}
	}
}
