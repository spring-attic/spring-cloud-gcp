/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.data.spanner.repository;

import java.util.function.Function;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author Ray Tsang
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public interface SpannerRepository<T, ID> extends PagingAndSortingRepository<T, ID> {

	/**
	 * Gets a {@link SpannerOperations}, which allows more-direct access to Google Cloud Spanner
	 * functions.
	 * @return the operations object providing Cloud Spanner functions.
	 */
	SpannerOperations getSpannerTemplate();

	/**
	 * Performs multiple read and write operations in a single transaction.
	 * @param operations the function representing the operations to perform using a
	 * SpannerRepository based on a single transaction.
	 * @param <A> the final return type of the operations.
	 * @return the final result of the transaction.
	 */
	<A> A performReadWriteTransaction(Function<SpannerRepository<T, ID>, A> operations);

	/**
	 * Performs multiple read-only operations in a single transaction.
	 * @param operations the function representing the operations to perform using a
	 * SpannerRepository based on a single transaction.
	 * @param <A> the final return type of the operations.
	 * @return the final result of the transaction.
	 */
	<A> A performReadOnlyTransaction(Function<SpannerRepository<T, ID>, A> operations);
}
