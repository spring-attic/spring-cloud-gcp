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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreReaderWriter;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.ProjectionEntityQuery;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Descendants;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DiscriminatorField;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DiscriminatorValue;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Unindexed;
import org.springframework.cloud.gcp.data.datastore.entities.CustomMap;
import org.springframework.cloud.gcp.data.datastore.entities.ServiceConfiguration;
import org.springframework.cloud.gcp.data.datastore.it.TestEntity.Shape;
import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository;
import org.springframework.cloud.gcp.data.datastore.repository.query.Query;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionSystemException;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for Datastore that use many features.
 *
 * @author Chengyuan Zhao
 * @author Dmitry Solomakha
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { DatastoreIntegrationTestConfiguration.class })
public class DatastoreIntegrationTests extends AbstractDatastoreIntegrationTests {

	// This value is multiplied against recorded actual times needed to wait for eventual
	// consistency.
	private static final int WAIT_FOR_EVENTUAL_CONSISTENCY_SAFETY_MULTIPLE = 3;

	@Autowired
	private TestEntityRepository testEntityRepository;

	@Autowired
	private PetRepository petRepository;

	@Autowired
	private DogRepository dogRepository;

	@SpyBean
	private DatastoreTemplate datastoreTemplate;

	@Autowired
	private TransactionalTemplateService transactionalTemplateService;

	@Autowired
	private DatastoreReaderWriter datastore;

	private Key keyForMap;
	private long millisWaited;

	private final TestEntity testEntityA = new TestEntity(1L, "red", 1L, Shape.CIRCLE, null);

	private final TestEntity testEntityB = new TestEntity(2L, "blue", 2L, Shape.CIRCLE, null);

	private final TestEntity testEntityC = new TestEntity(3L, "red", 1L, Shape.CIRCLE, null, new EmbeddedEntity("c"));

	private final TestEntity testEntityD = new TestEntity(4L, "red", 1L, Shape.SQUARE, null, new EmbeddedEntity("d"));

	private final List<TestEntity> allTestEntities;

	{
		this.allTestEntities = Arrays.asList(this.testEntityA, this.testEntityB, this.testEntityC, this.testEntityD);
	}

