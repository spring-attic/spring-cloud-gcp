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

package org.springframework.cloud.gcp.data.spanner.core.mapping;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Supplier;

import org.springframework.util.Assert;

/**
 * An implementation of {@link java.util.List} that retrieves its contents from Spanner
 * when first used. This is not intended to be instantiated directly by the user.
 * @author Chengyuan Zhao
 */
public class SpannerLazyList<E> implements List<E> {

	private final Supplier<List<E>> getContentsOperation;

	private List<E> delegateList;

	/**
	 * Constructor
	 * @param getContentsOperation the operation to run to obtain the underlying list.
	 */
	public SpannerLazyList(Supplier<List<E>> getContentsOperation) {
		Assert.notNull(getContentsOperation,
				"A valid function to supply the contents is required.");
		this.getContentsOperation = getContentsOperation;
	}

	private void retrieveContentsIfNeeded() {
		if (this.delegateList == null) {
			this.delegateList = this.getContentsOperation.get();
		}
	}

	/**
	 * Returns true if the operation to obtain the underlying contents has already been
	 * run. False otherwise.
	 * @return
	 */
	public boolean hasBeenEvaluated() {
		return !(this.delegateList == null);
	}

	@Override
	public int size() {
		retrieveContentsIfNeeded();
		return this.delegateList.size();
	}

	@Override
	public boolean isEmpty() {
		retrieveContentsIfNeeded();
		return this.delegateList.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		retrieveContentsIfNeeded();
		return this.delegateList.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		retrieveContentsIfNeeded();
		return this.delegateList.iterator();
	}

	@Override
	public Object[] toArray() {
		retrieveContentsIfNeeded();
		return this.delegateList.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		retrieveContentsIfNeeded();
		return this.delegateList.toArray(a);
	}

	@Override
	public boolean add(E e) {
		retrieveContentsIfNeeded();
		return this.delegateList.add(e);
	}

	@Override
	public boolean remove(Object o) {
		retrieveContentsIfNeeded();
		return this.delegateList.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		retrieveContentsIfNeeded();
		return this.delegateList.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		retrieveContentsIfNeeded();
		return this.delegateList.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		retrieveContentsIfNeeded();
		return this.delegateList.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		retrieveContentsIfNeeded();
		return this.delegateList.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		retrieveContentsIfNeeded();
		return this.delegateList.retainAll(c);
	}

	@Override
	public void clear() {
		retrieveContentsIfNeeded();
		this.delegateList.clear();
	}

	@Override
	public E get(int index) {
		retrieveContentsIfNeeded();
		return this.delegateList.get(index);
	}

	@Override
	public E set(int index, E element) {
		retrieveContentsIfNeeded();
		return this.delegateList.set(index, element);
	}

	@Override
	public void add(int index, E element) {
		retrieveContentsIfNeeded();
		this.delegateList.add(index, element);
	}

	@Override
	public E remove(int index) {
		retrieveContentsIfNeeded();
		return this.delegateList.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		retrieveContentsIfNeeded();
		return this.delegateList.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		retrieveContentsIfNeeded();
		return this.delegateList.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		retrieveContentsIfNeeded();
		return this.delegateList.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		retrieveContentsIfNeeded();
		return this.delegateList.listIterator(index);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		retrieveContentsIfNeeded();
		return this.delegateList.subList(fromIndex, toIndex);
	}
}
