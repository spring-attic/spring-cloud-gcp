/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.data.datastore.repository;

import java.util.function.Function;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

/**
 * A {@link PagingAndSortingRepository} that provides Datastore-specific functionality.
 *
 * @param <T> the type of the domain object
 * @param <ID> the type of the ID property in the domain object
 * @author Chengyuan Zhao
 * @since 1.1
 */
public interface DatastoreRepository<T, ID>
		extends PagingAndSortingRepository<T, ID>, QueryByExampleExecutor<T> {

	/**
	 * Performs multiple read and write operations in a single transaction.
	 * @param operations the function representing the operations to perform using a
	 * DatastoreRepository based on a single transaction.
	 * @param <A> the final return type of the operations.
	 * @return the final result of the transaction.
	 */
	<A> A performTransaction(Function<DatastoreRepository<T, ID>, A> operations);

}
