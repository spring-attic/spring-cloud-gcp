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

package org.springframework.cloud.gcp.data.firestore.it;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.firestore.entities.User;
import org.springframework.cloud.gcp.data.firestore.entities.User.Address;
import org.springframework.cloud.gcp.data.firestore.entities.UserRepository;
import org.springframework.cloud.gcp.data.firestore.transaction.ReactiveFirestoreTransactionManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FirestoreIntegrationTestsConfiguration.class)
public class FirestoreRepositoryIntegrationTests {
	//tag::autowire[]
	@Autowired
	UserRepository userRepository;
	//end::autowire[]

	//tag::autowire_tx_manager[]
	@Autowired
	ReactiveFirestoreTransactionManager txManager;
	//end::autowire_tx_manager[]

	//tag::autowire_user_service[]
	@Autowired
	UserService userService;
	//end::autowire_user_service[]

	@Autowired
	ReactiveFirestoreTransactionManager transactionManager;

	@BeforeClass
	public static void checkToRun() {
		assumeThat("Firestore-sample tests are disabled. "
				+ "Please use '-Dit.firestore=true' to enable them. ",
				System.getProperty("it.firestore"), is("true"));
	}

	@Before
	public void cleanTestEnvironment() {
		this.userRepository.deleteAll().block();
		reset(this.transactionManager);
	}

	@Test
	public void countTest() {
		Flux<User> users = Flux.fromStream(IntStream.range(1, 10).boxed())
				.map(n -> new User("blah-person" + n, n));

		this.userRepository.saveAll(users).blockLast();

		long count = this.userRepository.countByAgeIsGreaterThan(5).block();
		assertThat(count).isEqualTo(4);
	}

	@Test
	//tag::repository_built_in[]
	public void writeReadDeleteTest() {
		List<User.Address> addresses = Arrays.asList(new User.Address("123 Alice st", "US"),
				new User.Address("1 Alice ave", "US"));
		User.Address homeAddress = new User.Address("10 Alice blvd", "UK");
		User alice = new User("Alice", 29, null, addresses, homeAddress);
		User bob = new User("Bob", 60);

		this.userRepository.save(alice).block();
		this.userRepository.save(bob).block();

		assertThat(this.userRepository.count().block()).isEqualTo(2);
		assertThat(this.userRepository.findAll().map(User::getName).collectList().block())
				.containsExactlyInAnyOrder("Alice", "Bob");

		User aliceLoaded = this.userRepository.findById("Alice").block();
		assertThat(aliceLoaded.getAddresses()).isEqualTo(addresses);
		assertThat(aliceLoaded.getHomeAddress()).isEqualTo(homeAddress);
	}
	//end::repository_built_in[]

	@Test
	public void transactionalOperatorTest() {
		//tag::repository_transactional_operator[]
		DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
		transactionDefinition.setReadOnly(false);
		TransactionalOperator operator = TransactionalOperator.create(this.txManager, transactionDefinition);
		//end::repository_transactional_operator[]

		//tag::repository_operations_in_a_transaction[]
		User alice = new User("Alice", 29);
		User bob = new User("Bob", 60);

		this.userRepository.save(alice)
				.then(this.userRepository.save(bob))
				.as(operator::transactional)
				.block();

		this.userRepository.findAll()
				.flatMap(a -> {
					a.setAge(a.getAge() - 1);
					return this.userRepository.save(a);
				})
				.as(operator::transactional).collectList().block();

		assertThat(this.userRepository.findAll().map(User::getAge).collectList().block())
				.containsExactlyInAnyOrder(28, 59);
		//end::repository_operations_in_a_transaction[]
	}

	@Test
	//tag::repository_part_tree[]
	public void partTreeRepositoryMethodTest() {
		User u1 = new User("Cloud", 22, null, null, new Address("1 First st., NYC", "USA"));
		u1.favoriteDrink = "tea";
		User u2 = new User("Squall", 17, null, null, new Address("2 Second st., London", "UK"));
		u2.favoriteDrink = "wine";
		Flux<User> users = Flux.fromArray(new User[] {u1, u2});

		this.userRepository.saveAll(users).blockLast();

		assertThat(this.userRepository.count().block()).isEqualTo(2);
		assertThat(this.userRepository.findByAge(22).collectList().block()).containsExactly(u1);
		assertThat(this.userRepository.findByHomeAddressCountry("USA").collectList().block()).containsExactly(u1);
		assertThat(this.userRepository.findByFavoriteDrink("wine").collectList().block()).containsExactly(u2);
		assertThat(this.userRepository.findByAgeGreaterThanAndAgeLessThan(20, 30).collectList().block())
				.containsExactly(u1);
		assertThat(this.userRepository.findByAgeGreaterThan(10).collectList().block()).containsExactlyInAnyOrder(u1,
				u2);
	}
	//end::repository_part_tree[]

