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

package org.springframework.cloud.gcp.data.spanner.core;

import java.util.Collection;
import java.util.function.Function;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ReadOnlyTransaction;
import com.google.cloud.spanner.Statement;

import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerEntityProcessor;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;

/**
 * A {@link SpannerTemplate} that performs all operations in a single transaction. This
 * template is not intended for the user to directly instantiate.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
class ReadOnlyTransactionSpannerTemplate extends SpannerTemplate {

	private ReadOnlyTransaction readOnlyTransaction;

	ReadOnlyTransactionSpannerTemplate(DatabaseClient databaseClient,
			SpannerMappingContext mappingContext,
			SpannerEntityProcessor spannerEntityProcessor,
			SpannerMutationFactory spannerMutationFactory,
			SpannerSchemaUtils spannerSchemaUtils,
			ReadOnlyTransaction readOnlyTransaction) {
		super(databaseClient, mappingContext, spannerEntityProcessor,
				spannerMutationFactory, spannerSchemaUtils);
		this.readOnlyTransaction = readOnlyTransaction;
	}

	@Override
	protected void applyMutations(Collection<Mutation> mutations) {
		throw new SpannerDataException(
				"A read-only transaction template cannot perform mutations.");
	}

	@Override
	public long executeDmlStatement(Statement statement) {
		throw new SpannerDataException(
				"A read-only transaction template cannot execute DML.");
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
	public <T> T performReadWriteTransaction(Function<SpannerTemplate, T> operations) {
		throw new SpannerDataException(
				"A read-only transaction is already under execution. "
						+ "Opening sub-transactions is not supported!");
	}

	@Override
	public <T> T performReadOnlyTransaction(Function<SpannerTemplate, T> operations,
			SpannerReadOptions readOptions) {
		throw new SpannerDataException(
				"A read-only transaction is already under execution. "
						+ "Opening sub-transactions is not supported!");
	}
}
