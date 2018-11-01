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

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.HashSet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreCustomConversions;
import org.springframework.context.annotation.Bean;

/**
 * @author Chengyuan Zhao
 */
@SpringBootApplication
public class DatastoreRepositoryExample {

	@Autowired
	private SingerRepository singerRepository;

	@Autowired
	private TransactionalRepositoryService transactionalRepositoryService;

	public static void main(String[] args) {
		SpringApplication.run(DatastoreRepositoryExample.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner() {
		return args -> {
			System.out.println("Remove all records from 'singers' kind");
			this.singerRepository.deleteAll();

			this.singerRepository
					.save(new Singer("singer1", "John", "Doe", ImmutableSet.of()));

			Singer maryJane = new Singer("singer2", "Mary", "Jane",
					ImmutableSet.of(new Album("a", LocalDate.of(2012, Month.JANUARY, 20)),
							new Album("b", LocalDate.of(2018, Month.FEBRUARY, 12))));
			Singer scottSmith = new Singer("singer3", "Scott", "Smith", ImmutableSet
					.of(new Album("c", LocalDate.of(2000, Month.AUGUST, 31))));

			this.singerRepository.saveAll(ImmutableList.of(maryJane, scottSmith));

			createRelationshipsInTransaction(maryJane, scottSmith);

			// The following line uses count(), which is a global-query in Datastore. This
			// has only eventual consistency.
			Thread.sleep(3000);

			retrieveAndPrintSingers();

			System.out.println("This concludes the sample.");

		};
	}

	private void retrieveAndPrintSingers() {
		System.out.println("The kind for singers has been cleared and "
				+ this.singerRepository.count() + " new singers have been inserted:");

		Iterable<Singer> allSingers = this.singerRepository.findAll();
		allSingers.forEach(System.out::println);

		System.out.println("You can also retrieve by keys for strong consistency: ");

		// Retrieving by keys or querying with a restriction to a single entity group
		// / family is strongly consistent.
		this.singerRepository
				.findAllById(ImmutableList.of("singer1", "singer2", "singer3"))
				.forEach(x -> System.out.println("retrieved singer: " + x));
	}

	private void createRelationshipsInTransaction(Singer maryJane, Singer scottSmith) {
		Band band1 = new Band("band1");
		Band band2 = new Band("band2");
		Band band3 = new Band("band3");

		// Creates the related Band and Instrument entities and links them to a Singer
		// in a single atomic transaction
		this.transactionalRepositoryService.createAndSaveSingerRelationshipsInTransaction(
				maryJane, band1, Arrays.asList(band1, band2), new HashSet<>(Arrays
						.asList(new Instrument("recorder"), new Instrument("cow bell"))));

		// You can also execute code within a transaction directly using the
		// SingerRepository.
		// The following call also performs the creation and saving of relationships
		// in a single transaction.
		this.singerRepository.performTransaction(transactionRepository -> {
			scottSmith.setFirstBand(band3);
			scottSmith.setBands(Arrays.asList(band3, band2));
			scottSmith.setPersonalInstruments(new HashSet<>(Arrays
					.asList(new Instrument("triangle"), new Instrument("marimba"))));
			this.singerRepository.save(scottSmith);
			return null;
		});
	}

	@Bean
	public DatastoreCustomConversions datastoreCustomConversions() {
		return new DatastoreCustomConversions(Arrays.asList(
				// Converter to read ImmutableSet (List to ImmutableSet)
				// Note that you don't need a ImmutableSet to List converter
				ConvertersExample.LIST_IMMUTABLE_SET_CONVERTER,

				// Converters to read and write custom Singer.Album type
				ConvertersExample.ALBUM_STRING_CONVERTER,
				ConvertersExample.STRING_ALBUM_CONVERTER));
	}
}
