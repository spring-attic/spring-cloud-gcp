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

package org.springframework.cloud.gcp.data.datastore.it;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.springframework.cloud.gcp.data.datastore.core.mapping.Reference;
import org.springframework.data.annotation.Id;

/**
 * @author Dmitry Solomakha
 */
class ReferenceEntry {
	@Id
	Long id;

	String name;

	@Reference
	ReferenceEntry sibling;

	@Reference
	List<ReferenceEntry> childeren;

	ReferenceEntry(String name, ReferenceEntry sibling, List<ReferenceEntry> childeren) {
		this.name = name;
		this.sibling = sibling;
		this.childeren = childeren;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ReferenceEntry that = (ReferenceEntry) o;
		return Objects.equals(this.id, that.id) &&
				Objects.equals(this.name, that.name) &&
				Objects.equals(this.sibling, that.sibling) &&
				new HashSet<>(this.childeren != null ? this.childeren : Collections.emptyList())
						.equals(new HashSet<>(that.childeren != null ? that.childeren : Collections.emptyList()));
	}

	@Override
	public int hashCode() {

		return Objects.hash(this.id, this.name, this.sibling, this.childeren);
	}

	@Override
	public String toString() {
		return "ReferenceEntry{" +
				"id=" + this.id +
				", name='" + this.name + '\'' +
				", sibling=" + this.sibling +
				", childeren=" + this.childeren +
				'}';
	}
}
