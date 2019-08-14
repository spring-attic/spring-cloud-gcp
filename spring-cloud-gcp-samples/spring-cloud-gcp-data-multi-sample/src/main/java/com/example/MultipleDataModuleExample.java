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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * An example demonstrating the use of both Spring Data Cloud Spanner and Datastore.
 *
 * @author Chengyuan Zhao
 */
@SpringBootApplication
public class MultipleDataModuleExample {

	// A Spring Data Datastore repository
	@Autowired
	PersonRepository personRepository;

	// A Spring Data Cloud Spanner repository
	@Autowired
	TraderRepository traderRepository;

	public static void main(String[] args) {
		SpringApplication.run(MultipleDataModuleExample.class, args);
	}

	@Bean
	ApplicationRunner applicationRunner() {
		return (args) -> {

			System.out.println("Deleting all entities.");

			this.personRepository.deleteAll();
			this.traderRepository.deleteAll();

			System.out.println("The number of Person entities is now: " + this.personRepository.count());
			System.out.println("The number of Trader entities is now: " + this.traderRepository.count());

			System.out.println("Saving one entity with each repository.");

			this.traderRepository.save(new Trader("id1", "trader", "one"));
			this.personRepository.save(new Person(1L, "person1"));

			System.out.println("The number of Person entities is now: " + this.personRepository.count());
			System.out.println("The number of Trader entities is now: " + this.traderRepository.count());
		};
	}
}
