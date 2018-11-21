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

import java.util.List;
import java.util.Objects;

import org.springframework.data.annotation.Id;

/**
 * @author Dmitry Solomakha
 */
public class TreeCollection {
	@Id
	private long id;

	private List<EmbeddableTreeNode> treeNodes;

	public TreeCollection(long id, List<EmbeddableTreeNode> treeNodes) {
		this.id = id;
		this.treeNodes = treeNodes;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TreeCollection that = (TreeCollection) o;
		return Objects.equals(this.treeNodes, that.treeNodes);
	}

	@Override
	public int hashCode() {

		return Objects.hash(this.treeNodes);
	}
}
