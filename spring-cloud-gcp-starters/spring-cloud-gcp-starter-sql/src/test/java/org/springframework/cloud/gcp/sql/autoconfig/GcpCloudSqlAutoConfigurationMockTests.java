/*
 *  Copyright 2017 original author or authors.
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

package org.springframework.cloud.gcp.sql.autoconfig;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gcp.core.autoconfig.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.sql.CloudSqlJdbcUrlProvider;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

/**
 * @author João André Martins
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
		classes = {GcpCloudSqlAutoConfiguration.class, GcpContextAutoConfiguration.class,
				GcpCloudSqlTestConfiguration.class
		},
		properties = {"spring.cloud.gcp.projectId=proj",
				"spring.cloud.gcp.sql.instanceName=test-instance",
				"spring.cloud.gcp.sql.databaseName=test-database",
				"spring.cloud.gcp.sql.initFailFast=false"
		}
)
public abstract class GcpCloudSqlAutoConfigurationMockTests {

	@Autowired
	protected DataSource dataSource;

	@Autowired
	protected CloudSqlJdbcUrlProvider urlProvider;

	public abstract void test();

	public static class CloudSqlJdbcUrlProviderTest extends GcpCloudSqlAutoConfigurationMockTests {
		@Test
		@Override
		public void test() {
			assertEquals("jdbc:mysql://google/test-database?cloudSqlInstance=proj:reg:test-instance"
							+ "&socketFactory=com.google.cloud.sql.mysql.SocketFactory",
					this.urlProvider.getJdbcUrl());
		}
	}

	public static class CloudSqlDataSourceTest extends GcpCloudSqlAutoConfigurationMockTests {
		@Test
		@Override
		public void test() {
			HikariDataSource dataSource = (HikariDataSource) this.dataSource;
			assertEquals("jdbc:mysql://google/test-database?cloudSqlInstance=proj:reg:test-instance"
							+ "&socketFactory=com.google.cloud.sql.mysql.SocketFactory",
					this.urlProvider.getJdbcUrl());
			assertEquals("root", dataSource.getUsername());
			assertEquals("", dataSource.getPassword());
		}
	}

	@TestPropertySource(properties = {"spring.cloud.gcp.sql.region=siberia"})
	public static class GcpCloudSqlAutoConfigurationWithRegionTest
			extends GcpCloudSqlAutoConfigurationMockTests {
		@Test
		@Override
		public void test() {
			HikariDataSource dataSource = (HikariDataSource) this.dataSource;
			assertEquals("jdbc:mysql://google/test-database?cloudSqlInstance="
							+ "proj:siberia:test-instance&"
							+ "socketFactory=com.google.cloud.sql.mysql.SocketFactory",
					this.urlProvider.getJdbcUrl());
			assertEquals("root", dataSource.getUsername());
			assertEquals("", dataSource.getPassword());
		}
	}

	@TestPropertySource(properties = {
			"spring.cloud.gcp.sql.userName=watchmaker",
			"spring.cloud.gcp.sql.password=pass"
	})
	public static class GcpCloudSqlAutoConfigurationWithUserAndPassTest
			extends GcpCloudSqlAutoConfigurationMockTests {
		@Test
		@Override
		public void test() {
			HikariDataSource dataSource = (HikariDataSource) this.dataSource;
			assertEquals("jdbc:mysql://google/test-database?cloudSqlInstance=proj:reg:test-instance"
							+ "&socketFactory=com.google.cloud.sql.mysql.SocketFactory",
					this.urlProvider.getJdbcUrl());
			assertEquals("watchmaker", dataSource.getUsername());
			assertEquals("pass", dataSource.getPassword());
		}
	}
}