	@Test
	public void pageableQueryTest() {
		Flux<User> users = Flux.fromStream(IntStream.range(1, 11).boxed())
				.map(n -> new User("blah-person" + n, n));
		this.userRepository.saveAll(users).blockLast();

		PageRequest pageRequest = PageRequest.of(2, 3, Sort.by(Order.desc("age")));
		List<String> pagedUsers = this.userRepository.findByAgeGreaterThan(0, pageRequest)
				.map(User::getName)
				.collectList()
				.block();

		assertThat(pagedUsers).containsExactlyInAnyOrder(
				"blah-person4", "blah-person3", "blah-person2");
	}

	@Test
	public void sortQueryTest() {
		Flux<User> users = Flux.fromStream(IntStream.range(1, 11).boxed())
				.map(n -> new User("blah-person" + n, n));
		this.userRepository.saveAll(users).blockLast();

		List<String> pagedUsers = this.userRepository
				.findByAgeGreaterThan(7, Sort.by(Order.asc("age")))
				.map(User::getName)
				.collectList()
				.block();

		assertThat(pagedUsers).containsExactlyInAnyOrder(
				"blah-person8", "blah-person9", "blah-person10");
	}

	@Test
	public void inFilterQueryTest() {
		User u1 = new User("Cloud", 22);
		User u2 = new User("Squall", 17);
		Flux<User> users = Flux.fromArray(new User[] {u1, u2});

		this.userRepository.saveAll(users).blockLast();

		List<String> pagedUsers = this.userRepository.findByAgeIn(Arrays.asList(22, 23, 24))
				.map(User::getName)
				.collectList()
				.block();

		assertThat(pagedUsers).containsExactly("Cloud");

		pagedUsers = this.userRepository.findByAgeIn(Arrays.asList(17, 22))
				.map(User::getName)
				.collectList()
				.block();

		assertThat(pagedUsers).containsExactly("Cloud", "Squall");

		pagedUsers = this.userRepository.findByAgeIn(Arrays.asList(18, 23))
				.map(User::getName)
				.collectList()
				.block();

		assertThat(pagedUsers).isEmpty();
	}

	@Test
	public void containsFilterQueryTest() {
		User u1 = new User("Cloud", 22, Arrays.asList("cat", "dog"));
		User u2 = new User("Squall", 17, Collections.singletonList("pony"));
		Flux<User> users = Flux.fromArray(new User[] {u1, u2});

		this.userRepository.saveAll(users).blockLast();

		List<String> pagedUsers = this.userRepository.findByPetsContains(Arrays.asList("cat", "dog"))
				.map(User::getName)
				.collectList()
				.block();

		assertThat(pagedUsers).containsExactly("Cloud");

		pagedUsers = this.userRepository.findByPetsContains(Arrays.asList("cat", "pony"))
				.map(User::getName)
				.collectList()
				.block();

		assertThat(pagedUsers).containsExactlyInAnyOrder("Cloud", "Squall");

		pagedUsers = this.userRepository.findByAgeAndPetsContains(17, Arrays.asList("cat", "pony"))
				.map(User::getName)
				.collectList()
				.block();

		assertThat(pagedUsers).containsExactlyInAnyOrder("Squall");

		pagedUsers = this.userRepository.findByPetsContainsAndAgeIn("cat", Arrays.asList(22, 23))
				.map(User::getName)
				.collectList()
				.block();

		assertThat(pagedUsers).containsExactlyInAnyOrder("Cloud");
	}

	@Test
	public void declarativeTransactionRollbackTest() {
		this.userService.deleteUsers().onErrorResume(throwable -> Mono.empty()).block();

		verify(this.transactionManager, times(0)).commit(any());
		verify(this.transactionManager, times(1)).rollback(any());
		verify(this.transactionManager, times(1)).getReactiveTransaction(any());
	}

	@Test
	public void declarativeTransactionCommitTest() {
		User alice = new User("Alice", 29);
		User bob = new User("Bob", 60);

		this.userRepository.save(alice).then(this.userRepository.save(bob)).block();

		this.userService.updateUsers().block();

		verify(this.transactionManager, times(1)).commit(any());
		verify(this.transactionManager, times(0)).rollback(any());
		verify(this.transactionManager, times(1)).getReactiveTransaction(any());

		assertThat(this.userRepository.findAll().map(User::getAge).collectList().block())
				.containsExactlyInAnyOrder(28, 59);
	}

	@Test
	public void transactionPropagationTest() {
		User alice = new User("Alice", 29);
		User bob = new User("Bob", 60);

		this.userRepository.save(alice).then(this.userRepository.save(bob)).block();

		this.userService.updateUsersTransactionPropagation().block();

		verify(this.transactionManager, times(1)).commit(any());
		verify(this.transactionManager, times(0)).rollback(any());
		verify(this.transactionManager, times(1)).getReactiveTransaction(any());

		assertThat(this.userRepository.findAll().map(User::getAge).collectList().block())
				.containsExactlyInAnyOrder(28, 59);
	}
}
