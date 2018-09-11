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

import com.google.cloud.datastore.Blob;

import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.data.annotation.Id;

/**
 * @author Chengyuan Zhao
 */
@Entity(name = "test_entities_ci")
public class TestEntity {

	@Id
	private String id;

	private String color;

	private String shape;

	public TestEntity(String id, String color, String shape, Blob blobField) {
		this.id = id;
		this.color = color;
		this.shape = shape;
		this.blobField = blobField;
	}

	private Blob blobField;

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
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

	public String getShape() {
		return this.shape;
	}

	public void setShape(String shape) {
		this.shape = shape;
	}
}
