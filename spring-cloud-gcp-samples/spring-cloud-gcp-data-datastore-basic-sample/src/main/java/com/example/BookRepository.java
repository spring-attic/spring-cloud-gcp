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

import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository;

/**
 * This interface contains custom-defined query methods of which implementations are generated for you.
 *
 * @author Dmitry Solomakha
 */
public interface BookRepository extends DatastoreRepository<Book, Long> {

	List<Book> findByAuthor(String author);

	List<Book> findByYearGreaterThan(int year);

	List<Book> findByAuthorAndYear(String author, int year);

}