	/**
	 * Used to check exception types and messages.
	 */
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

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
		this.datastoreTemplate.deleteAll(ParentEntity.class);
		this.datastoreTemplate.deleteAll(SubEntity.class);
		this.datastoreTemplate.deleteAll(Pet.class);
		this.datastoreTemplate.deleteAll(PetOwner.class);
		this.datastoreTemplate.deleteAll(Event.class);
		this.datastoreTemplate.deleteAll(LazyEntity.class);
		this.testEntityRepository.deleteAll();
		if (this.keyForMap != null) {
			this.datastore.delete(this.keyForMap);
		}
	}

	@Before
	public void saveEntities() {
		this.testEntityRepository.saveAll(this.allTestEntities);

		this.millisWaited = waitUntilTrue(
				() -> this.testEntityRepository.countBySize(1L) == 3);

	}

	@Test
	public void testFindByExample() {
		assertThat(this.testEntityRepository
				.findAll(Example.of(new TestEntity(null, "red", null, Shape.CIRCLE, null))))
				.containsExactlyInAnyOrder(this.testEntityA, this.testEntityC);

		assertThat(this.testEntityRepository
				.findAll(Example.of(new TestEntity(2L, "blue", null, null, null))))
				.containsExactly(this.testEntityB);

		assertThat(this.testEntityRepository
				.findAll(Example.of(new TestEntity(2L, "red", null, null, null))))
				.isEmpty();

		Page<TestEntity> result1 = this.testEntityRepository
				.findAll(
						Example.of(new TestEntity(null, null, null, null, null)),
						PageRequest.of(0, 2, Sort.by("size")));
		assertThat(result1.getTotalElements()).isEqualTo(4);
		assertThat(result1.getNumber()).isEqualTo(0);
		assertThat(result1.getNumberOfElements()).isEqualTo(2);
		assertThat(result1.getTotalPages()).isEqualTo(2);
		assertThat(result1.hasNext()).isEqualTo(true);
		assertThat(result1).containsExactly(this.testEntityA, this.testEntityC);

		Page<TestEntity> result2 = this.testEntityRepository
				.findAll(
						Example.of(new TestEntity(null, null, null, null, null)),
						result1.getPageable().next());
		assertThat(result2.getTotalElements()).isEqualTo(4);
		assertThat(result2.getNumber()).isEqualTo(1);
		assertThat(result2.getNumberOfElements()).isEqualTo(2);
		assertThat(result2.getTotalPages()).isEqualTo(2);
		assertThat(result2.hasNext()).isEqualTo(false);
		assertThat(result2).containsExactly(this.testEntityD, this.testEntityB);

		assertThat(this.testEntityRepository
				.findAll(
						Example.of(new TestEntity(null, null, null, null, null)),
						Sort.by(Sort.Direction.ASC, "size")))
				.containsExactly(this.testEntityA, this.testEntityB, this.testEntityC, this.testEntityD);

		assertThat(this.testEntityRepository
				.count(Example.of(new TestEntity(null, "red", null, Shape.CIRCLE, null),
						ExampleMatcher.matching().withIgnorePaths("size", "blobField"))))
				.isEqualTo(2);

		assertThat(this.testEntityRepository
				.exists(Example.of(new TestEntity(null, "red", null, Shape.CIRCLE, null),
						ExampleMatcher.matching().withIgnorePaths("size", "blobField"))))
				.isEqualTo(true);

		assertThat(this.testEntityRepository
				.exists(Example.of(new TestEntity(null, "red", null, null, null),
						ExampleMatcher.matching().withIgnorePaths("id").withIncludeNullValues())))
				.isEqualTo(false);

		assertThat(this.testEntityRepository
				.exists(Example.of(new TestEntity(null, "red", null, null, null))))
				.isEqualTo(true);
	}

	@Test
	public void testSlice() {
		List<TestEntity> results = new ArrayList<>();
		Slice<TestEntity> slice = this.testEntityRepository.findEntitiesWithCustomQuerySlice("red",
				PageRequest.of(0, 1));

		assertThat(slice.hasNext()).isTrue();
		assertThat(slice).hasSize(1);
		results.addAll(slice.getContent());

		slice = this.testEntityRepository.findEntitiesWithCustomQuerySlice("red",
				slice.getPageable().next());

		assertThat(slice.hasNext()).isTrue();
		assertThat(slice).hasSize(1);
		results.addAll(slice.getContent());

		slice = this.testEntityRepository.findEntitiesWithCustomQuerySlice("red",
				slice.getPageable().next());

		assertThat(slice.hasNext()).isFalse();
		assertThat(slice).hasSize(1);
		results.addAll(slice.getContent());

		assertThat(results).containsExactlyInAnyOrder(this.testEntityA, this.testEntityC, this.testEntityD);
	}

	@Test
	public void testPage() {
		List<TestEntity> results = new ArrayList<>();
		Page<TestEntity> page = this.testEntityRepository.findEntitiesWithCustomQueryPage("red",
				PageRequest.of(0, 2));

		assertThat(page.hasNext()).isTrue();
		assertThat(page).hasSize(2);
		assertThat(page.getTotalPages()).isEqualTo(2);
		assertThat(page.getTotalElements()).isEqualTo(3);
		results.addAll(page.getContent());

		page = this.testEntityRepository.findEntitiesWithCustomQueryPage("red",
				page.getPageable().next());

		assertThat(page.hasNext()).isFalse();
		assertThat(page).hasSize(1);
		assertThat(page.getTotalPages()).isEqualTo(2);
		assertThat(page.getTotalElements()).isEqualTo(3);
		results.addAll(page.getContent());

		assertThat(results).containsExactlyInAnyOrder(this.testEntityA, this.testEntityC, this.testEntityD);
	}

	@Test
	public void testProjectionPage() {
		Page<String> page = this.testEntityRepository
				.getColorsPage(PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "color")));

		assertThat(page.hasNext()).isTrue();
		assertThat(page).hasSize(3);
		assertThat(page.getTotalPages()).isEqualTo(2);
		assertThat(page.getTotalElements()).isEqualTo(4);
		assertThat(page.getContent()).containsExactly("red", "red", "red");

		page = this.testEntityRepository.getColorsPage(page.getPageable().next());

		assertThat(page.hasNext()).isFalse();
		assertThat(page).hasSize(1);
		assertThat(page.getTotalPages()).isEqualTo(2);
		assertThat(page.getTotalElements()).isEqualTo(4);
		assertThat(page.getContent()).containsExactly("blue");
	}

	@Test
	public void testSliceSort() {
		List<TestEntity> results = this.testEntityRepository.findEntitiesWithCustomQuerySort(Sort.by("color"));

		assertThat(results.get(0)).isEqualTo(this.testEntityB);
		assertThat(results).containsExactlyInAnyOrder(this.testEntityA, this.testEntityB, this.testEntityC,
				this.testEntityD);
	}

	@Test
	public void testSliceSortDesc() {
		List<TestEntity> results = this.testEntityRepository
				.findEntitiesWithCustomQuerySort(Sort.by(Sort.Direction.DESC, "color"));

		assertThat(results.get(results.size() - 1)).isEqualTo(this.testEntityB);
		assertThat(results).containsExactlyInAnyOrder(this.testEntityA, this.testEntityB, this.testEntityC,
				this.testEntityD);
	}

	@Test
	public void testSaveAndDeleteRepository() throws InterruptedException {
		assertThat(this.testEntityRepository.findByEmbeddedEntityStringField("c")).containsExactly(this.testEntityC);
		assertThat(this.testEntityRepository.findByEmbeddedEntityStringField("d")).containsExactly(this.testEntityD);

		assertThat(this.testEntityRepository.findFirstByColor("blue")).contains(this.testEntityB);
		assertThat(this.testEntityRepository.findFirstByColor("green")).isNotPresent();

		assertThat(this.testEntityRepository.getByColor("green")).isNull();
		assertThatThrownBy(() -> this.testEntityRepository.findByColor("green"))
				.isInstanceOf(EmptyResultDataAccessException.class)
				.hasMessageMatching("Result must not be null!");

		assertThat(this.testEntityRepository.getByColor("blue")).isEqualTo(this.testEntityB);

		assertThat(this.testEntityRepository.getByColorAndIdGreaterThanEqualOrderById("red", 3L))
				.containsExactly(this.testEntityC, this.testEntityD);

		assertThat(this.testEntityRepository.findByShape(Shape.SQUARE).stream()
				.map(TestEntity::getId).collect(Collectors.toList())).contains(4L);

		Slice<TestEntity> red1 = this.testEntityRepository.findByColor("red", PageRequest.of(0, 1));
		assertThat(red1.hasNext()).isTrue();
		assertThat(red1.getNumber()).isEqualTo(0);
		Slice<TestEntity> red2 = this.testEntityRepository.findByColor("red", red1.getPageable().next());
		assertThat(red2.hasNext()).isTrue();
		assertThat(red2.getNumber()).isEqualTo(1);
		Slice<TestEntity> red3 = this.testEntityRepository.findByColor("red", red2.getPageable().next());
		assertThat(red3.hasNext()).isFalse();
		assertThat(red3.getNumber()).isEqualTo(2);

		assertThat(this.testEntityRepository.findByColor("red", PageRequest.of(1, 1)).hasNext()).isTrue();
		assertThat(
				this.testEntityRepository.findByColor("red", PageRequest.of(2, 1)).hasNext()).isFalse();

		Page<TestEntity> circles = this.testEntityRepository.findByShape(Shape.CIRCLE, PageRequest.of(0, 2));
		assertThat(circles.getTotalElements()).isEqualTo(3L);
		assertThat(circles.getTotalPages()).isEqualTo(2);
		assertThat(circles.get().count()).isEqualTo(2L);
		assertThat(circles.get().allMatch((e) -> e.getShape().equals(Shape.CIRCLE))).isTrue();

		Page<TestEntity> circlesNext = this.testEntityRepository.findByShape(Shape.CIRCLE, circles.nextPageable());
		assertThat(circlesNext.getTotalElements()).isEqualTo(3L);
		assertThat(circlesNext.getTotalPages()).isEqualTo(2);
		assertThat(circlesNext.get().count()).isEqualTo(1L);
		assertThat(circlesNext.get().allMatch((e) -> e.getShape().equals(Shape.CIRCLE))).isTrue();

		assertThat(this.testEntityRepository.findByEnumQueryParam(Shape.SQUARE).stream()
				.map(TestEntity::getId).collect(Collectors.toList())).contains(4L);

		assertThat(this.testEntityRepository.deleteBySize(1L)).isEqualTo(3);
		assertThat(this.testEntityRepository.countBySize(1L)).isEqualTo(0);

		this.testEntityRepository.saveAll(this.allTestEntities);

		this.testEntityRepository.deleteBySizeEquals(1L);
		assertThat(this.testEntityRepository.countBySize(1L)).isEqualTo(0);

		//test saveAll for iterable
		Iterable<TestEntity> testEntities = () -> this.allTestEntities.iterator();
		this.testEntityRepository.saveAll(testEntities);

		this.millisWaited = Math.max(this.millisWaited,
				waitUntilTrue(() -> this.testEntityRepository.countBySize(1L) == 3));

		assertThat(
				this.testEntityRepository.removeByColor("red").stream()
						.map(TestEntity::getId).collect(Collectors.toList()))
								.containsExactlyInAnyOrder(1L, 3L, 4L);

		this.testEntityRepository.saveAll(this.allTestEntities);
		assertThat(this.testEntityRepository.findById(1L).get().getBlobField()).isNull();

		this.testEntityA.setBlobField(Blob.copyFrom("testValueA".getBytes()));

		this.testEntityRepository.save(this.testEntityA);

		assertThat(this.testEntityRepository.findById(1L).get().getBlobField())
				.isEqualTo(Blob.copyFrom("testValueA".getBytes()));

		this.millisWaited = Math.max(this.millisWaited, waitUntilTrue(
				() -> this.testEntityRepository.countBySizeAndColor(1L, "red") == 3));

		assertThat(this.testEntityRepository
				.findEntitiesWithCustomQueryWithId(1L, this.datastoreTemplate.createKey(TestEntity.class, 1L)))
				.containsOnly(this.testEntityA);

		List<TestEntity> foundByCustomQuery = this.testEntityRepository
				.findEntitiesWithCustomQuery(1L);
		TestEntity[] foundByCustomProjectionQuery = this.testEntityRepository
				.findEntitiesWithCustomProjectionQuery(1L);

		assertThat(this.testEntityRepository.countBySizeAndColor(2, "blue")).isEqualTo(1);
		assertThat(this.testEntityRepository.getBySize(2L).getColor()).isEqualTo("blue");
		assertThat(this.testEntityRepository.countBySizeAndColor(1, "red")).isEqualTo(3);
		assertThat(
				this.testEntityRepository.findTop3BySizeAndColor(1, "red").stream()
						.map(TestEntity::getId).collect(Collectors.toList()))
								.containsExactlyInAnyOrder(1L, 3L, 4L);

		assertThat(this.testEntityRepository.getKeys().stream().map(Key::getId).collect(Collectors.toList()))
				.containsExactlyInAnyOrder(1L, 2L, 3L, 4L);

		assertThat(foundByCustomQuery.size()).isEqualTo(3);
		assertThat(this.testEntityRepository.countEntitiesWithCustomQuery(1L)).isEqualTo(3);
		assertThat(this.testEntityRepository.existsByEntitiesWithCustomQuery(1L)).isTrue();
		assertThat(this.testEntityRepository.existsByEntitiesWithCustomQuery(100L)).isFalse();
		assertThat(foundByCustomQuery.get(0).getBlobField()).isEqualTo(Blob.copyFrom("testValueA".getBytes()));

		assertThat(foundByCustomProjectionQuery.length).isEqualTo(3);
		assertThat(foundByCustomProjectionQuery[0].getBlobField()).isNull();
		assertThat(foundByCustomProjectionQuery[0].getId()).isEqualTo((Long) 1L);

		this.testEntityA.setBlobField(null);

		assertThat(this.testEntityRepository.getKey().getId()).isEqualTo((Long) 1L);
		assertThat(this.testEntityRepository.getSizes(1L).length).isEqualTo(3);
		assertThat(this.testEntityRepository.getOneSize(2L)).isEqualTo(2);
		assertThat(this.testEntityRepository.getOneTestEntity(2L)).isNotNull();

		this.testEntityRepository.save(this.testEntityA);

		assertThat(this.testEntityRepository.findById(1L).get().getBlobField()).isNull();

		assertThat(this.testEntityRepository.findAllById(Arrays.asList(1L, 2L))).hasSize(2);

		this.testEntityRepository.delete(this.testEntityA);

		assertThat(this.testEntityRepository.findById(1L).isPresent()).isFalse();

		this.testEntityRepository.deleteAll();

		this.transactionalTemplateService.testSaveAndStateConstantInTransaction(
				this.allTestEntities,
				this.millisWaited * WAIT_FOR_EVENTUAL_CONSISTENCY_SAFETY_MULTIPLE);

		this.millisWaited = Math.max(this.millisWaited,
				waitUntilTrue(() -> this.testEntityRepository.countBySize(1L) == 3));

		this.testEntityRepository.deleteAll();

		try {
			this.transactionalTemplateService
					.testSaveInTransactionFailed(this.allTestEntities);
		}
		catch (Exception ignored) {
		}

		// we wait a period long enough that the previously attempted failed save would
		// show up if it is unexpectedly successful and committed.
		sleep(this.millisWaited * WAIT_FOR_EVENTUAL_CONSISTENCY_SAFETY_MULTIPLE);

		assertThat(this.testEntityRepository.count()).isEqualTo(0);

		assertThat(this.testEntityRepository.findAllById(Arrays.asList(1L, 2L)).iterator().hasNext()).isFalse();
	}

	@Test
	public void projectionTest() {
		reset(datastoreTemplate);
		assertThat(this.testEntityRepository.findBySize(2L).getColor()).isEqualTo("blue");

		ProjectionEntityQuery projectionQuery =
				com.google.cloud.datastore.Query.newProjectionEntityQueryBuilder()
						.addProjection("color")
						.setFilter(PropertyFilter.eq("size", 2L))
						.setKind("test_entities_ci").setLimit(1).build();

		verify(datastoreTemplate).queryKeysOrEntities(eq(projectionQuery), any());
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
		ReferenceEntry parent = saveEntitiesGraph();

		ReferenceEntry loadedParent = this.datastoreTemplate.findById(parent.id, ReferenceEntry.class);
		assertThat(loadedParent).isEqualTo(parent);

		parent.name = "parent updated";
		parent.children.forEach((child) -> child.name = child.name + " updated");
		parent.sibling.name = "sibling updated";

		this.datastoreTemplate.save(parent);

		waitUntilTrue(() ->
				this.datastoreTemplate.findAll(ReferenceEntry.class)
						.stream().allMatch((entry) -> entry.name.contains("updated")));

		ReferenceEntry loadedParentAfterUpdate = this.datastoreTemplate.findById(parent.id, ReferenceEntry.class);
		assertThat(loadedParentAfterUpdate).isEqualTo(parent);
	}

	@Test
	public void lazyReferenceCollectionTest() {
		ReferenceEntry parent = saveEntitiesGraph();

		ReferenceEntry lazyParent = this.datastoreTemplate.findById(parent.id, ReferenceEntry.class);

		//Saving an entity with not loaded lazy field
		this.datastoreTemplate.save(lazyParent);

		ReferenceEntry loadedParent = this.datastoreTemplate.findById(lazyParent.id, ReferenceEntry.class);
		assertThat(loadedParent.children).containsExactlyInAnyOrder(parent.children.toArray(new ReferenceEntry[0]));
	}


	@Test
	public void lazyReferenceTest() throws InterruptedException {
		LazyEntity lazyParentEntity = new LazyEntity(new LazyEntity(new LazyEntity()));
		this.datastoreTemplate.save(lazyParentEntity);

		LazyEntity loadedParent = this.datastoreTemplate.findById(lazyParentEntity.id, LazyEntity.class);

		//Saving an entity with not loaded lazy field
		this.datastoreTemplate.save(loadedParent);

		loadedParent = this.datastoreTemplate.findById(loadedParent.id, LazyEntity.class);
		assertThat(loadedParent).isEqualTo(lazyParentEntity);
	}


	@Test
	public void singularLazyPropertyTest() {
		LazyEntity lazyParentEntity = new LazyEntity(new LazyEntity(new LazyEntity()));
		this.datastoreTemplate.save(lazyParentEntity);

		LazyEntity loadedParent = this.datastoreTemplate.findById(lazyParentEntity.id, LazyEntity.class);
		assertThat(loadedParent).isEqualTo(lazyParentEntity);
	}

	@Test
	public void lazyReferenceTransactionTest() {
		ReferenceEntry parent = saveEntitiesGraph();

		//Exception should be produced if a lazy loaded property accessed outside of the initial transaction
		ReferenceEntry finalLoadedParent = this.transactionalTemplateService.findByIdLazy(parent.id);
		assertThatThrownBy(() -> finalLoadedParent.children.size()).isInstanceOf(DatastoreDataException.class)
				.hasMessage("Lazy load should be invoked within the same transaction");

		//No exception should be produced if a lazy loaded property accessed within the initial transaction
		ReferenceEntry finalLoadedParentLazyLoaded = this.transactionalTemplateService.findByIdLazyAndLoad(parent.id);
		assertThat(finalLoadedParentLazyLoaded).isEqualTo(parent);
	}

	private ReferenceEntry saveEntitiesGraph() {
		ReferenceEntry child1 = new ReferenceEntry("child1", null, null);
		ReferenceEntry child2 = new ReferenceEntry("child2", null, null);
		ReferenceEntry sibling = new ReferenceEntry("sibling", null, null);
		ReferenceEntry parent = new ReferenceEntry("parent", sibling, Arrays.asList(child1, child2));
		this.datastoreTemplate.save(parent);
		waitUntilTrue(() -> this.datastoreTemplate.findAll(ReferenceEntry.class).size() == 4);
		return parent;
	}

	@Test
	public void allocateIdTest() {
		// intentionally null ID value
		TestEntity testEntity = new TestEntity(null, "red", 1L, Shape.CIRCLE, null);
		assertThat(testEntity.getId()).isNull();
		this.testEntityRepository.save(testEntity);
		assertThat(testEntity.getId()).isNotNull();
		assertThat(this.testEntityRepository.findById(testEntity.getId())).isPresent();
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

	@Test
	public void recursiveSave() {
		SubEntity subEntity1 = new SubEntity();
		SubEntity subEntity2 = new SubEntity();
		SubEntity subEntity3 = new SubEntity();
		SubEntity subEntity4 = new SubEntity();

		subEntity1.stringList = Arrays.asList("a", "b");

		ParentEntity parentEntity = new ParentEntity(Arrays.asList(subEntity1, subEntity2),
				Collections.singletonList(subEntity4), subEntity3);
		subEntity1.parent = parentEntity;
		subEntity2.parent = parentEntity;
		subEntity3.parent = parentEntity;
		subEntity4.parent = parentEntity;

		subEntity1.sibling = subEntity2;
		subEntity2.sibling = subEntity1;

		subEntity3.sibling = subEntity4;
		subEntity4.sibling = subEntity3;

		this.datastoreTemplate.save(parentEntity);

		ParentEntity readParentEntity = this.datastoreTemplate.findById(parentEntity.id, ParentEntity.class);

		SubEntity readSubEntity1 = readParentEntity.subEntities.get(0);
		assertThat(readSubEntity1.parent).isSameAs(readParentEntity);
		assertThat(readSubEntity1.parent.subEntities.get(0)).isSameAs(readSubEntity1);

		SubEntity readSubEntity3 = readParentEntity.singularSubEntity;
		assertThat(readSubEntity3.parent).isSameAs(readParentEntity);
		assertThat(readSubEntity3.parent.singularSubEntity).isSameAs(readSubEntity3);

		SubEntity readSubEntity4 = readParentEntity.descendants.get(0);
		assertThat(readSubEntity4.parent).isSameAs(readParentEntity);
		assertThat(readSubEntity4.sibling).isSameAs(readSubEntity3);
		assertThat(readSubEntity3.sibling).isSameAs(readSubEntity4);

		Collection<SubEntity> allById = this.datastoreTemplate
				.findAllById(Arrays.asList(subEntity1.key, subEntity2.key),
						SubEntity.class);
		Iterator<SubEntity> iterator = allById.iterator();
		readSubEntity1 = iterator.next();
		SubEntity readSubEntity2 = iterator.next();
		assertThat(readSubEntity1.sibling).isSameAs(readSubEntity2);
		assertThat(readSubEntity2.sibling).isSameAs(readSubEntity1);
	}

	@Test
	public void nullPropertyTest() {
		SubEntity subEntity1 = new SubEntity();
		subEntity1.stringList = Arrays.asList("a", "b", null, "c");
		subEntity1.stringProperty = null;

		this.datastoreTemplate.save(subEntity1);

		SubEntity readEntity = this.datastoreTemplate.findById(subEntity1.key, SubEntity.class);

		assertThat(readEntity.stringProperty).isNull();
		assertThat(readEntity.stringList).containsExactlyInAnyOrder("a", "b", null, "c");
	}

	@Test
	public void inheritanceTest() {
		PetOwner petOwner = new PetOwner();
		petOwner.pets = Arrays.asList(
				new Cat("Alice"),
				new Cat("Bob"),
				new Pug("Bob"),
				new Dog("Bob"));

		this.datastoreTemplate.save(petOwner);

		PetOwner readPetOwner = this.datastoreTemplate.findById(petOwner.id, PetOwner.class);

		assertThat(readPetOwner.pets).hasSize(4);

		assertThat(readPetOwner.pets.stream().filter(pet -> "meow".equals(pet.speak()))).hasSize(2);
		assertThat(readPetOwner.pets.stream().filter(pet -> "woof".equals(pet.speak()))).hasSize(1);
		assertThat(readPetOwner.pets.stream().filter(pet -> "woof woof".equals(pet.speak()))).hasSize(1);

		waitUntilTrue(() -> this.datastoreTemplate.count(Pet.class) == 4);
		List<Pet> bobPets = this.petRepository.findByName("Bob");
		assertThat(bobPets.stream().map(Pet::speak)).containsExactlyInAnyOrder("meow", "woof", "woof woof");

		List<Dog> bobDogs = this.dogRepository.findByName("Bob");
		assertThat(bobDogs.stream().map(Pet::speak)).containsExactlyInAnyOrder("woof", "woof woof");

		assertThatThrownBy(() -> this.dogRepository.findByCustomQuery()).isInstanceOf(DatastoreDataException.class)
				.hasMessage("Can't append discrimination condition");
	}

	@Test
	public void inheritanceTestFindAll() {
		this.datastoreTemplate.saveAll(Arrays.asList(
				new Cat("Cat1"),
				new Dog("Dog1"),
				new Pug("Dog2")));

		waitUntilTrue(() -> this.datastoreTemplate.count(Pet.class) == 3);

		Collection<Dog> dogs = this.datastoreTemplate.findAll(Dog.class);

		assertThat(dogs).hasSize(2);

		Long dogCount = dogs.stream().filter(pet -> "woof".equals(pet.speak())).count();
		Long pugCount = dogs.stream().filter(pet -> "woof woof".equals(pet.speak())).count();

		assertThat(pugCount).isEqualTo(1);
		assertThat(dogCount).isEqualTo(1);
	}

	@Test
	public void enumKeys() {
		Map<CommunicationChannels, String> phone = new HashMap<>();
		phone.put(CommunicationChannels.SMS, "123456");

		Map<CommunicationChannels, String> email = new HashMap<>();
		phone.put(CommunicationChannels.EMAIL, "a@b.c");

		Event event1 = new Event("event1", phone);
		Event event2 = new Event("event2", email);

		this.datastoreTemplate.saveAll(Arrays.asList(event1, event2));

		waitUntilTrue(() -> this.datastoreTemplate.count(Event.class) == 2);

		Collection<Event> events = this.datastoreTemplate.findAll(Event.class);

		assertThat(events).containsExactlyInAnyOrder(event1, event2);
	}

	@Test
	public void mapSubclass() {
		CustomMap customMap1 = new CustomMap();
		customMap1.put("key1", "val1");
		ServiceConfiguration service1 = new ServiceConfiguration("service1", customMap1);
		CustomMap customMap2 = new CustomMap();
		customMap2.put("key2", "val2");
		ServiceConfiguration service2 = new ServiceConfiguration("service2", customMap2);

		this.datastoreTemplate.saveAll(Arrays.asList(service1, service2));

		waitUntilTrue(() -> this.datastoreTemplate.count(ServiceConfiguration.class) == 2);

		Collection<ServiceConfiguration> events = this.datastoreTemplate.findAll(ServiceConfiguration.class);

		assertThat(events).containsExactlyInAnyOrder(service1, service2);
	}

	@Test
	public void readOnlySaveTest() {
		this.expectedException.expect(TransactionSystemException.class);
		this.expectedException.expectMessage("Cloud Datastore transaction failed to commit.");
		this.transactionalTemplateService.writingInReadOnly();
	}

	@Test
	public void readOnlyDeleteTest() {
		this.expectedException.expect(TransactionSystemException.class);
		this.expectedException.expectMessage("Cloud Datastore transaction failed to commit.");
		this.transactionalTemplateService.deleteInReadOnly();
	}

	@Test
	public void readOnlyCountTest() {
		assertThat(this.transactionalTemplateService.findByIdInReadOnly(1)).isEqualTo(this.testEntityA);
	}

	@Test
	public void sameClassDescendantsTest() {
		Employee entity3 = new Employee(Collections.EMPTY_LIST);
		Employee entity2 = new Employee(Collections.EMPTY_LIST);
		Employee entity1 = new Employee(Arrays.asList(entity2, entity3));
		Company company = new Company(1L, Arrays.asList(entity1));
		this.datastoreTemplate.save(company);

		Company readCompany = this.datastoreTemplate.findById(company.id, Company.class);
		Employee child = readCompany.leaders.get(0);

		assertThat(child.id).isEqualTo(entity1.id);
		assertThat(child.subordinates).containsExactlyInAnyOrderElementsOf(entity1.subordinates);

		assertThat(readCompany.leaders).hasSize(1);
		assertThat(readCompany.leaders.get(0).id).isEqualTo(entity1.id);
	}

	@Test
	public void testSlicedEntityProjections() {
		reset(datastoreTemplate);
		Slice<TestEntityProjection> testEntityProjectionSlice =
				this.testEntityRepository.findBySize(2L, PageRequest.of(0, 1));

		List<TestEntityProjection> testEntityProjections =
				testEntityProjectionSlice.get().collect(Collectors.toList());

		assertThat(testEntityProjections).hasSize(1);
		assertThat(testEntityProjections.get(0)).isInstanceOf(TestEntityProjection.class);
		assertThat(testEntityProjections.get(0)).isNotInstanceOf(TestEntity.class);

		// Verifies that the projection method call works.
		assertThat(testEntityProjections.get(0).getColor()).isEqualTo("blue");

		ProjectionEntityQuery projectionQuery =
				com.google.cloud.datastore.Query.newProjectionEntityQueryBuilder()
						.addProjection("color")
						.setFilter(PropertyFilter.eq("size", 2L))
						.setKind("test_entities_ci").setLimit(1).build();

		verify(datastoreTemplate).queryKeysOrEntities(eq(projectionQuery), any());
	}

	@Test
	public void testPageableGqlEntityProjectionsPage() {
		Page<TestEntityProjection> page =
				this.testEntityRepository.getBySizePage(2L, PageRequest.of(0, 3));

		List<TestEntityProjection> testEntityProjections = page.get().collect(Collectors.toList());

		assertThat(testEntityProjections).hasSize(1);
		assertThat(testEntityProjections.get(0)).isInstanceOf(TestEntityProjection.class);
		assertThat(testEntityProjections.get(0)).isNotInstanceOf(TestEntity.class);
		assertThat(testEntityProjections.get(0).getColor()).isEqualTo("blue");
	}

	@Test
	public void testPageableGqlEntityProjectionsSlice() {
		Slice<TestEntityProjection> slice =
				this.testEntityRepository.getBySizeSlice(2L, PageRequest.of(0, 3));

		List<TestEntityProjection> testEntityProjections = slice.get().collect(Collectors.toList());

		assertThat(testEntityProjections).hasSize(1);
		assertThat(testEntityProjections.get(0)).isInstanceOf(TestEntityProjection.class);
		assertThat(testEntityProjections.get(0)).isNotInstanceOf(TestEntity.class);
		assertThat(testEntityProjections.get(0).getColor()).isEqualTo("blue");
	}

	@Test(timeout = 10000L)
	public void testSliceString() {
		try {
			Slice<String> slice = this.testEntityRepository.getSliceStringBySize(2L, PageRequest.of(0, 3));

			List<String> testEntityProjections = slice.get().collect(Collectors.toList());

			assertThat(testEntityProjections).hasSize(1);
			assertThat(testEntityProjections.get(0)).isEqualTo("blue");
		}
		catch (DatastoreException e) {
			if (e.getMessage().contains("no matching index found")) {
				throw new RuntimeException("The required index is not found. " +
						"The index could be created by running this command from 'resources' directory: " +
						"'gcloud datastore indexes create index.yaml'");
			}
			throw e;
		}
	}

	@Test(timeout = 10000L)
	public void testUnindex() {
		SubEntity childSubEntity = new SubEntity();
		childSubEntity.stringList = Collections.singletonList(generateString(1600));
		childSubEntity.stringProperty = generateString(1600);
		SubEntity parentSubEntity = new SubEntity();
		parentSubEntity.embeddedSubEntities = Collections.singletonList(childSubEntity);
		this.datastoreTemplate.save(parentSubEntity);
	}

	private String generateString(int length) {
		return IntStream.range(0, length).mapToObj(String::valueOf)
						.collect(Collectors.joining(","));
	}
}

