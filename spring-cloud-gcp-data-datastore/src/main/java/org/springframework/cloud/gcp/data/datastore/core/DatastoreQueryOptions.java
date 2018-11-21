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

package org.springframework.cloud.gcp.data.datastore.core;

import java.util.Objects;

import org.springframework.data.domain.Sort;

/**
 * Encapsulates Cloud Memorystore query options.
 *
 * @author Dmitry Solomakha
 */
public class DatastoreQueryOptions {

	private Integer limit;

	private Integer offset;

	private Sort sort;

	public DatastoreQueryOptions(Integer limit, Integer offset, Sort sort) {
		this.limit = limit;
		this.offset = offset;
		this.sort = sort;
	}

	public Integer getLimit() {
		return this.limit;
	}

	public Integer getOffset() {
		return this.offset;
	}

	public Sort getSort() {
		return this.sort;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DatastoreQueryOptions that = (DatastoreQueryOptions) o;
		return Objects.equals(getLimit(), that.getLimit()) &&
				Objects.equals(getOffset(), that.getOffset()) &&
				Objects.equals(getSort(), that.getSort());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getLimit(), getOffset(), getSort());
	}
}
