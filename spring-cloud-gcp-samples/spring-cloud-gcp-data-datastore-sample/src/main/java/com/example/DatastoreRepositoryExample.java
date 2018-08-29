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

import com.google.common.collect.ImmutableList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author Chengyuan Zhao
 */
@SpringBootApplication
public class DatastoreRepositoryExample {

	@Autowired
	private SingerRepository singerRepository;

	public static void main(String[] args) {
		SpringApplication.run(DatastoreRepositoryExample.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner() {
		return args -> {

			this.singerRepository.deleteAll();

			this.singerRepository.save(new Singer("singer1", "John", "Doe"));
			this.singerRepository
					.saveAll(ImmutableList.of(new Singer("singer2", "Mary", "Jane"),
							new Singer("singer3", "Scott", "Smith")));

			// The following line uses count(), which is a global-query in Datastore. This
			// has only eventual consistency.
			Thread.sleep(3000);

			System.out.println("The table for singers has been cleared and "
					+ this.singerRepository.count() + " new singers have been inserted:");

			Iterable<Singer> allSingers = this.singerRepository.findAll();
			allSingers.forEach(System.out::println);

			System.out.println("You can also retrieve by keys for strong consistency: ");

			// Retrieving by keys or querying with a restriction to a single entity group
			// / family is strongly consistent.
			this.singerRepository
					.findAllById(ImmutableList.of("singer1", "singer2", "singer3"))
					.forEach(System.out::println);

			System.out.println("This concludes the sample.");

		};
	}
}
