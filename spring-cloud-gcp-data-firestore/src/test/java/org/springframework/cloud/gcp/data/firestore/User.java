/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.firestore;

import java.util.Objects;

import com.google.cloud.firestore.annotation.DocumentId;

/**
 * Sample entity for integration tests.
 *
 * @author Daniel Zou
 */
//tag::class_definition[]
@Document(collectionName = "usersCollection")
public class User {
	@DocumentId
	private String name;

	private Integer age;
	//end::class_definition[]

	public User(String name, Integer age) {
		this.name = name;
		this.age = age;
	}

	public User() {
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAge() {
		return this.age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	@Override
	public String toString() {
		return "User{" +
				"name='" + this.name + '\'' +
				", age=" + this.age +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		User user = (User) o;
		return Objects.equals(getName(), user.getName()) &&
				Objects.equals(getAge(), user.getAge());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getAge());
	}
	//tag::class_definition[]
}
//end::class_definition[]
