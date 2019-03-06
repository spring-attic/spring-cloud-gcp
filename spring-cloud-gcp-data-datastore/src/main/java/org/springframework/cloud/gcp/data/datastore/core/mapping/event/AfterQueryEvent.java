/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.datastore.core.mapping.event;

import java.util.Objects;

import com.google.cloud.datastore.Query;

/**
 * An event published immediately after a read-by-query request.
 *
 * @author Chengyuan Zhao
 */
public class AfterQueryEvent extends ReadEvent {

	private final Query query;

	/**
	 * Constructor.
	 *
	 * @param results A list of results from the read operation where each item was mapped
	 *     from a Cloud Datastore entity.
	 * @param query The query run on Cloud Datastore.
	 */
	public AfterQueryEvent(Iterable results, Query query) {
		super(results);
		this.query = query;
	}

	/**
	 * Get the query that was run.
	 * @return the query that was run.
	 */
	public Query getQuery() {
		return this.query;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AfterQueryEvent that = (AfterQueryEvent) o;
		return Objects.equals(getResults(), that.getResults())
				&& Objects.equals(getQuery(), that.getQuery());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getResults(), getQuery());
	}
}
