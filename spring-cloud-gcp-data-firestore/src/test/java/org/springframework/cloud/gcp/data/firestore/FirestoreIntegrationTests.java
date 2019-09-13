/*
 * Copyright 2019-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.firestore;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import ch.qos.logback.classic.Level;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firestore.v1.FirestoreGrpc;
import io.grpc.CallCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.auth.MoreCallCredentials;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreMappingContext;
import org.springframework.cloud.gcp.data.firestore.repository.config.EnableReactiveFirestoreRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * @author Dmitry Solomakha
 * @author Chengyuan Zhao
 * @author Daniel Zou
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
public class FirestoreIntegrationTests {

	private static final String DEFAULT_PARENT = "projects/spring-cloud-gcp-ci-firestore/databases/(default)/documents";

	@Autowired
	FirestoreTemplate firestoreTemplate;

	@Autowired
	UserRepository userRepository;

	@BeforeClass
	public static void checkToRun() throws IOException {
		assumeThat(
				"Firestore-sample tests are disabled. Please use '-Dit.firestore=true' "
						+ "to enable them. ",
				System.getProperty("it.firestore"), is("true"));

		ch.qos.logback.classic.Logger root =
				(ch.qos.logback.classic.Logger)
						LoggerFactory.getLogger("io.grpc.netty");
		root.setLevel(Level.INFO);
	}

	@Before
	public void cleanTestEnvironment() {
		this.firestoreTemplate.deleteAll(User.class).block();
	}

	@Test
	public void writeReadDeleteTest() {
		User alice = new User("Alice", 29);
		User bob = new User("Bob", 60);

		this.firestoreTemplate.save(alice).block();
		this.firestoreTemplate.save(bob).block();

		assertThat(this.firestoreTemplate.findById(Mono.just("Saitama"), User.class).block()).isNull();

		assertThat(this.firestoreTemplate.findById(Mono.just("Bob"), User.class).block()).isEqualTo(bob);
		assertThat(this.firestoreTemplate.findAllById(Flux.just("Bob", "Saitama", "Alice"), User.class).collectList()
				.block())
				.containsExactlyInAnyOrder(bob, alice);

		List<User> usersBeforeDelete = this.firestoreTemplate.findAll(User.class).collectList().block();

		assertThat(this.firestoreTemplate.count(User.class).block()).isEqualTo(2);
		this.firestoreTemplate.delete(Mono.just(bob)).block();
		assertThat(this.firestoreTemplate.count(User.class).block()).isEqualTo(1);
		assertThat(this.firestoreTemplate.existsById(Mono.just("Alice"), User.class).block()).isEqualTo(Boolean.TRUE);
		assertThat(this.firestoreTemplate.existsById(Mono.just("Bob"), User.class).block()).isEqualTo(Boolean.FALSE);
		this.firestoreTemplate.deleteById(Mono.just("Alice"), User.class).block();
		assertThat(this.firestoreTemplate.count(User.class).block()).isEqualTo(0);

		this.firestoreTemplate.save(alice).block();
		this.firestoreTemplate.save(bob).block();

		assertThat(this.userRepository.count().block()).isEqualTo(2);
		assertThat(this.firestoreTemplate.deleteAll(User.class).block()).isEqualTo(2);

		assertThat(usersBeforeDelete).containsExactlyInAnyOrder(alice, bob);
		assertThat(this.firestoreTemplate.findAll(User.class).collectList().block()).isEmpty();
	}


	@Test
	public void saveTest() throws InterruptedException {
		assertThat(this.firestoreTemplate.count(User.class).block()).isEqualTo(0);

		User u1 = new User("Cloud", 22);
		this.firestoreTemplate.save(u1).block();

		assertThat(this.firestoreTemplate.findAll(User.class).collectList().block())
				.containsExactly(u1);
	}

	@Test
	public void saveAllTest() throws InterruptedException {
		User u1 = new User("Cloud", 22);
		User u2 = new User("Squall", 17);
		Flux<User> users = Flux.fromArray(new User[]{u1, u2});

		assertThat(this.firestoreTemplate.count(User.class).block()).isEqualTo(0);

		this.firestoreTemplate.saveAll(users).blockLast();

		assertThat(this.firestoreTemplate.count(User.class).block()).isEqualTo(2);

		assertThat(this.userRepository.findByAge(22).collectList().block()).containsExactly(u1);
		assertThat(this.userRepository.findByAgeAndName(22, "Cloud").collectList().block()).containsExactly(u1);
		assertThat(this.userRepository.findByAgeGreaterThan(10).collectList().block()).containsExactlyInAnyOrder(u1, u2);

		assertThat(this.firestoreTemplate.deleteAll(User.class).block()).isEqualTo(2);
		assertThat(this.userRepository.existsById("Cloud").block()).isFalse();
	}

	@Test
	public void saveAllBulkTest() throws InterruptedException {
		Flux<User> users = Flux.create(sink -> {
			for (int i = 0; i < 1000; i++) {
				sink.next(new User("testUser " + i, i));
			}
			sink.complete();
		});

		assertThat(this.firestoreTemplate.findAll(User.class).collectList().block()).isEmpty();

		this.firestoreTemplate.saveAll(users).blockLast();

		assertThat(this.firestoreTemplate.findAll(User.class).count().block()).isEqualTo(1000);
	}

	@Test
	public void deleteTest() throws InterruptedException {
		this.firestoreTemplate.save(new User("alpha", 45)).block();
		this.firestoreTemplate.save(new User("beta", 23)).block();
		this.firestoreTemplate.save(new User("gamma", 44)).block();
		this.firestoreTemplate.save(new User("Joe Hogan", 22)).block();

		assertThat(this.firestoreTemplate.count(User.class).block()).isEqualTo(4);

		this.firestoreTemplate.delete(Mono.just(new User("alpha", 45))).block();
		assertThat(this.firestoreTemplate.findAll(User.class).map(User::getName).collectList().block())
				.containsExactlyInAnyOrder("beta", "gamma", "Joe Hogan");

		this.firestoreTemplate.deleteById(Mono.just("beta"), User.class).block();
		assertThat(this.firestoreTemplate.findAll(User.class).map(User::getName).collectList().block())
				.containsExactlyInAnyOrder("gamma", "Joe Hogan");

		this.firestoreTemplate.deleteAll(User.class).block();
		assertThat(this.firestoreTemplate.count(User.class).block()).isEqualTo(0);
	}

	/**
	 * Spring config for the tests.
	 */
	@Configuration
	@EnableReactiveFirestoreRepositories
	static class Config {

		@Bean
		public FirestoreTemplate firestoreTemplate() throws IOException {

			GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
			CallCredentials callCredentials = MoreCallCredentials.from(credentials);

			// Create a channel
			ManagedChannel channel = ManagedChannelBuilder
					.forAddress("firestore.googleapis.com", 443)
					.build();

			return new FirestoreTemplate(FirestoreGrpc.newStub(channel).withCallCredentials(callCredentials),
					DEFAULT_PARENT);
		}

		@Bean
		public FirestoreMappingContext firestoreMappingContext() {
			return new FirestoreMappingContext();
		}
	}

	@Entity(collectionName = "usersCollection")
	public static class User {
		@Id
		private String name;

		private Integer age;

		User(String name, Integer age) {
			this.name = name;
			this.age = age;
		}

		User() {
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getAge() {
			return this.age;
		}

		public void setAge(Integer age) {
			this.age = age;
		}

		@Override
		public String toString() {
			return "User{" +
					"name='" + this.name + '\'' +
					", age=" + this.age +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			User user = (User) o;
			return Objects.equals(getName(), user.getName()) &&
					Objects.equals(getAge(), user.getAge());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getName(), getAge());
		}
	}
}
