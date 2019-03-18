/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.datastore.core;

import java.util.Objects;

import com.google.cloud.datastore.Cursor;

import org.springframework.data.domain.Sort;

/**
 * Encapsulates Cloud Datastore query options.
 *
 * @author Dmitry Solomakha
 */
public final class DatastoreQueryOptions {

	private Integer limit;

	private Integer offset;

	private Sort sort;

	private Cursor cursor;

	private DatastoreQueryOptions(Integer limit, Integer offset, Sort sort, Cursor cursor) {
		this.limit = limit;
		this.offset = offset;
		this.sort = sort;
		this.cursor = cursor;
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

	public Cursor getCursor() {
		return this.cursor;
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
				Objects.equals(getSort(), that.getSort()) &&
				Objects.equals(getCursor(), that.getCursor());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getLimit(), getOffset(), getSort(), getCursor());
	}

	public static class Builder {
		private Integer limit;
		private Integer offset;
		private Sort sort;
		private Cursor cursor;

		public Builder setLimit(Integer limit) {
			this.limit = limit;
			return this;
		}

		public Builder setOffset(Integer offset) {
			this.offset = offset;
			return this;
		}

		public Builder setSort(Sort sort) {
			this.sort = sort;
			return this;
		}

		public Builder setCursor(Cursor cursor) {
			this.cursor = cursor;
			return this;
		}

		public DatastoreQueryOptions build() {
			return new DatastoreQueryOptions(this.limit, this.offset, this.sort, this.cursor);
		}
	}

}
