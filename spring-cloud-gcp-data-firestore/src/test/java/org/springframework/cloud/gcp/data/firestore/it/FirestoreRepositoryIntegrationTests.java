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

import java.io.IOException;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.firestore.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FirestoreIntegrationTestsConfiguration.class)
public class FirestoreRepositoryIntegrationTests {

	@Autowired
	UserRepository userRepository;

	@BeforeClass
	public static void checkToRun() throws IOException {
		assumeThat("Firestore-sample tests are disabled. "
				+ "Please use '-Dit.firestore=true' to enable them. ",
				System.getProperty("it.firestore"), is("true"));
	}

	@Before
	public void cleanTestEnvironment() {
		this.userRepository.deleteAll().block();
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
	public void writeReadDeleteTest() {
		User alice = new User("Alice", 29);
		User bob = new User("Bob", 60);

		this.userRepository.save(alice).block();
		this.userRepository.save(bob).block();

		assertThat(this.userRepository.count().block()).isEqualTo(2);
		assertThat(this.userRepository.findAll().map(User::getName).collectList().block())
				.containsExactlyInAnyOrder("Alice", "Bob");
	}

	@Test
	public void partTreeRepositoryMethodTest() {
		User u1 = new User("Cloud", 22);
		User u2 = new User("Squall", 17);
		Flux<User> users = Flux.fromArray(new User[] { u1, u2 });

		this.userRepository.saveAll(users).blockLast();

		assertThat(this.userRepository.count().block()).isEqualTo(2);
		assertThat(this.userRepository.findByAge(22).collectList().block()).containsExactly(u1);
		assertThat(this.userRepository.findByAgeAndName(22, "Cloud").collectList().block()).containsExactly(u1);
		assertThat(this.userRepository.findByAgeGreaterThan(10).collectList().block()).containsExactlyInAnyOrder(u1,
				u2);
	}
}
