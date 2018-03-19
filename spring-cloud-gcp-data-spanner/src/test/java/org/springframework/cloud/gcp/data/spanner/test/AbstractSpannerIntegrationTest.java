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

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseId;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * <p>
 * For Spanner integration tests, you will need to have an instance predefined, everything
 * else is generated. The instance by default is "integration-instance", which can be
 * overriden in <code>src/test/resources/application-test.properties</code>, the property:
 * <code>test.integration.spanner.instance</code>
 * </p>
 * <p>
 * Within the <code>integration-instance</code> instance, there is a single database for
 * tests, <code>integration-db</code>. This is automatically created, if doesn't exist.
 * The tables are generated to have a unique postfix, which is updated on the entity
 * annotations as well dynamically to avoid collisions of multiple parallel tests running
 * against the same instance.
 * </p>
 *
 * @author Balint Pato
 */
@ContextConfiguration(classes = { IntegrationTestConfiguration.class })
public abstract class AbstractSpannerIntegrationTest {

	@Autowired
	@Rule
	public SkipWhenNoSpanner skipWhenNoSpanner;

	@Autowired
	protected DatabaseAdminClient databaseAdminClient;

	@Autowired
	protected DatabaseId databaseId;

	@Autowired
	protected ApplicationContext applicationContext;

	protected String tablePostfix;

	private boolean setupFailed;

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
		this.tablePostfix = String.valueOf(System.currentTimeMillis());
		ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) this.applicationContext)
				.getBeanFactory();
		beanFactory.registerSingleton("tablePostfix", this.tablePostfix);
		String instanceId = this.databaseId.getInstanceId().getInstance();
		String database = this.databaseId.getDatabase();

		if (!hasDatabaseDefined(instanceId, database)) {
			this.databaseAdminClient.createDatabase(instanceId, database,
					getCreateSchemaStatements()).waitFor();
			System.out.println(
					this.getClass() + " - Integration database created with schema: " + getCreateSchemaStatements());
		}
		else {
			this.databaseAdminClient.updateDatabaseDdl(instanceId, database,
					getCreateSchemaStatements(), null).waitFor();
			System.out.println(this.getClass() + " - schema created: " + getCreateSchemaStatements());
		}
	}

	protected abstract Iterable<String> getCreateSchemaStatements();

	protected abstract Iterable<String> getDropSchemaStatements();

	@After
	public void clean() {
		// this is to reduce duplicated errors reported by surefire plugin
		if (this.setupFailed) {
			return;
		}
		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) this.applicationContext
				.getAutowireCapableBeanFactory();
		beanFactory
				.destroySingleton("tablePostfix");
		String instanceId = this.databaseId.getInstanceId().getInstance();
		String database = this.databaseId.getDatabase();
		if (hasDatabaseDefined(instanceId, database)) {
			this.databaseAdminClient.updateDatabaseDdl(instanceId, database,
					getDropSchemaStatements(),
					null).waitFor();
			System.out.println("Integration database cleaned up!");
		}
	}

	private boolean hasDatabaseDefined(String instanceId, String database) {
		Stream<String> databaseNames = StreamSupport
				.stream(this.databaseAdminClient.listDatabases(instanceId).getValues().spliterator(), false)
				.map(d -> d.getId().getDatabase());
		return databaseNames.anyMatch(database::equals);
	}
}
