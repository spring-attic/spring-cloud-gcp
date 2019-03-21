/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.data.datastore.it;

import java.util.Objects;

import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.data.annotation.Id;

/**
 * A test class that uses embedded relationships to represent a tree.
 *
 * @author Dmitry Solomakha
 */
@Entity
public class EmbeddableTreeNode {
	@Id
	long value;

	EmbeddableTreeNode left;

	EmbeddableTreeNode right;

	public EmbeddableTreeNode(long value, EmbeddableTreeNode left, EmbeddableTreeNode right) {
		this.value = value;
		this.left = left;
		this.right = right;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		EmbeddableTreeNode treeNode = (EmbeddableTreeNode) o;
		return this.value == treeNode.value &&
				Objects.equals(this.left, treeNode.left) &&
				Objects.equals(this.right, treeNode.right);
	}

	@Override
	public int hashCode() {

		return Objects.hash(this.value, this.left, this.right);
	}

	@Override
	public String toString() {
		return "EmbeddableTreeNode{" +
				"value=" + this.value +
				", left=" + this.left +
				", right=" + this.right +
				'}';
	}
}