/**
 * Test class.
 *
 * @author Dmitry Solomakha
 */
@Entity
class ParentEntity {
	@Id
	Long id;

	@Reference
	List<SubEntity> subEntities;

	@Reference
	SubEntity singularSubEntity;

	@Descendants
	List<SubEntity> descendants;

	ParentEntity(List<SubEntity> subEntities, List<SubEntity> descendants, SubEntity singularSubEntity) {
		this.subEntities = subEntities;
		this.singularSubEntity = singularSubEntity;
		this.descendants = descendants;
	}
}

/**
 * Test class.
 *
 * @author Dmitry Solomakha
 */
@Entity
class SubEntity {
	@Id
	Key key;

	@Reference
	ParentEntity parent;

	@Reference
	SubEntity sibling;

	@Unindexed
	List<String> stringList;

	String stringProperty;

	@Unindexed
	List<SubEntity> embeddedSubEntities;
}

class PetOwner {
	@Id
	Long id;

	@Reference
	Collection<Pet> pets;
}

@Entity
@DiscriminatorField(field = "pet_type")
abstract class Pet {
	@Id
	Long id;

	String name;

	Pet(String name) {
		this.name = name;
	}

	abstract String speak();
}

@DiscriminatorValue("cat")
class Cat extends Pet {

	Cat(String name) {
		super(name);
	}

