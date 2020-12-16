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

package com.google.cloud.spring.data.datastore.entities;

import com.google.cloud.datastore.Key;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;

public class Product {
	@Id
	private Key id;

	@Reference
	private Store store;

	public Product(Store store) {
		this.store = store;
	}

	@Override
	public String toString() {
		return "Product{" +
				"id=" + id +
				", store=" + store +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Product)) {
			return false;
		}

		Product product = (Product) o;

		if (id != null ? !id.equals(product.id) : product.id != null) {
			return false;
		}
		return store != null ? store.equals(product.store) : product.store == null;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (store != null ? store.hashCode() : 0);
		return result;
	}
}
