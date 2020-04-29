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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.firestore.FirestoreDataException;
import org.springframework.cloud.gcp.data.firestore.entities.User;
import org.springframework.cloud.gcp.data.firestore.entities.UserRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Dmitry Solomakha
 */

//tag::user_service[]
class UserService {
	@Autowired
	private UserRepository userRepository;

	//end::user_service[]
	@Transactional
	public Mono<Void> updateUsersTransactionPropagation() {
		return findAll()
				.flatMap(a -> {
					a.setAge(a.getAge() - 1);
					return this.userRepository.save(a);
				})
				.then();
	}

	@Transactional
	private Flux<User> findAll() {
		return this.userRepository.findAll();
	}

	@Transactional
	public Mono<Void> deleteUsers() {
		return this.userRepository.saveAll(Mono.defer(() -> {
			throw new FirestoreDataException("BOOM!");
		})).then(this.userRepository.deleteAll());
	}

	// tag::user_service[]
	@Transactional
	public Mono<Void> updateUsers() {
		return this.userRepository.findAll()
				.flatMap(a -> {
					a.setAge(a.getAge() - 1);
					return this.userRepository.save(a);
				})
				.then();
	}
}
//end::user_service[]
