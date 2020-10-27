/*
 * Copyright 2019-2019 the original author or authors.
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

package com.google.cloud.spring.autoconfigure.firestore.it;

import java.util.List;
import java.util.Objects;

/**
 * @author Dmitry Solomakha
 */
public class User {
	private String name;
	private List<Phone> phones;

	public User() {
	}

	User(String name, List<Phone> phones) {
		this.name = name;
		this.phones = phones;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Phone> getPhones() {
		return this.phones;
	}

	public void setPhones(List<Phone> phones) {
		this.phones = phones;
	}

	@Override
	public String toString() {
		return "User{" + "name='" + this.name + '\'' + ", phones=" + this.phones + '}';
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
				Objects.equals(getPhones(), user.getPhones());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getPhones());
	}
}

class Phone {
	private int number;
	private PhoneType type;

	Phone() {
	}

	Phone(int number, PhoneType type) {
		this.number = number;
		this.type = type;
	}

	public int getNumber() {
		return this.number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public PhoneType getType() {
		return this.type;
	}

	public void setType(PhoneType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "Phone{" + "number=" + this.number + ", type=" + this.type + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Phone phone = (Phone) o;
		return getNumber() == phone.getNumber() &&
				getType() == phone.getType();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getNumber(), getType());
	}
}

enum PhoneType {
	WORK, CELL;
}

