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

import java.util.List;

import com.google.common.collect.Lists;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

/**
 * This class contains the main method and performs common operations with books.
 *
 * @author Dmitry Solomakha
 */
@ShellComponent
@SpringBootApplication
public class DatastoreBookshelfExample {
	@Autowired
	BookRepository bookRepository;

	public static void main(String[] args) {
		SpringApplication.run(DatastoreBookshelfExample.class, args);
	}

	@ShellMethod("Saves a book to Cloud Datastore: save-book <author> <title> <year>")
	public String saveBook(String title, String author, int year) {
		Book savedBook = this.bookRepository.save(new Book(title, author, year));
		return savedBook.toString();
	}

	@ShellMethod("Loads all books")
	public String findAllBooks() {
		Iterable<Book> books = this.bookRepository.findAll();
		return Lists.newArrayList(books).toString();
	}

	@ShellMethod("Loads books by author: find-by-author <author>")
	public String findByAuthor(String author) {
		List<Book> books = this.bookRepository.findByAuthor(author);
		return books.toString();
	}

	@ShellMethod("Loads books released after a specified year: find-by-year-greater-than <year>")
	public String findByYearGreaterThan(int year) {
		List<Book> books = this.bookRepository.findByYearGreaterThan(year);
		return books.toString();
	}

	@ShellMethod("Loads books by author and year: find-by-author-year <author> <year>")
	public String findByAuthorYear(String author, int year) {
		List<Book> books = this.bookRepository.findByAuthorAndYear(author, year);
		return books.toString();
	}

	@ShellMethod("Removes all books")
	public void removeAllBooks() {
		this.bookRepository.deleteAll();
	}
}
