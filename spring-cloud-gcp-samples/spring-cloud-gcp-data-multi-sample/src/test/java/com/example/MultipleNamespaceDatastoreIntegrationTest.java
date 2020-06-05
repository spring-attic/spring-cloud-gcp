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

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.datastore.DatastoreNamespaceProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Integration test for multiple-namespace support.
 *
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application-test.properties")
@EnableAutoConfiguration
public class MultipleNamespaceDatastoreIntegrationTest {

	@Autowired
	PersonRepository datastorePersonRepository;

	@BeforeClass
	public static void checkToRun() {
		assumeThat("Datastore integration tests are disabled. "
				+ "Please use '-Dit.datastore=true' to enable them.",
				System.getProperty("it.datastore"), is("true"));
	}

	@Test
	public void testMultipleNamespaces() {

		this.datastorePersonRepository.deleteAll();
		Config.flipNamespace();
		this.datastorePersonRepository.deleteAll();

		Awaitility.await().atMost(15, TimeUnit.SECONDS)
				.until(() -> this.datastorePersonRepository.count() == 0);
		Config.flipNamespace();
		Awaitility.await().atMost(15, TimeUnit.SECONDS)
				.until(() -> this.datastorePersonRepository.count() == 0);

		this.datastorePersonRepository.save(new Person(1L, "a"));
		Config.flipNamespace();
		this.datastorePersonRepository.save(new Person(2L, "a"));
		Config.flipNamespace();
		this.datastorePersonRepository.save(new Person(3L, "a"));

		Awaitility.await().atMost(15, TimeUnit.SECONDS)
				.until(() -> this.datastorePersonRepository.count() == 2);
		Config.flipNamespace();
		Awaitility.await().atMost(15, TimeUnit.SECONDS)
				.until(() -> this.datastorePersonRepository.count() == 1);
	}

	/**
	 * Configuring custom multiple namespaces.
	 *
	 * @author Chengyuan Zhao
	 */
	@Configuration
	static class Config {

		static boolean namespaceFlipper;

		/**
		 * Flips the namespace that all Datastore repositories and templates use.
		 */
		static void flipNamespace() {
			namespaceFlipper = !namespaceFlipper;
		}

		@Bean
		public DatastoreNamespaceProvider namespaceProvider() {
			return () -> namespaceFlipper ? "n1" : "n2";
		}
	}
}
