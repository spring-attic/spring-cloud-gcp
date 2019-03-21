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

import com.google.cloud.datastore.Blob;

import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.data.annotation.Id;

/**
 * A test entity for Datastore integration tests.
 *
 * @author Chengyuan Zhao
 */
@Entity(name = "test_entities_#{\"ci\"}")
public class TestEntity {

	@Id
	private Long id;

	private String color;

	private Long size;

	private Shape shape;

	private Blob blobField;

	public TestEntity(Long id, String color, Long size, Shape shape, Blob blobField) {
		this.id = id;
		this.color = color;
		this.size = size;
		this.shape = shape;
		this.blobField = blobField;
	}

	public Shape getShape() {
		return this.shape;
	}

	public void setShape(Shape shape) {
		this.shape = shape;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Blob getBlobField() {
		return this.blobField;
	}

	public void setBlobField(Blob blobField) {
		this.blobField = blobField;
	}

	public String getColor() {
		return this.color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public Long getSize() {
		return this.size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	/**
	 * An enum that tests conversion and storage.
	 */
	enum Shape {

		CIRCLE, SQUARE;

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TestEntity that = (TestEntity) o;
		return Objects.equals(getId(), that.getId())
				&& Objects.equals(getColor(), that.getColor())
				&& Objects.equals(getSize(), that.getSize())
				&& getShape() == that.getShape()
				&& Objects.equals(getBlobField(), that.getBlobField());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getColor(), getSize(), getShape(), getBlobField());
	}

	@Override
	public String toString() {
		return "TestEntity{" + "id=" + this.id + ", color='" + this.color + '\''
				+ ", size=" + this.size + ", shape=" + this.shape + ", blobField="
				+ this.blobField + '}';
	}

}
