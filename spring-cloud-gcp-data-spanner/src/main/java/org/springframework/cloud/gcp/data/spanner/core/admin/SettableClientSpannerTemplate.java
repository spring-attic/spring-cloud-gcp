/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.spanner.core.admin;

import org.springframework.cloud.gcp.data.spanner.core.SpannerMutationFactory;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerEntityProcessor;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;

/**
 * This class is intended for users to override with a bean to configure per-method
 * param-based database clients. It cannot cannot execute real operations.
 *
 * @author Chengyuan Zhao
 */
public class SettableClientSpannerTemplate extends SpannerTemplate {

	public SettableClientSpannerTemplate() {
		this.databaseClientProvider = new CachingComposingDatabaseClientSupplier(() -> 0, x -> null);
	}

	/**
	 * Primary constructor.
	 * @param databaseClientProvider a supplier of database clients. This is used to supply
	 *     each database client for a new operation or new transaction.
	 * @param mappingContext the Spring Data mapping context used to provide entity metadata.
	 * @param spannerEntityProcessor the entity processor for working with Cloud Spanner row
	 *     objects
	 * @param spannerMutationFactory the mutation factory for generating write operations.
	 * @param spannerSchemaUtils the utility class for working with table and entity schemas.
	 */
	public SettableClientSpannerTemplate(CachingComposingDatabaseClientSupplier databaseClientProvider,
			SpannerMappingContext mappingContext,
			SpannerEntityProcessor spannerEntityProcessor,
			SpannerMutationFactory spannerMutationFactory,
			SpannerSchemaUtils spannerSchemaUtils) {
		super(databaseClientProvider, mappingContext, spannerEntityProcessor, spannerMutationFactory,
				spannerSchemaUtils);
	}

	/**
	 * Get the database client provider this template will use for operations and queries.
	 * @return the database client provider that will be used.
	 */
	public CachingComposingDatabaseClientSupplier getDatabaseClientProvider() {
		return this.databaseClientProvider;
	}
}
