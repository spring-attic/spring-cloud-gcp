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

package org.springframework.cloud.gcp.data.datastore.it;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.DatastoreReaderWriter;
import com.google.cloud.datastore.Key;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.it.TestEntity.Shape;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Integration tests for Datastore that use many features.
 *
 * @author Chengyuan Zhao
 * @author Dmitry Solomakha
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { DatastoreIntegrationTestConfiguration.class })
public class DatastoreIntegrationTests {

	// queries are eventually consistent, so we may need to retry a few times.
	private static final int QUERY_WAIT_INTERVAL_SECONDS = 15;

	// This value is multiplied against recorded actual times needed to wait for eventual
	// consistency.
	private static final int WAIT_FOR_EVENTUAL_CONSISTENCY_SAFETY_MULTIPLE = 3;

	@Autowired
	private TestEntityRepository testEntityRepository;

	@Autowired
	private DatastoreTemplate datastoreTemplate;

	@Autowired
	private TransactionalTemplateService transactionalTemplateService;

	@Autowired
	private DatastoreReaderWriter datastore;

	private Key keyForMap;

	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"Datastore integration tests are disabled. Please use '-Dit.datastore=true' "
						+ "to enable them. ",
				System.getProperty("it.datastore"), is("true"));
	}

	@After
	public void deleteAll() {
		this.datastoreTemplate.deleteAll(EmbeddableTreeNode.class);
		this.datastoreTemplate.deleteAll(AncestorEntity.class);
		this.datastoreTemplate.deleteAll(AncestorEntity.DescendantEntry.class);
		this.datastoreTemplate.deleteAll(TreeCollection.class);
		this.datastoreTemplate.deleteAll(ReferenceEntry.class);
		this.testEntityRepository.deleteAll();
		if (this.keyForMap != null) {
			this.datastore.delete(this.keyForMap);
		}
	}

	@Test
	public void testSaveAndDeleteRepository() throws InterruptedException {

		TestEntity testEntityA = new TestEntity(1L, "red", 1L, Shape.CIRCLE, null);

		TestEntity testEntityB = new TestEntity(2L, "blue", 1L, Shape.CIRCLE, null);

		TestEntity testEntityC = new TestEntity(3L, "red", 1L, Shape.CIRCLE, null);

		TestEntity testEntityD = new TestEntity(4L, "red", 1L, Shape.SQUARE, null);

		List<TestEntity> allTestEntities = ImmutableList.of(testEntityA, testEntityB,
				testEntityC, testEntityD);

		this.testEntityRepository.saveAll(allTestEntities);

		long millisWaited = waitUntilTrue(
				() -> this.testEntityRepository.countBySize(1L) == 4);

		assertThat(this.testEntityRepository.findByShape(Shape.SQUARE).stream()
				.map((x) -> x.getId()).collect(Collectors.toList())).contains(4L);

		assertThat(this.testEntityRepository.findByColor("red", PageRequest.of(0, 1)).hasNext()).isTrue();
		assertThat(this.testEntityRepository.findByColor("red", PageRequest.of(1, 1)).hasNext()).isTrue();
		assertThat(
				this.testEntityRepository.findByColor("red", PageRequest.of(2, 1)).hasNext()).isFalse();

		Page<TestEntity> circles = this.testEntityRepository.findByShape(Shape.CIRCLE, PageRequest.of(0, 2));
		assertThat(circles.getTotalElements()).isEqualTo(3L);
		assertThat(circles.getTotalPages()).isEqualTo(2);
		assertThat(circles.get().count()).isEqualTo(2L);
		assertThat(circles.get().allMatch((e) -> e.getShape().equals(Shape.CIRCLE))).isTrue();

		assertThat(this.testEntityRepository.findByEnumQueryParam(Shape.SQUARE).stream()
				.map((x) -> x.getId()).collect(Collectors.toList())).contains(4L);

		assertThat(this.testEntityRepository.deleteBySize(1L)).isEqualTo(4);

		this.testEntityRepository.saveAll(allTestEntities);

		millisWaited = Math.max(millisWaited,
				waitUntilTrue(() -> this.testEntityRepository.countBySize(1L) == 4));

		assertThat(
				this.testEntityRepository.removeByColor("red").stream()
						.map(TestEntity::getId).collect(Collectors.toList()))
								.containsExactlyInAnyOrder(1L, 3L, 4L);

		this.testEntityRepository.saveAll(allTestEntities);

		assertThat(this.testEntityRepository.findById(1L).get().getBlobField()).isNull();

		testEntityA.setBlobField(Blob.copyFrom("testValueA".getBytes()));

		this.testEntityRepository.save(testEntityA);

		assertThat(this.testEntityRepository.findById(1L).get().getBlobField())
				.isEqualTo(Blob.copyFrom("testValueA".getBytes()));

		millisWaited = Math.max(millisWaited, waitUntilTrue(
				() -> this.testEntityRepository.countBySizeAndColor(1L, "red") == 3));

		List<TestEntity> foundByCustomQuery = this.testEntityRepository
				.findEntitiesWithCustomQuery(1L);
		TestEntity[] foundByCustomProjectionQuery = this.testEntityRepository
				.findEntitiesWithCustomProjectionQuery(1L);

		assertThat(this.testEntityRepository.countBySizeAndColor(1, "blue")).isEqualTo(1);
		assertThat(this.testEntityRepository.getById(2L).getColor()).isEqualTo("blue");
		assertThat(this.testEntityRepository.countBySizeAndColor(1, "red")).isEqualTo(3);
		assertThat(
				this.testEntityRepository.findTop3BySizeAndColor(1, "red").stream()
						.map(TestEntity::getId).collect(Collectors.toList()))
								.containsExactlyInAnyOrder(1L, 3L, 4L);

		assertThat(this.testEntityRepository.getKeys().stream().map(Key::getId).collect(Collectors.toList()))
				.containsExactlyInAnyOrder(1L, 2L, 3L, 4L);

		assertThat(foundByCustomQuery.size()).isEqualTo(1);
		assertThat(this.testEntityRepository.countEntitiesWithCustomQuery(1L)).isEqualTo(4);
		assertThat(this.testEntityRepository.existsByEntitiesWithCustomQuery(1L)).isTrue();
		assertThat(foundByCustomQuery.get(0).getBlobField()).isEqualTo(Blob.copyFrom("testValueA".getBytes()));

		assertThat(foundByCustomProjectionQuery.length).isEqualTo(1);
		assertThat(foundByCustomProjectionQuery[0].getBlobField()).isNull();
		assertThat(foundByCustomProjectionQuery[0].getId()).isEqualTo((Long) 1L);

		testEntityA.setBlobField(null);

		assertThat(this.testEntityRepository.getKey().getId()).isEqualTo((Long) 1L);
		assertThat(this.testEntityRepository.getIds(1L).length).isEqualTo(1);
		assertThat(this.testEntityRepository.getOneId(1L)).isEqualTo(1);
		assertThat(this.testEntityRepository.getOneTestEntity(1L)).isNotNull();

		this.testEntityRepository.save(testEntityA);

		assertThat(this.testEntityRepository.findById(1L).get().getBlobField()).isNull();

		assertThat(this.testEntityRepository.findAllById(ImmutableList.of(1L, 2L))).hasSize(2);

		this.testEntityRepository.delete(testEntityA);

		assertThat(this.testEntityRepository.findById(1L).isPresent()).isFalse();

		this.testEntityRepository.deleteAll();

		this.transactionalTemplateService.testSaveAndStateConstantInTransaction(
				allTestEntities,
				millisWaited * WAIT_FOR_EVENTUAL_CONSISTENCY_SAFETY_MULTIPLE);

		millisWaited = Math.max(millisWaited,
				waitUntilTrue(() -> this.testEntityRepository.countBySize(1L) == 4));

		this.testEntityRepository.deleteAll();

		try {
			this.transactionalTemplateService
					.testSaveInTransactionFailed(allTestEntities);
		}
		catch (Exception ignored) {
		}

		// we wait a period long enough that the previously attempted failed save would
		// show up if it is unexpectedly successful and committed.
		Thread.sleep(millisWaited * WAIT_FOR_EVENTUAL_CONSISTENCY_SAFETY_MULTIPLE);

		assertThat(this.testEntityRepository.count()).isEqualTo(0);

		assertThat(this.testEntityRepository.findAllById(ImmutableList.of(1L, 2L)).iterator().hasNext()).isFalse();
	}

	@Test
	public void embeddedEntitiesTest() {
		EmbeddableTreeNode treeNode10 = new EmbeddableTreeNode(10, null, null);
		EmbeddableTreeNode treeNode8 = new EmbeddableTreeNode(8, null, null);
		EmbeddableTreeNode treeNode9 = new EmbeddableTreeNode(9, treeNode8, treeNode10);
		EmbeddableTreeNode treeNode7 = new EmbeddableTreeNode(7, null, treeNode9);


		this.datastoreTemplate.save(treeNode7);

		EmbeddableTreeNode loaded = this.datastoreTemplate.findById(7L, EmbeddableTreeNode.class);

		assertThat(loaded).isEqualTo(treeNode7);
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

		assertThat(loaded).isEqualTo(treeCollection);
	}

	@Test
	public void ancestorsTest() {
		AncestorEntity.DescendantEntry descendantEntryA = new AncestorEntity.DescendantEntry("a");
		AncestorEntity.DescendantEntry descendantEntryB = new AncestorEntity.DescendantEntry("b");
		AncestorEntity.DescendantEntry descendantEntryC = new AncestorEntity.DescendantEntry("c");

		AncestorEntity ancestorEntity =
				new AncestorEntity("abc", Arrays.asList(descendantEntryA, descendantEntryB, descendantEntryC));

		this.datastoreTemplate.save(ancestorEntity);
		waitUntilTrue(() -> {
			AncestorEntity byId = this.datastoreTemplate.findById(ancestorEntity.id, AncestorEntity.class);
			return byId != null && byId.descendants.size() == 3;
		});

		AncestorEntity loadedEntity = this.datastoreTemplate.findById(ancestorEntity.id, AncestorEntity.class);
		assertThat(loadedEntity).isEqualTo(ancestorEntity);

		ancestorEntity.descendants.forEach((descendatEntry) -> descendatEntry.name = descendatEntry.name + " updated");
		this.datastoreTemplate.save(ancestorEntity);
		waitUntilTrue(() ->
				this.datastoreTemplate.findAll(AncestorEntity.DescendantEntry.class)
						.stream().allMatch((descendatEntry) -> descendatEntry.name.contains("updated")));

		AncestorEntity loadedEntityAfterUpdate =
				this.datastoreTemplate.findById(ancestorEntity.id, AncestorEntity.class);
		assertThat(loadedEntityAfterUpdate).isEqualTo(ancestorEntity);
	}

	@Test
	public void referenceTest() {
		ReferenceEntry child1 = new ReferenceEntry("child1", null, null);
		ReferenceEntry child2 = new ReferenceEntry("child2", null, null);
		ReferenceEntry sibling = new ReferenceEntry("sibling", null, null);
		ReferenceEntry parent = new ReferenceEntry("parent", sibling, Arrays.asList(child1, child2));

		this.datastoreTemplate.save(parent);
		waitUntilTrue(() -> this.datastoreTemplate.findAll(ReferenceEntry.class).size() == 4);

		ReferenceEntry loadedParent = this.datastoreTemplate.findById(parent.id, ReferenceEntry.class);
		assertThat(loadedParent).isEqualTo(parent);

		parent.name = "parent updated";
		parent.childeren.forEach((child) -> child.name = child.name + " updated");
		parent.sibling.name = "sibling updated";

		this.datastoreTemplate.save(parent);

		waitUntilTrue(() ->
				this.datastoreTemplate.findAll(ReferenceEntry.class)
						.stream().allMatch((entry) -> entry.name.contains("updated")));

		ReferenceEntry loadedParentAfterUpdate = this.datastoreTemplate.findById(parent.id, ReferenceEntry.class);
		assertThat(loadedParentAfterUpdate).isEqualTo(parent);
	}

	@Test
	public void mapTest() {
		Map<String, Long> map = new HashMap<>();
		map.put("field1", 1L);
		map.put("field2", 2L);
		map.put("field3", 3L);

		this.keyForMap = this.datastoreTemplate.createKey("map", "myMap");

		this.datastoreTemplate.writeMap(this.keyForMap, map);
		Map<String, Long> loadedMap = this.datastoreTemplate.findByIdAsMap(this.keyForMap, Long.class);

		assertThat(loadedMap).isEqualTo(map);
	}

	private long waitUntilTrue(Supplier<Boolean> condition) {
		Stopwatch stopwatch = Stopwatch.createStarted();
		Awaitility.await().atMost(QUERY_WAIT_INTERVAL_SECONDS, TimeUnit.SECONDS).until(condition::get);
		stopwatch.stop();
		return stopwatch.elapsed(TimeUnit.MILLISECONDS);
	}
}
