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

package org.springframework.cloud.gcp.data.datastore.it;

import java.util.Objects;

import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.LazyReference;
import org.springframework.data.annotation.Id;

@Entity
public class LazyEntity {
	@Id
	Long id;

	@LazyReference
	LazyEntity lazyChild;

	public LazyEntity() {
	}

	public LazyEntity(LazyEntity child) {
		this.lazyChild = child;
	}

	Long getId() {
		return this.id;
	}

	LazyEntity getLazyChild() {
		return this.lazyChild;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		LazyEntity that = (LazyEntity) o;
		return Objects.equals(getId(), that.getId()) &&
				Objects.equals(getLazyChild(), that.getLazyChild());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getLazyChild());
	}

	@Override
	public String toString() {
		return "LazyEntity{" +
				"id=" + this.id +
				", lazyChild=" + this.lazyChild +
				'}';
	}
}
