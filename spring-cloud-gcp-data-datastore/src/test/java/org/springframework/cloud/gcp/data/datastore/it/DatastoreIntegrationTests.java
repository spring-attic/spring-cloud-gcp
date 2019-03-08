/*
 * Copyright 2017-2019 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.DatastoreReaderWriter;
import com.google.cloud.datastore.Key;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Descendants;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DiscriminatorField;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DiscriminatorValue;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.cloud.gcp.data.datastore.it.TestEntity.Shape;
import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository;
import org.springframework.cloud.gcp.data.datastore.repository.query.Query;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
	private PetRepository petRepository;

	@Autowired
	private DogRepository dogRepository;

	@Autowired
	private DatastoreTemplate datastoreTemplate;

	@Autowired
	private TransactionalTemplateService transactionalTemplateService;

	@Autowired
	private DatastoreReaderWriter datastore;

	private Key keyForMap;
	private long millisWaited;

	private final TestEntity testEntityA = new TestEntity(1L, "red", 1L, Shape.CIRCLE, null);

	private final TestEntity testEntityB = new TestEntity(2L, "blue", 1L, Shape.CIRCLE, null);

	private final TestEntity testEntityC = new TestEntity(3L, "red", 1L, Shape.CIRCLE, null);

	private final TestEntity testEntityD = new TestEntity(4L, "red", 1L, Shape.SQUARE, null);

	private final List<TestEntity> allTestEntities;

	{
		this.allTestEntities = Arrays.asList(this.testEntityA, this.testEntityB, this.testEntityC, this.testEntityD);
	}

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
		this.testEntityRepository.deleteAll();
		if (this.keyForMap != null) {
			this.datastore.delete(this.keyForMap);
		}
	}

	@Before
	public void saveEntities() {
		this.testEntityRepository.saveAll(this.allTestEntities);

		this.millisWaited = waitUntilTrue(
				() -> this.testEntityRepository.countBySize(1L) == 4);

	}

	@Test
	public void testFindByExample() {
		assertThat(this.testEntityRepository
				.findAll(Example.of(new TestEntity(null, "red", null, Shape.CIRCLE, null))))
				.containsExactlyInAnyOrder(this.testEntityA, this.testEntityC);

		Page<TestEntity> result = this.testEntityRepository
				.findAll(
						Example.of(new TestEntity(null, null, null, null, null)),
						PageRequest.of(1, 2));
		assertThat(result.getTotalElements()).isEqualTo(4);
		assertThat(result.getNumberOfElements()).isEqualTo(2);
		assertThat(result.getTotalPages()).isEqualTo(2);

		assertThat(this.testEntityRepository
				.findAll(
						Example.of(new TestEntity(null, null, null, null, null)),
						Sort.by(Sort.Direction.ASC, "id")))
				.containsExactly(this.testEntityA, this.testEntityB, this.testEntityC, this.testEntityD);

		assertThat(this.testEntityRepository
				.count(Example.of(new TestEntity(null, "red", null, Shape.CIRCLE, null),
						ExampleMatcher.matching().withIgnorePaths("id", "size", "blobField"))))
				.isEqualTo(2);

		assertThat(this.testEntityRepository
				.exists(Example.of(new TestEntity(null, "red", null, Shape.CIRCLE, null),
						ExampleMatcher.matching().withIgnorePaths("id", "size", "blobField"))))
				.isEqualTo(true);

		assertThat(this.testEntityRepository
				.exists(Example.of(new TestEntity(null, "red", null, null, null),
						ExampleMatcher.matching().withIncludeNullValues())))
				.isEqualTo(false);

		assertThat(this.testEntityRepository
				.exists(Example.of(new TestEntity(null, "red", null, null, null))))
				.isEqualTo(true);
	}

	@Test
	public void testSaveAndDeleteRepository() throws InterruptedException {
		assertThat(this.testEntityRepository.findFirstByColor("blue")).contains(this.testEntityB);
		assertThat(this.testEntityRepository.findFirstByColor("green")).isNotPresent();
		assertThat(this.testEntityRepository.getByColor("green")).isNull();
		assertThat(this.testEntityRepository.getByColor("blue")).isEqualTo(this.testEntityB);

		assertThat(this.testEntityRepository.findByShape(Shape.SQUARE).stream()
				.map(TestEntity::getId).collect(Collectors.toList())).contains(4L);

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
				.map(TestEntity::getId).collect(Collectors.toList())).contains(4L);

		assertThat(this.testEntityRepository.deleteBySize(1L)).isEqualTo(4);

		this.testEntityRepository.saveAll(this.allTestEntities);

		this.millisWaited = Math.max(this.millisWaited,
				waitUntilTrue(() -> this.testEntityRepository.countBySize(1L) == 4));

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

		this.testEntityA.setBlobField(null);

		assertThat(this.testEntityRepository.getKey().getId()).isEqualTo((Long) 1L);
		assertThat(this.testEntityRepository.getIds(1L).length).isEqualTo(1);
		assertThat(this.testEntityRepository.getOneId(1L)).isEqualTo(1);
		assertThat(this.testEntityRepository.getOneTestEntity(1L)).isNotNull();

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
				waitUntilTrue(() -> this.testEntityRepository.countBySize(1L) == 4));

		this.testEntityRepository.deleteAll();

		try {
			this.transactionalTemplateService
					.testSaveInTransactionFailed(this.allTestEntities);
		}
		catch (Exception ignored) {
		}

		// we wait a period long enough that the previously attempted failed save would
		// show up if it is unexpectedly successful and committed.
		Thread.sleep(this.millisWaited * WAIT_FOR_EVENTUAL_CONSISTENCY_SAFETY_MULTIPLE);

		assertThat(this.testEntityRepository.count()).isEqualTo(0);

		assertThat(this.testEntityRepository.findAllById(Arrays.asList(1L, 2L)).iterator().hasNext()).isFalse();
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

		Collection<SubEntity> allById = this.datastoreTemplate.findAllById(Arrays.asList(subEntity1.key, subEntity2.key),
				SubEntity.class);
		Iterator<SubEntity> iterator = allById.iterator();
		readSubEntity1 = iterator.next();
		SubEntity readSubEntity2 = iterator.next();
		assertThat(readSubEntity1.sibling).isSameAs(readSubEntity2);
		assertThat(readSubEntity2.sibling).isSameAs(readSubEntity1);
	}

	private long waitUntilTrue(Supplier<Boolean> condition) {
		long startTime = System.currentTimeMillis();
		Awaitility.await().atMost(QUERY_WAIT_INTERVAL_SECONDS, TimeUnit.SECONDS).until(condition::get);

		return System.currentTimeMillis() - startTime;
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
