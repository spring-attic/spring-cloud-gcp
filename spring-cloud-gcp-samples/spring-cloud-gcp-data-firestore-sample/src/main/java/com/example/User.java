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

package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.cloud.firestore.annotation.DocumentId;

import org.springframework.cloud.gcp.data.firestore.Document;

/**
 * Example POJO to demonstrate Spring Cloud GCP Spring Data Firestore operations.
 *
 * @author Daniel Zou
 */
@Document(collectionName = "users")
public class User {

	@DocumentId
	String name;

	int age;

	List<Pet> pets;

	User() {
		pets = new ArrayList<>();
	}

	public User(String name, int age, List<Pet> pets) {
		this.name = name;
		this.age = age;
		this.pets = pets;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public List<Pet> getPets() {
		return pets;
	}

	public void setPets(List<Pet> pets) {
		this.pets = pets;
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
		return age == user.age &&
				Objects.equals(name, user.name) &&
				Objects.equals(pets, user.pets);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, age, pets);
	}

	@Override
	public String toString() {
		return "User{" +
				"name='" + name + '\'' +
				", age=" + age +
				", pets=" + pets +
				'}';
	}
}
