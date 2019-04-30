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

package com.example;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author Dmitry Solomakha
 *
 * @since 1.2
 */
@SpringBootApplication
public class FirestoreSampleApp {
	@Autowired
	Firestore firestore;

	public static void main(String[] args) {
		SpringApplication.run(FirestoreSampleApp.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner() {
		return args -> {
			writeDocumentFromMap();
			writeDocumentFromObject();
			readDocumentToMap();
			readDocumentToObject();
			removeDocuments();

		};
	}

	private void writeDocumentFromMap() throws InterruptedException, java.util.concurrent.ExecutionException {
		DocumentReference docRef = this.firestore.collection("users").document("ada");
		// Add document data with id "ada" using a hashmap
		Map<String, Object> data = new HashMap<>();
		data.put("name", "Ada");
		data.put("phones", Arrays.asList(123, 456));

		// asynchronously write data
		ApiFuture<WriteResult> result = docRef.set(data);

		// result.get() blocks on response
		System.out.println("Update time: " + result.get().getUpdateTime());
	}

	private void writeDocumentFromObject() throws ExecutionException, InterruptedException {
		// Add document data with id "joe" using a custom User class
		User data = new User("Joe",
				Arrays.asList(new Phone(12345, PhoneType.CELL), new Phone(54321, PhoneType.WORK)));

		// .get() blocks on response
		WriteResult writeResult = this.firestore.document("users/joe").set(data).get();

		System.out.println("Update time: " + writeResult.getUpdateTime());
	}

	private void readDocumentToMap() throws ExecutionException, InterruptedException {
		DocumentReference docRef = this.firestore.document("users/ada");

		ApiFuture<DocumentSnapshot> documentSnapshotApiFuture = docRef.get();

		DocumentSnapshot document = documentSnapshotApiFuture.get();

		System.out.println("read: " + document.getData());
	}

	private void readDocumentToObject() throws ExecutionException, InterruptedException {
		ApiFuture<DocumentSnapshot> documentSnapshotApiFuture = this.firestore.document("users/joe").get();

		User user = documentSnapshotApiFuture.get().toObject(User.class);

		System.out.println("read: " + user);
	}

	private void removeDocuments() {
		//Warning: Deleting a document does not delete its subcollections!
		//
		//If you want to delete documents in subcollections when deleting a document, you must do so manually.
		//See https://firebase.google.com/docs/firestore/manage-data/delete-data#collections
		CollectionReference users = this.firestore.collection("users");
		Iterable<DocumentReference> documentReferences = users.listDocuments();
		documentReferences.forEach(documentReference -> {
			System.out.println("removing: " + documentReference.getId());
			try {
				documentReference.delete().get();
			}
			catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		});
	}
}

