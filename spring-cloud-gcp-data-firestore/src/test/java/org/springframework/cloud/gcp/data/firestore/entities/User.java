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

package org.springframework.cloud.gcp.data.firestore.entities;

import java.util.List;
import java.util.Objects;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;

import org.springframework.cloud.gcp.data.firestore.Document;

/**
 * Sample entity for integration tests.
 *
 * @author Daniel Zou
 * @author Dmitry Solomakha
 */
//tag::class_definition[]
//tag::embedded_class_collections[]
@Document(collectionName = "usersCollection")
public class User {
	/**
	 * Used to test @PropertyName annotation on a field.
	 */
	@PropertyName("drink")
	public String favoriteDrink;

	@DocumentId
	private String name;

	private Integer age;

	//end::class_definition[]
	private List<String> pets;

	private List<Address> addresses;

	private Address homeAddress;

	//end::embedded_class_collections[]

	public User(String name, Integer age) {
		this.name = name;
		this.age = age;
	}

	public User(String name, Integer age, List<String> pets) {
		this.name = name;
		this.age = age;
		this.pets = pets;
	}

	public User(String name, Integer age, List<String> pets, List<Address> addresses, Address homeAddress) {
		this.name = name;
		this.age = age;
		this.pets = pets;
		this.addresses = addresses;
		this.homeAddress = homeAddress;
	}

	//tag::class_definition[]
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
	//end::class_definition[]

	//tag::embedded_class_collections[]
	public List<String> getPets() {
		return this.pets;
	}

	public void setPets(List<String> pets) {
		this.pets = pets;
	}

	public List<Address> getAddresses() {
		return this.addresses;
	}

	public void setAddresses(List<Address> addresses) {
		this.addresses = addresses;
	}

	@PropertyName("address")
	public Address getHomeAddress() {
		return this.homeAddress;
	}

	@PropertyName("address")
	public void setHomeAddress(Address homeAddress) {
		this.homeAddress = homeAddress;
	}
	//end::embedded_class_collections[]
	@Override
	public String toString() {
		return "User{" +
				"name='" + name + '\'' +
				", age=" + age +
				", pets=" + pets +
				", addresses=" + addresses +
				", homeAddress=" + homeAddress +
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
				Objects.equals(getAge(), user.getAge()) &&
				Objects.equals(getPets(), user.getPets()) &&
				Objects.equals(getAddresses(), user.getAddresses()) &&
				Objects.equals(getHomeAddress(), user.getHomeAddress());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getAge(), getPets(), getAddresses(), getHomeAddress());
	}

	//tag::embedded_class_collections[]

	public static class Address {
		String streetAddress;
		String country;

		public Address() {
		}
		//end::embedded_class_collections[]
		public Address(String streetAddress, String country) {
			this.streetAddress = streetAddress;
			this.country = country;
		}

		public String getStreetAddress() {
			return this.streetAddress;
		}

		public String getCountry() {
			return this.country;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Address address = (Address) o;
			return Objects.equals(getStreetAddress(), address.getStreetAddress()) &&
					Objects.equals(getCountry(), address.getCountry());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getStreetAddress(), getCountry());
		}
		//tag::embedded_class_collections[]
	}
	//tag::class_definition[]
}
//end::embedded_class_collections[]
//end::class_definition[]
