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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.cloud.datastore.Blob;
import com.google.common.collect.ImmutableList;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

/**
 * @author Chengyuan Zhao
 * @author Dmitry Solomakha
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { DatastoreIntegrationTestConfiguration.class })
public class DatastoreIntegrationTests {

	// queries are eventually consistent, so we may need to retry a few times.
	private static final int QUERY_WAIT_ATTEMPTS = 100;

	private static final int QUERY_WAIT_INTERVAL_MILLIS = 1000;

	@Autowired
	private TestEntityRepository testEntityRepository;

	@Autowired
	private DatastoreTemplate datastoreTemplate;

	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"Datastore integration tests are disabled. Please use '-Dit.datastore=true' "
						+ "to enable them. ",
				System.getProperty("it.datastore"), is("true"));
	}

	@Test
	public void testSaveAndDeleteRepository() throws InterruptedException {

		TestEntity testEntityA = new TestEntity(1L, "red", 1L, null);

		TestEntity testEntityB = new TestEntity(2L, "blue", 1L, null);

		TestEntity testEntityC = new TestEntity(3L, "red", 1L, null);

		TestEntity testEntityD = new TestEntity(4L, "red", 1L, null);

		this.testEntityRepository.saveAll(
				ImmutableList.of(testEntityA, testEntityB, testEntityC, testEntityD));

		assertNull(this.testEntityRepository.findById(1L).get().getBlobField());

		testEntityA.setBlobField(Blob.copyFrom("testValueA".getBytes()));

		this.testEntityRepository.save(testEntityA);

		assertEquals(Blob.copyFrom("testValueA".getBytes()),
				this.testEntityRepository.findById(1L).get().getBlobField());

		List<TestEntity> foundByCustomQuery = Collections.emptyList();
		List<TestEntity> foundByCustomProjectionQuery = Collections.emptyList();

		for (int i = 0; i < QUERY_WAIT_ATTEMPTS; i++) {
			if (!foundByCustomQuery.isEmpty() && this.testEntityRepository
					.countBySizeAndColor(1L, "red") == 3) {
				break;
			}
			Thread.sleep(QUERY_WAIT_INTERVAL_MILLIS);
			foundByCustomQuery = this.testEntityRepository
					.findEntitiesWithCustomQuery(1L);
			foundByCustomProjectionQuery = this.testEntityRepository
					.findEntitiesWithCustomProjectionQuery(1L);
		}
		assertEquals(1, this.testEntityRepository.countBySizeAndColor(1, "blue"));
		assertEquals(3,
				this.testEntityRepository.countBySizeAndColor(1, "red"));
		assertThat(
				this.testEntityRepository.findTop3BySizeAndColor(1, "red").stream()
						.map(TestEntity::getId).collect(Collectors.toList()),
				containsInAnyOrder(1L, 3L, 4L));

		assertEquals(1, foundByCustomQuery.size());
		assertEquals(4, this.testEntityRepository.countEntitiesWithCustomQuery(1L));
		assertTrue(this.testEntityRepository.existsByEntitiesWithCustomQuery(1L));
		assertEquals(Blob.copyFrom("testValueA".getBytes()),
				foundByCustomQuery.get(0).getBlobField());

		assertEquals(1, foundByCustomProjectionQuery.size());
		assertNull(foundByCustomProjectionQuery.get(0).getBlobField());
		assertEquals((Long) 1L, foundByCustomProjectionQuery.get(0).getId());

		testEntityA.setBlobField(null);

		this.testEntityRepository.save(testEntityA);

		assertNull(this.testEntityRepository.findById(1L).get().getBlobField());

		assertThat(this.testEntityRepository.findAllById(ImmutableList.of(1L, 2L)),
				iterableWithSize(2));

		this.testEntityRepository.delete(testEntityA);

		assertFalse(this.testEntityRepository.findById(1L).isPresent());

		this.testEntityRepository.deleteAll();

		assertFalse(this.testEntityRepository.findAllById(ImmutableList.of(1L, 2L))
				.iterator().hasNext());
	}

	@Test
	public void embeddedEntitiesTest() {
		EmbeddableTreeNode treeNode10 = new EmbeddableTreeNode(10, null, null);
		EmbeddableTreeNode treeNode8 = new EmbeddableTreeNode(8, null, null);
		EmbeddableTreeNode treeNode9 = new EmbeddableTreeNode(9, treeNode8, treeNode10);
		EmbeddableTreeNode treeNode7 = new EmbeddableTreeNode(7, null, treeNode9);


		this.datastoreTemplate.save(treeNode7);

		EmbeddableTreeNode loaded = this.datastoreTemplate.findById(7L, EmbeddableTreeNode.class);

		this.datastoreTemplate.deleteAll(EmbeddableTreeNode.class);
		assertEquals(treeNode7, loaded);
	}

	@Test
	public void embeddedCollectionTest() {
		EmbeddableTreeNode treeNode10 = new EmbeddableTreeNode(10, null, null);
		EmbeddableTreeNode treeNode8 = new EmbeddableTreeNode(8, null, null);
		EmbeddableTreeNode treeNode9 = new EmbeddableTreeNode(9, treeNode8, treeNode10);
		EmbeddableTreeNode treeNode7 = new EmbeddableTreeNode(7, null, treeNode9);

		TreeCollection treeCollection =
				new TreeCollection(1L, Arrays.asList(treeNode7, treeNode8, treeNode9, treeNode10));

		this.datastoreTemplate.save(treeCollection);

		TreeCollection loaded = this.datastoreTemplate.findById(1L, TreeCollection.class);

		this.datastoreTemplate.deleteAll(TreeCollection.class);
		assertEquals(treeCollection, loaded);
	}

}
