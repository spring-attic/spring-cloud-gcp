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

package com.example;

import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.gcp.data.spanner.core.admin.DatabaseIdProvider;
import org.springframework.cloud.gcp.data.spanner.core.admin.SettableClientSpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerDatabaseAdminTemplate;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Tests for the Spanner repository example using multiple databases.
 *
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application.properties")
@EnableAutoConfiguration
public class SpannerRepositoryMultiDatabaseTests {
	@Autowired
	private TraderRepository traderRepository;

	@Autowired
	private SpannerSchemaUtils spannerSchemaUtils;

	@Autowired
	private SpannerDatabaseAdminTemplate spannerDatabaseAdminTemplate;

	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"Spanner integration tests are disabled. "
						+ "Please use '-Dit.spanner=true' to enable them. ",
				System.getProperty("it.spanner"), is("true"));
	}

	@Before
	public void setUp() {
		createTable();
		Config.flipDatabase();
		createTable();
	}

	private void createTable() {
		if (!this.spannerDatabaseAdminTemplate.tableExists("traders")) {
			this.spannerDatabaseAdminTemplate.executeDdlStrings(
					this.spannerSchemaUtils.getCreateTableDdlStringsForInterleavedHierarchy(Trader.class),
					true);
		}
		this.traderRepository.deleteAll();
	}

	@Test
	public void testLoadsCorrectData() {
		// all operations on Cloud Spanner use connections determined by the `databaseIdProvider`
		// bean.
		assertThat(this.traderRepository.count()).isEqualTo(0);
		Config.flipDatabase();
		assertThat(this.traderRepository.count()).isEqualTo(0);

		// however, save operations in particular utilize the
		// `databaseClientConfiguringSpannerTemplate`
		// bean which allows the user to examine the parameters of each `SpannerTemplate` call.
		this.traderRepository.save(new Trader("1", "a", "al"));
		this.traderRepository.save(new Trader("2", "a", "al"));
		this.traderRepository.save(new Trader("3", "a", "al"));
		this.traderRepository.save(new Trader("5", "a", "al"));

		assertThat(this.traderRepository.count()).isEqualTo(1);
		Config.flipDatabase();
		assertThat(this.traderRepository.count()).isEqualTo(3);
	}

	/**
	 * Configuring custom multiple database connections.
	 *
	 * @author Chengyuan Zhao
	 */
	@Configuration
	static class Config {

		static boolean databaseFlipper;

		@Value("${spring.cloud.gcp.spanner.instance-id}")
		String instanceId;

		/**
		 * Flips the database connection that all repositories and templates use.
		 */
		static void flipDatabase() {
			databaseFlipper = !databaseFlipper;
		}

		@Bean
		public DatabaseIdProvider databaseIdProvider(SpannerOptions spannerOptions) {
			return () -> DatabaseId.of(spannerOptions.getProjectId(), this.instanceId,
					databaseFlipper ? "db1" : "db2");
		}

		@Bean
		public SettableClientSpannerTemplate settableClientSpannerTemplate(Spanner spanner) {
			return new SettableClientSpannerTemplate() {

						/*
						 * The purpose is to call the `setDatabaseClient` method, which determines the connection
						 * to use for this operation based on the arguments.
						 * Note that this override DOES NOT need to execute the mutations. The override method
						 * merely presents the arguments to the user for client-setting.
						 */
						@Override
						public void upsert(Object object) {
							Trader trader = (Trader) object;
							if (Integer.valueOf(trader.getTraderId()) % 2 == 0) {
								getDatabaseClientProvider()
										.setCurrentThreadLocalDatabaseClient(spanner.getDatabaseClient(DatabaseId
												.of(spanner.getOptions().getProjectId(), Config.this.instanceId,
														"db2")));
							}
							else {
								getDatabaseClientProvider()
										.setCurrentThreadLocalDatabaseClient(spanner.getDatabaseClient(DatabaseId
												.of(spanner.getOptions().getProjectId(), Config.this.instanceId,
														"db1")));
							}
						}
			};
		}
	}
}
