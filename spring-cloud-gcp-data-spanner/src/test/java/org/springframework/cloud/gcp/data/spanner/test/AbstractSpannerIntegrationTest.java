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

package org.springframework.cloud.gcp.data.spanner.test;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerDatabaseAdminTemplate;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.test.domain.Trade;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * This class provides the foundation for the integration test framework for Spanner. Its
 * responsibilities:
 * <ul>
 * <li>initializes the Spring application context</li>
 * <li>sets up the database schema</li>
 * </ul>
 *
 * <p>Prerequisites for running integration tests:
 *
 * <p>For Cloud Spanner integration tests, you will need to have an instance predefined, everything
 * else is generated. The instance by default is "integration-instance", which can be
 * overriden in <code>src/test/resources/application-test.properties</code>, the property:
 * <code>test.integration.spanner.instance</code>
 * </p>
 *
 * <p>Within the <code>integration-instance</code> instance, the tests rely on a single
 * database for tests, <code>integration-db</code>. This is automatically created, if
 * doesn't exist. The tables are generated to have a unique suffix, which is updated on
 * the entity annotations as well dynamically to avoid collisions of multiple parallel
 * tests running against the same instance.
 * </p>
 *
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
@ContextConfiguration(classes = { IntegrationTestConfiguration.class })
public abstract class AbstractSpannerIntegrationTest {

	private static final Log LOGGER = LogFactory.getLog(AbstractSpannerIntegrationTest.class);

	@Autowired
	protected SpannerOperations spannerOperations;

	@Autowired
	protected ApplicationContext applicationContext;

	protected String tableNameSuffix;

	@Autowired
	SpannerDatabaseAdminTemplate spannerDatabaseAdminTemplate;

	@Autowired
	protected SpannerSchemaUtils spannerSchemaUtils;

	@Autowired
	SpannerMappingContext spannerMappingContext;

	private static boolean setupFailed;

	private static boolean tablesInitialized;

	private static int initializeAttempts;

	@BeforeClass
	public static void checkToRun() {
		assumeThat(System.getProperty("it.spanner"))
				.as("Spanner integration tests are disabled. "
						+ "Please use '-Dit.spanner=true' to enable them. ")
				.isEqualTo("true");
	}

	@Before
	public void setup() {
		try {
			initializeAttempts++;
			if (tablesInitialized) {
				return;
			}
			createDatabaseWithSchema();
			tablesInitialized = true;
		}
		catch (Exception ex) {
			setupFailed = true;
			throw ex;
		}
	}

	@Test
	public void tableCreatedTest() {
		assertThat(this.spannerDatabaseAdminTemplate.tableExists(
				this.spannerMappingContext.getPersistentEntity(Trade.class).tableName())).isTrue();
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
	}

	protected List<String> createSchemaStatements() {
		return this.spannerSchemaUtils
				.getCreateTableDdlStringsForInterleavedHierarchy(Trade.class);
	}

	protected Iterable<String> dropSchemaStatements() {
		return this.spannerSchemaUtils
				.getDropTableDdlStringsForInterleavedHierarchy(Trade.class);
	}

	@After
	public void clean() {
			// this is to reduce duplicated errors reported by surefire plugin
			if (setupFailed || initializeAttempts > 0) {
				initializeAttempts--;
				return;
			}
			this.spannerDatabaseAdminTemplate.executeDdlStrings(dropSchemaStatements(),
					false);
			LOGGER.debug("Integration database cleaned up!");
	}
}
