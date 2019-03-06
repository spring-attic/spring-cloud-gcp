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
import java.util.Set;

import com.google.cloud.datastore.Key;

/**
 * An event published immediately after a read operation has finished.
 *
 * @author Chengyuan Zhao
 */
public class AfterFindByKeyEvent extends ReadEvent {

	private final Set<Key> targetKeys;

	/**
	 * Constructor.
	 *
	 * @param results A list of results from the read operation where each item was mapped
	 *     from a Cloud Datastore entity.
	 * @param keys The Keys that were attempted to be read.
	 */
	public AfterFindByKeyEvent(Iterable results, Set<Key> keys) {
		super(results);
		this.targetKeys = keys;
	}

	/**
	 * Get the list of target read keys.
	 * @return the list of Keys that were attempted to be read.
	 */
	public Set<Key> getTargetKeys() {
		return this.targetKeys;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AfterFindByKeyEvent that = (AfterFindByKeyEvent) o;
		return Objects.equals(getResults(), that.getResults())
				&& Objects.equals(getTargetKeys(), that.getTargetKeys());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getResults(), getTargetKeys());
	}
}
