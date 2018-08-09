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

package org.springframework.cloud.gcp.data.datastore.it;

import com.google.cloud.datastore.Blob;
import com.google.common.collect.ImmutableList;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

/**
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { DatastoreIntegrationTestConfiguration.class })
public class DatastoreIntegrationTests {

	@Autowired
	private TestEntityRepository testEntityRepository;

	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"Datastre integration tests are disabled. Please use '-Dit.datastore=true' "
						+ "to enable them. ",
				System.getProperty("it.datastore"), is("true"));
	}

	@Test
	public void testRepository() {

		TestEntity testEntityA = new TestEntity();
		testEntityA.setId("a");

		TestEntity testEntityB = new TestEntity();
		testEntityB.setId("b");

		this.testEntityRepository.saveAll(ImmutableList.of(testEntityA, testEntityB));

		assertNull(this.testEntityRepository.findById("a").get().getBlobField());

		testEntityA.setBlobField(Blob.copyFrom("testValueA".getBytes()));

		this.testEntityRepository.save(testEntityA);

		assertEquals(Blob.copyFrom("testValueA".getBytes()),
				this.testEntityRepository.findById("a").get().getBlobField());

		testEntityA.setBlobField(null);

		this.testEntityRepository.save(testEntityA);

		assertNull(this.testEntityRepository.findById("a").get().getBlobField());

		assertThat(this.testEntityRepository.findAllById(ImmutableList.of("a", "b")),
				iterableWithSize(2));

		this.testEntityRepository.delete(testEntityA);

		assertFalse(this.testEntityRepository.findById("a").isPresent());

		this.testEntityRepository.deleteAll();

		assertFalse(this.testEntityRepository.findAllById(ImmutableList.of("a", "b"))
				.iterator().hasNext());
	}

}
