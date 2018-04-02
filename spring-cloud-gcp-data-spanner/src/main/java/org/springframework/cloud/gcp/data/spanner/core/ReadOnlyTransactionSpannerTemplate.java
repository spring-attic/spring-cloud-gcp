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

package org.springframework.cloud.gcp.data.spanner.core;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ReadOnlyTransaction;

import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;

/**
 * A {@link SpannerTemplate} that performs all operations in a single transaction.
 * This template is not intended for the user to directly instantiate.
 * @author Chengyuan Zhao
 */
class ReadOnlyTransactionSpannerTemplate extends SpannerTemplate {

	private ReadOnlyTransaction readOnlyTransaction;

	ReadOnlyTransactionSpannerTemplate(DatabaseClient databaseClient,
			SpannerMappingContext mappingContext, SpannerConverter spannerConverter,
			SpannerMutationFactory spannerMutationFactory,
			ReadOnlyTransaction readOnlyTransaction) {
		super(databaseClient, mappingContext, spannerConverter, spannerMutationFactory);
		this.readOnlyTransaction = readOnlyTransaction;
	}

	@Override
	protected <T, U> void applyMutationTwoArgs(BiFunction<T, U, Mutation> function,
			T arg1, U arg2) {
		throw new SpannerDataException(
				"A read-only transaction template cannot perform mutations.");
	}

	@Override
	protected ReadContext getReadContext() {
		return this.readOnlyTransaction;
	}

	@Override
	protected ReadContext getReadContext(Timestamp timestamp) {
		throw new SpannerDataException(
				"Getting stale snapshot read contexts is not supported"
						+ " in read-only transaction templates.");
	}

	@Override
	public <T> T performReadWriteTransaction(Function<SpannerOperations, T> operations) {
		throw new SpannerDataException("A read-only transaction is already under execution. "
				+ "Opening sub-transactions is not supported!");
	}

	@Override
	public <T> T performReadOnlyTransaction(Function<SpannerOperations, T> operations, SpannerReadOptions readOptions) {
		throw new SpannerDataException("A read-only transaction is already under execution. "
				+ "Opening sub-transactions is not supported!");
	}
}