	@Override
	String speak() {
		return "meow";
	}
}

@DiscriminatorValue("dog")
class Dog extends Pet {

	Dog(String name) {
		super(name);
	}

	@Override
	String speak() {
		return "woof";
	}
}

@DiscriminatorValue("pug")
class Pug extends Dog {

	Pug(String name) {
		super(name);
	}

	@Override
	String speak() {
		return "woof woof";
	}
}

interface PetRepository extends DatastoreRepository<Pet, Long> {
	List<Pet> findByName(String s);
}

interface DogRepository extends DatastoreRepository<Dog, Long> {
	List<Dog> findByName(String s);

	@Query("select * from Pet")
	List<Dog> findByCustomQuery();
}

@Entity
class Event {
	@Id
	private String eventName;

	private Map<CommunicationChannels, String> preferences;

	Event(String eventName, Map<CommunicationChannels, String> preferences) {
		this.eventName = eventName;
		this.preferences = preferences;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Event event = (Event) o;
		return Objects.equals(this.eventName, event.eventName) &&
				Objects.equals(this.preferences, event.preferences);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.eventName, this.preferences);
	}
}

@Entity
class Company {
	@Id
	Long id;

	@Descendants
	List<Employee> leaders;

	Company(Long id, List<Employee> leaders) {
		this.id = id;
		this.leaders = leaders;
	}
}

@Entity
class Employee {
	@Id
	public Key id;

	@Descendants
	public List<Employee> subordinates;

	Employee(List<Employee> subordinates) {
		this.subordinates = subordinates;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Employee that = (Employee) o;
		return Objects.equals(this.id, that.id) &&
				Objects.equals(this.subordinates, that.subordinates);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id, this.subordinates);
	}

	@Override
	public String toString() {
		return "Employee{" +
				"id=" + this.id.getNameOrId() +
				", subordinates="
				+ (this.subordinates != null
				? this.subordinates.stream()
				.map(employee -> employee.id.getNameOrId())
				.collect(Collectors.toList())
				: null)
				+
				'}';
	}
}

enum CommunicationChannels {
	EMAIL, SMS;
}
