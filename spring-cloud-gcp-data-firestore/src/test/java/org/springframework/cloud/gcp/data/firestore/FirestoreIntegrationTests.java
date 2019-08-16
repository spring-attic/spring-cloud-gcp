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

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.WriteBatch;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ch.qos.logback.classic.Level;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firestore.v1.FirestoreGrpc;
import io.grpc.CallCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.auth.MoreCallCredentials;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.data.annotation.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * @author Dmitry Solomakha
 */
public class FirestoreIntegrationTests {
	private static final String DEFAULT_PARENT = "projects/cowzow/databases/(default)/documents";



	private FirestoreGrpc.FirestoreStub firestoreStub;

	private FirestoreTemplate firestoreTemplate;


	@Before
	public void setup() throws IOException {

		ch.qos.logback.classic.Logger root =
				(ch.qos.logback.classic.Logger)
						LoggerFactory.getLogger("io.grpc.netty");
		root.setLevel(Level.INFO);

		GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
		CallCredentials callCredentials = MoreCallCredentials.from(credentials);

		// Create a channel
		ManagedChannel channel = ManagedChannelBuilder
				.forAddress("firestore.googleapis.com", 443)
				.build();

		this.firestoreStub = FirestoreGrpc.newStub(channel).withCallCredentials(callCredentials);
		this.firestoreTemplate = new FirestoreTemplate(this.firestoreStub, DEFAULT_PARENT);

		this.firestoreTemplate.deleteAll(User.class).block();

	}

	@Test
	public void writeReadDeleteTest() {
		User alice = new User("Alice", 29);
		User bob = new User("Bob", 60);

		this.firestoreTemplate.save(alice).block();
		this.firestoreTemplate.save(bob).block();

		List<User> usersBeforeDelete = this.firestoreTemplate.findAll(User.class).collectList().block();

		assertThat(this.firestoreTemplate.deleteAll(User.class).block()).isEqualTo(2);

		assertThat(usersBeforeDelete).containsExactlyInAnyOrder(alice, bob);
		assertThat(this.firestoreTemplate.findAll(User.class).collectList().block()).isEmpty();
	}

	@Test
	public void saveAllTest() throws InterruptedException {
		User u1 = new User("Cloud", 22);
		User u2 = new User("Squall", 17);
		Flux<User> users = Flux.fromArray(new User[] { u1, u2 });

		assertThat(this.firestoreTemplate.findAll(User.class).collectList().block()).isEmpty();

		this.firestoreTemplate.saveAll(users).collectList().block();

		assertThat(this.firestoreTemplate.findAll(User.class).collectList().block().size()).isEqualTo(2);
	}

	@Test
	public void saveAllBulkTestWithReactor() throws InterruptedException {

		Flux<User> users = Flux.create(sink -> {
			for (int i = 0; i < 300; i++) {
				sink.next(new User("testUserX " + i, i));
			}
			sink.complete();
		});

		assertThat(this.firestoreTemplate.findAll(User.class).collectList().block()).isEmpty();

		this.firestoreTemplate.saveAll(users).blockLast();

		assertThat(this.firestoreTemplate.findAll(User.class).collectList().block().size()).isEqualTo(100);
	}

	@Test
	public void saveBulkWithClientLibraries() throws ExecutionException, InterruptedException {

		FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance();
		Firestore db = firestoreOptions.getService();

		WriteBatch batch = db.batch();

		for (int i = 0; i < 300; i++) {
			DocumentReference ref = db.collection("example_test").document("test user " + i);
			batch.set(ref, new User("blargatron", i));
		}

		batch.commit().get();
	}
}


@Entity(collectionName = "usersFirestoreTemplate")
class User {
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
