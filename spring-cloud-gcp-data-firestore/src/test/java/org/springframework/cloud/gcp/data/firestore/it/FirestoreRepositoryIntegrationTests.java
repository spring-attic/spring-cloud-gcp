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

import ch.qos.logback.classic.Level;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.firestore.FirestoreTemplate;
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
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.grpc.netty");
		root.setLevel(Level.INFO);
	}

	@Before
	public void cleanTestEnvironment() {
		this.firestoreTemplate.deleteAll(User.class).block();
	}

	@Test
	public void countTest() {
		Flux<User> users = Flux.create(sink -> {
			for (int i = 1; i < 10; i++) {
				sink.next(new User("blah-person" + i, i));
			}
			sink.complete();
		});

		this.firestoreTemplate.saveAll(users).blockLast();

		long count = this.userRepository.countByAgeIsGreaterThan(5).block();
		assertThat(count).isEqualTo(4);
	}
}
