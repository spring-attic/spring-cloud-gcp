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
 * @author Dmitry Solomakha
 */
public class DatastoreResultsIterable<T> implements Iterable<T> {
	private final Iterator<T> iterator;
	private final Cursor cursor;
	private Iterable<T> iterable;

	public DatastoreResultsIterable(Iterable<T> iterable, Cursor cursor) {
		this(iterable.iterator(), cursor);
		this.iterable = iterable;
	}

	public DatastoreResultsIterable(Iterator<T> iterator, Cursor cursor) {
		this.iterator = iterator;
		this.cursor = cursor;
	}

	@Override
	public Iterator<T> iterator() {
		return this.iterator;
	}

	public Cursor getCursor() {
		return this.cursor;
	}

	public Iterable<T> getIterable() {
		return this.iterable;
	}
}
