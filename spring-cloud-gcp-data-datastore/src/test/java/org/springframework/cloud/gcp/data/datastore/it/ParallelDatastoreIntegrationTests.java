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

package org.springframework.cloud.gcp.data.datastore.it;

import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Tests performing many operations at the same time using single instances of the
 * repository.
 *
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { DatastoreIntegrationTestConfiguration.class })
public class ParallelDatastoreIntegrationTests extends AbstractDatastoreIntegrationTests {

	private static final int PARALLEL_OPERATIONS = 10;

	@Autowired
	TestEntityRepository testEntityRepository;

	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"Datastore integration tests are disabled. Please use '-Dit.datastore=true' "
						+ "to enable them. ",
				System.getProperty("it.datastore"), is("true"));
	}

	@After
	public void deleteAll() {
		this.testEntityRepository.deleteAll();
	}

	@Test
	public void testParallelOperations() {
		performOperation(x -> this.testEntityRepository.save(new TestEntity((long) x, "color", (long) x, null, null)));

		waitUntilTrue(() -> this.testEntityRepository.count() == PARALLEL_OPERATIONS - 1);

		performOperation(x -> assertThat(this.testEntityRepository.getIds(x).length).isEqualTo(x));

		performOperation(x -> this.testEntityRepository.deleteBySize((long) x));

		waitUntilTrue(() -> this.testEntityRepository.count() == 0);
	}

	private void performOperation(IntConsumer function) {
		IntStream.range(1, PARALLEL_OPERATIONS).parallel().forEach(function);
	}
}
