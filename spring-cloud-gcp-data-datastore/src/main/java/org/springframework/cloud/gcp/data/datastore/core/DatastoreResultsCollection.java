/*
 * Copyright 2019-2019 the original author or authors.
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

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.cloud.datastore.Cursor;

/**
 * @author Dmitry Solomakha
 */
public class DatastoreResultsCollection<T> implements Collection<T> {
	private final Collection<T> collection;

	private final Cursor cursor;

	DatastoreResultsCollection(Collection<T> collection, Cursor cursor) {
		this.collection = collection;
		this.cursor = cursor;
	}

	public Cursor getCursor() {
		return this.cursor;
	}

	@Override
	public int size() {
		return this.collection.size();
	}

	@Override
	public boolean isEmpty() {
		return this.collection.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return this.collection.contains(o);
	}

	@Override
	public Iterator<T> iterator() {
		return this.collection.iterator();
	}

	@Override
	public Object[] toArray() {
		return this.collection.toArray();
	}

	@Override
	public <T1> T1[] toArray(T1[] t1s) {
		return this.collection.toArray(t1s);
	}

	@Override
	public boolean add(T t) {
		return this.collection.add(t);
	}

	@Override
	public boolean remove(Object o) {
		return this.collection.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		return this.collection.containsAll(collection);
	}

	@Override
	public boolean addAll(Collection<? extends T> collection) {
		return this.collection.addAll(collection);
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		return this.collection.removeAll(collection);
	}

	@Override
	public boolean removeIf(Predicate<? super T> predicate) {
		return this.collection.removeIf(predicate);
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		return this.collection.retainAll(collection);
	}

	@Override
	public void clear() {
		this.collection.clear();
	}

	@Override
	public boolean equals(Object o) {
		return this.collection.equals(o);
	}

	@Override
	public int hashCode() {
		return this.collection.hashCode();
	}

	@Override
	public Spliterator<T> spliterator() {
		return this.collection.spliterator();
	}

	@Override
	public Stream<T> stream() {
		return this.collection.stream();
	}

	@Override
	public Stream<T> parallelStream() {
		return this.collection.parallelStream();
	}

	@Override
	public void forEach(Consumer<? super T> consumer) {
		this.collection.forEach(consumer);
	}
}
