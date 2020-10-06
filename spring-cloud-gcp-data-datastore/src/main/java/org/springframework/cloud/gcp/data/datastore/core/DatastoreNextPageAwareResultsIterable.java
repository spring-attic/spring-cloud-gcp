/*
 * Copyright 2019-2019 the original author or authors.
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

/**
 * An iterable that contains a cursor for the next page and can be used to determine
 * if the next page exists.
 *
 * @author Dmitry Solomakha
 */
public class DatastoreNextPageAwareResultsIterable<T> implements Iterable<T> {
	final DatastoreResultsIterable<T> datastoreResultsIterable;

	final boolean hasNextPage;

	public DatastoreNextPageAwareResultsIterable(DatastoreResultsIterable<T> datastoreResultsIterable, boolean hasNextPage) {
		this.datastoreResultsIterable = datastoreResultsIterable;
		this.hasNextPage = hasNextPage;
	}

	@Override
	public Iterator<T> iterator() {
		return datastoreResultsIterable.iterator();
	}

	public Cursor getCursor() {
		return datastoreResultsIterable.getCursor();
	}

	public boolean hasNextPage() {
		return hasNextPage;
	}
}
