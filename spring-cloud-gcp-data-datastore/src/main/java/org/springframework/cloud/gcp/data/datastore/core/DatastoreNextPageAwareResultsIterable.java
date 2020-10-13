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

package org.springframework.cloud.gcp.data.datastore.core;

import java.util.Iterator;

import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.StructuredQuery;

import org.springframework.data.util.Lazy;

public class DatastoreNextPageAwareResultsIterable<T> implements DatastoreResultsIterable<T> {
	private DatastoreResultsIterable datastoreResultsIterable;

	private Cursor cursor;

	private StructuredQuery structuredQuery;


	private Lazy<Boolean> hasNextPage;

	public DatastoreNextPageAwareResultsIterable(
			DatastoreResultsIterable datastoreResultsIterable,
			Cursor cursor,
			StructuredQuery structuredQuery,
			DatastoreTemplate datastoreTemplate) {
		this.datastoreResultsIterable = datastoreResultsIterable;
		this.cursor = cursor;
		this.structuredQuery = structuredQuery;
		this.hasNextPage = Lazy.of(datastoreTemplate.nextPageExists(this.structuredQuery, this.cursor));
	}

	public boolean hasNextPage() {
		return hasNextPage.get();
	}

	@Override
	public Iterator<T> iterator() {
		return datastoreResultsIterable.iterator();
	}

	@Override
	public Cursor getCursor() {
		return datastoreResultsIterable.getCursor();
	}

	@Override
	public Iterable<T> getIterable() {
		return datastoreResultsIterable.getIterable();
	}
}
