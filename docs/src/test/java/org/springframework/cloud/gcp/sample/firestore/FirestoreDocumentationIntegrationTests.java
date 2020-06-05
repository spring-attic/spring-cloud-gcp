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

package org.springframework.cloud.gcp.sample.firestore;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.firestore.GcpFirestoreAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * @author Dmitry Solomakha
 *
 * @since 1.2
 */
public class FirestoreDocumentationIntegrationTests {
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GcpContextAutoConfiguration.class,
					GcpFirestoreAutoConfiguration.class));

	@BeforeClass
	public static void enableTests() {
		assumeThat(System.getProperty("it.firestore")).isEqualTo("true");
	}

	@Test
	public void writeDocumentFromObject() throws ExecutionException, InterruptedException {
		this.contextRunner.run((context) -> {
			Firestore firestore = context.getBean(Firestore.class);
			FirestoreExample firestoreExample = new FirestoreExample(firestore);

			firestoreExample.writeDocumentFromObject();
			User user = firestoreExample.readDocumentToObject();
			assertThat(user).isEqualTo(new User("Joe",
					Arrays.asList(
							new Phone(12345, PhoneType.CELL),
							new Phone(54321, PhoneType.WORK))));

			firestoreExample.writeDocumentFromObject2();
			user = firestoreExample.readDocumentToObject();
			assertThat(user).isEqualTo(new User("Joseph",
					Arrays.asList(
							new Phone(23456, PhoneType.CELL),
							new Phone(65432, PhoneType.WORK))));
		});
	}

}

class FirestoreExample {
	private Firestore firestore;

	private static final Log LOGGER = LogFactory.getLog(FirestoreExample.class);

	FirestoreExample(Firestore firestore) {
		this.firestore = firestore;
	}

	void writeDocumentFromObject2() throws ExecutionException, InterruptedException {
		// Add document data with id "joe" using a custom User class
		User data = new User("Joseph",
				Arrays.asList(
						new Phone(23456, PhoneType.CELL),
						new Phone(65432, PhoneType.WORK)));

		// .get() blocks on response
		WriteResult writeResult = this.firestore.document("users/joe").set(data).get();

		LOGGER.info("Update time: " + writeResult.getUpdateTime());
	}

	//tag::write[]
	void writeDocumentFromObject() throws ExecutionException, InterruptedException {
		// Add document data with id "joe" using a custom User class
		User data = new User("Joe",
				Arrays.asList(
						new Phone(12345, PhoneType.CELL),
						new Phone(54321, PhoneType.WORK)));

		// .get() blocks on response
		WriteResult writeResult = this.firestore.document("users/joe").set(data).get();

		LOGGER.info("Update time: " + writeResult.getUpdateTime());
	}
	//end::write[]

	//tag::read[]
	User readDocumentToObject() throws ExecutionException, InterruptedException {
			ApiFuture<DocumentSnapshot> documentFuture =
					this.firestore.document("users/joe").get();

			User user = documentFuture.get().toObject(User.class);

			LOGGER.info("read: " + user);

			return user;
	}
	//end::read[]
}
