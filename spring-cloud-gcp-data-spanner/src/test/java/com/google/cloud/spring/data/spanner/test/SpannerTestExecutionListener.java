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

package com.google.cloud.spring.data.spanner.test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.google.cloud.spring.data.spanner.core.admin.SpannerDatabaseAdminTemplate;
import com.google.cloud.spring.data.spanner.core.admin.SpannerSchemaUtils;
import com.google.cloud.spring.data.spanner.test.domain.CommitTimestamps;
import com.google.cloud.spring.data.spanner.test.domain.Trade;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

public class SpannerTestExecutionListener implements TestExecutionListener {

	private static final Log LOGGER = LogFactory.getLog(SpannerTestExecutionListener.class);

	private SpannerDatabaseAdminTemplate spannerDatabaseAdminTemplate;

	private SpannerSchemaUtils spannerSchemaUtils;

	@Override
	public void beforeTestClass(TestContext testContext) throws Exception {
		// This runs before the JUnit @BeforeClass method, so this property needs to be checked here, too.
		if (!"true".equalsIgnoreCase(System.getProperty("it.spanner"))) {
			return;
		}

		spannerDatabaseAdminTemplate = testContext.getApplicationContext().getBean(SpannerDatabaseAdminTemplate.class);
		spannerSchemaUtils = testContext.getApplicationContext().getBean(SpannerSchemaUtils.class);
		createDatabaseWithSchema();
	}
	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
		if (!"true".equalsIgnoreCase(System.getProperty("it.spanner"))) {
			return;
		}

		SpannerDatabaseAdminTemplate spannerDatabaseAdminTemplate =
				testContext.getApplicationContext().getBean(SpannerDatabaseAdminTemplate.class);
		SpannerSchemaUtils spannerSchemaUtils =
				testContext.getApplicationContext().getBean(SpannerSchemaUtils.class);

		List<String> dropSchemaStatements =
				spannerSchemaUtils.getDropTableDdlStringsForInterleavedHierarchy(Trade.class);
		dropSchemaStatements.add(spannerSchemaUtils.getDropTableDdlString(CommitTimestamps.class));

		spannerDatabaseAdminTemplate.executeDdlStrings(dropSchemaStatements, false);
	}


	protected void createDatabaseWithSchema() {
		List<String> createStatements = createSchemaStatements();

		if (!this.spannerDatabaseAdminTemplate.databaseExists()) {
			LOGGER.debug(
					this.getClass() + " - Integration database created with schema: "
							+ createStatements);
			this.spannerDatabaseAdminTemplate.executeDdlStrings(createStatements, true);
		}
		else {
			LOGGER.debug(
					this.getClass() + " - schema created: " + createStatements);
			this.spannerDatabaseAdminTemplate.executeDdlStrings(createStatements, false);
		}

		await().pollDelay(Duration.ofSeconds(5))
				.pollInterval(Duration.ofSeconds(2))
				.atMost(Duration.ofMinutes(1))
				.untilAsserted(() -> assertThat(this.spannerDatabaseAdminTemplate.databaseExists()).isTrue());
	}

	protected List<String> createSchemaStatements() {
		List<String> list = new ArrayList<>(this.spannerSchemaUtils
				.getCreateTableDdlStringsForInterleavedHierarchy(Trade.class));
		list.add(this.spannerSchemaUtils
				.getCreateTableDdlString(CommitTimestamps.class)
				.replaceAll("TIMESTAMP", "TIMESTAMP OPTIONS (allow_commit_timestamp = true)"));
		return list;
	}
}
