/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.data.annotation.Id;

/**
 * This class represents a single book stored in Datastore.
 *
 * @author Dmitry Solomakha
 */
@Entity(name = "books")
public class Book {
	@Id
	Long id;

	String title;

	String author;

	int year;

	public Book(String title, String author, int year) {
		this.title = title;
		this.author = author;
		this.year = year;
	}

	public long getId() {
		return this.id;
	}

	@Override
	public String toString() {
		return "Book{" +
				"id=" + this.id +
				", title='" + this.title + '\'' +
				", author='" + this.author + '\'' +
				", year=" + this.year +
				'}';
	}
}
