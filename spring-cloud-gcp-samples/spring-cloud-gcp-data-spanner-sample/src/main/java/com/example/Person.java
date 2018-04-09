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

package com.example;

import org.springframework.core.convert.converter.Converter;

/**
 * @author Balint Pato
 */
public class Person {

	public String firstName;

	public String lastName;

	@Override
	public String toString() {
		return "Person{" +
				"firstName='" + this.firstName + '\'' +
				", lastName='" + this.lastName + '\'' +
				'}';
	}

	public static class PersonWriteConverter implements Converter<Person, String> {

		@Override
		public String convert(Person person) {
			return person.firstName + " " + person.lastName;
		}
	}

	public static class PersonReadConverter implements Converter<String, Person> {

		@Override
		public Person convert(String s) {
			Person person = new Person();
			person.firstName = s.split(" ")[0];
			person.lastName = s.split(" ")[1];
			return person;
		}
	}
}
