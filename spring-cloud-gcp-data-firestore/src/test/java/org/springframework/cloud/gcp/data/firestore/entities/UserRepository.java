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

package org.springframework.cloud.gcp.data.firestore.entities;

import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gcp.data.firestore.FirestoreReactiveRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * A test custom repository.
 *
 * @author Chengyuan Zhao
 */
//tag::repository[]
public interface UserRepository extends FirestoreReactiveRepository<User> {
	Flux<User> findByAge(Integer age);

	Flux<User> findByHomeAddressCountry(String country);

	Flux<User> findByFavoriteDrink(String drink);

	Flux<User> findByAgeGreaterThanAndAgeLessThan(Integer age1, Integer age2);

	Flux<User> findByAgeGreaterThan(Integer age);

	Flux<User> findByAgeGreaterThan(Integer age, Sort sort);

	Flux<User> findByAgeGreaterThan(Integer age, Pageable pageable);

	Flux<User> findByAgeIn(List<Integer> ages);

	Flux<User> findByAgeAndPetsContains(Integer age, List<String> pets);

	Flux<User> findByPetsContains(List<String> pets);

	Flux<User> findByPetsContainsAndAgeIn(String pets, List<Integer> ages);

	Mono<Long> countByAgeIsGreaterThan(Integer age);
}
//end::repository[]
