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

package org.springframework.cloud.gcp.data.spanner.test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseId;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntityImpl;
import org.springframework.cloud.gcp.data.spanner.test.domain.Trade;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.util.TypeInformation;
import org.springframework.test.context.ContextConfiguration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class provides the foundation for the integration test framework for Spanner.
 * Its responsibilities:
 * <ul>
 *   <li>initializes the Spring application context</li>
 *   <li>sets up the database schema</li>
 *   <li>manages table suffix generation so that parallel test cases don't collide</li>
 * </ul>
 * <p>
 * Prerequisites for running integration tests:
 *
 * For Spanner integration tests, you will need to have an instance predefined, everything
 * else is generated. The instance by default is "integration-instance", which can be
 * overriden in <code>src/test/resources/application-test.properties</code>, the property:
 * <code>test.integration.spanner.instance</code>
 * </p>
 * <p>
 * Within the <code>integration-instance</code> instance, the tests rely on a single database for
 * tests, <code>integration-db</code>. This is automatically created, if doesn't exist.
 * The tables are generated to have a unique suffix, which is updated on the entity
 * annotations as well dynamically to avoid collisions of multiple parallel tests running
 * against the same instance.
 * </p>
 *
 * @author Balint Pato
 */
@ContextConfiguration(classes = { IntegrationTestConfiguration.class })
public abstract class AbstractSpannerIntegrationTest {

	private static final String TABLE_NAME_SUFFIX_BEAN_NAME = "tableNameSuffix";

	@Autowired
	protected DatabaseAdminClient databaseAdminClient;

	@Autowired
	protected DatabaseId databaseId;

	@Autowired
	protected ApplicationContext applicationContext;

	protected String tableNameSuffix;

	private boolean setupFailed;

	@BeforeClass
	public static void checkToRun() {
		assumeThat("Spanner integration tests are disabled. Please use '-Dit.spanner=true' "
				+ "to enable them. ",
				System.getProperty("it.spanner"),
				is("true"));
	}

	@Before
	public void setup() {
		try {
			createDatabaseWithSchema();
		}
		catch (Exception e) {
			this.setupFailed = true;
			throw e;
		}
	}

	protected void createDatabaseWithSchema() {
		this.tableNameSuffix = String.valueOf(System.currentTimeMillis());
		ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) this.applicationContext)
				.getBeanFactory();
		beanFactory.registerSingleton("tableNameSuffix", this.tableNameSuffix);
		String instanceId = this.databaseId.getInstanceId().getInstance();
		String database = this.databaseId.getDatabase();

		if (!hasDatabaseDefined(instanceId, database)) {
			this.databaseAdminClient.createDatabase(instanceId, database,
					createSchemaStatements()).waitFor();
			System.out.println(
					this.getClass() + " - Integration database created with schema: " + createSchemaStatements());
		}
		else {
			this.databaseAdminClient.updateDatabaseDdl(instanceId, database,
					createSchemaStatements(), null).waitFor();
			System.out.println(this.getClass() + " - schema created: " + createSchemaStatements());
		}
	}

	protected List<String> createSchemaStatements() {
		return Arrays.asList(Trade.createDDL(tableNameFor(Trade.class)));
	}

	protected Iterable<String> dropSchemaStatements() {
		return Arrays.asList(Trade.dropDDL(tableNameFor(Trade.class)));
	}

	protected String tableNameFor(Class<Trade> type) {
		return createDummyEntity(type).tableName();
	}

	protected <T> SpannerPersistentEntity createDummyEntity(Class<T> type) {
		TypeInformation<T> typeInformation = mock(TypeInformation.class);
		when(typeInformation.getType()).thenReturn(type);
		SpannerPersistentEntity dummyTrade = new SpannerPersistentEntityImpl<>(typeInformation);
		dummyTrade.setApplicationContext(this.applicationContext);
		return dummyTrade;
	}

	@After
	public void clean() {
		try {
			// this is to reduce duplicated errors reported by surefire plugin
			if (this.setupFailed) {
				return;
			}
			String instanceId = this.databaseId.getInstanceId().getInstance();
			String database = this.databaseId.getDatabase();
			if (hasDatabaseDefined(instanceId, database)) {
				this.databaseAdminClient.updateDatabaseDdl(instanceId, database,
						dropSchemaStatements(),
						null).waitFor();
				System.out.println("Integration database cleaned up!");
			}
		}
		finally {
			// we need to remove the extra bean created even if there is a failure at startup
			DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) this.applicationContext
					.getAutowireCapableBeanFactory();
			beanFactory.destroySingleton(TABLE_NAME_SUFFIX_BEAN_NAME);
		}
	}

	private boolean hasDatabaseDefined(String instanceId, String database) {
		Stream<String> databaseNames = StreamSupport
				.stream(this.databaseAdminClient.listDatabases(instanceId).getValues().spliterator(), false)
				.map(d -> d.getId().getDatabase());
		return databaseNames.anyMatch(database::equals);
	}
}
