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

package org.springframework.cloud.gcp.autoconfigure.sql;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

/**
 * @author João André Martins
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
		GcpCloudSqlAutoConfiguration.class, GcpContextAutoConfiguration.class,
		GcpCloudSqlTestConfiguration.class, DataSourceAutoConfiguration.class
}, properties = {
		"spring.cloud.gcp.sql.databaseName=test-database",
		"spring.cloud.gcp.config.enabled=false"
})
public abstract class GcpCloudSqlAutoConfigurationMockTests {

	@Autowired
	protected DataSource dataSource;

	@Autowired
	protected CloudSqlJdbcInfoProvider urlProvider;

	public abstract void test();

	@TestPropertySource(properties = {
			"spring.cloud.gcp.sql.instanceConnectionName=tubular-bells:singapore:test-instance"
	})
	public static class CloudSqlDataSourceTest extends GcpCloudSqlAutoConfigurationMockTests {
		@Test
		@Override
		public void test() {
			HikariDataSource dataSource = (HikariDataSource) this.dataSource;
			assertEquals("com.mysql.jdbc.Driver", dataSource.getDriverClassName());
			assertEquals("jdbc:mysql://google/test-database?cloudSqlInstance=tubular-bells:singapore:test-instance"
					+ "&socketFactory=com.google.cloud.sql.mysql.SocketFactory&useSSL=false",
					this.urlProvider.getJdbcUrl());
			assertEquals("root", dataSource.getUsername());
			assertEquals(null, dataSource.getPassword());
			assertEquals("com.mysql.jdbc.Driver", this.urlProvider.getJdbcDriverClass());
		}
	}

	// Project ID passed in so ServiceOptions doesn't try to get it from AppIdentity API,
	// which isn't easily mockable.
	@TestPropertySource(properties = {
			"spring.cloud.gcp.project-id=im-not-used-for-anything",
			"spring.cloud.gcp.sql.instanceConnectionName=tubular-bells:australia:test-instance" })
	public static class CloudSqlAppEngineDataSourceTest extends GcpCloudSqlAutoConfigurationMockTests {
		@BeforeClass
		public static void setUp() {
			System.setProperty("com.google.appengine.runtime.version", "Google App Engine/Some Server");
		}

		@AfterClass
		public static void tearDown() {
			System.getProperties().remove("com.google.appengine.runtime.version");
		}

		@Test
		@Override
		public void test() {
			HikariDataSource dataSource = (HikariDataSource) this.dataSource;
			assertEquals("com.mysql.jdbc.GoogleDriver",
					this.urlProvider.getJdbcDriverClass());
			assertEquals("com.mysql.jdbc.GoogleDriver", dataSource.getDriverClassName());
			assertEquals("jdbc:google:mysql://tubular-bells:australia:test-instance/test-database",
					this.urlProvider.getJdbcUrl());
			assertEquals("root", dataSource.getUsername());
			assertEquals(null, dataSource.getPassword());
		}
	}

	@TestPropertySource(properties = {
			"spring.datasource.username=watchmaker",
			"spring.datasource.password=pass",
			"spring.cloud.gcp.sql.instanceConnectionName=proj:reg:test-instance"
	})
	public static class GcpCloudSqlAutoConfigurationWithUserAndPassTest
			extends GcpCloudSqlAutoConfigurationMockTests {
		@Test
		@Override
		public void test() {
			HikariDataSource dataSource = (HikariDataSource) this.dataSource;
			assertEquals("jdbc:mysql://google/test-database?cloudSqlInstance=proj:reg:test-instance"
					+ "&socketFactory=com.google.cloud.sql.mysql.SocketFactory&useSSL=false",
					this.urlProvider.getJdbcUrl());
			assertEquals("watchmaker", dataSource.getUsername());
			assertEquals("pass", dataSource.getPassword());
			assertEquals("com.mysql.jdbc.Driver", this.urlProvider.getJdbcDriverClass());
		}
	}

	@TestPropertySource(properties = {
			"spring.cloud.gcp.sql.instanceConnectionName=proj:reg:test-instance",
			"spring.datasource.hikari.connectionTestQuery=select 1",
			"spring.datasource.hikari.maximum-pool-size=19"

	})
	public static class GcpCloudSqlAutoConfigurationWithDatasourcePropertiesTest
			extends GcpCloudSqlAutoConfigurationMockTests {
		@Test
		@Override
		public void test() {
			HikariDataSource dataSource = (HikariDataSource) this.dataSource;
			assertEquals("jdbc:mysql://google/test-database?cloudSqlInstance=proj:reg:test-instance"
					+ "&socketFactory=com.google.cloud.sql.mysql.SocketFactory&useSSL=false",
					this.urlProvider.getJdbcUrl());
			assertEquals("com.mysql.jdbc.Driver", this.urlProvider.getJdbcDriverClass());
			assertEquals(19, dataSource.getMaximumPoolSize());
			assertEquals("select 1", dataSource.getConnectionTestQuery());
		}
	}

	@TestPropertySource(properties = {
			"spring.cloud.gcp.sql.instanceConnectionName=world:asia:japan"
	})
	public static class GcpCloudSqlAutoConfigurationWithInstanceConnectionNameTest
			extends GcpCloudSqlAutoConfigurationMockTests {
		@Test
		@Override
		public void test() {
			assertEquals("jdbc:mysql://google/test-database?cloudSqlInstance=world:asia:japan"
					+ "&socketFactory=com.google.cloud.sql.mysql.SocketFactory&useSSL=false",
					this.urlProvider.getJdbcUrl());
			assertEquals("com.mysql.jdbc.Driver", this.urlProvider.getJdbcDriverClass());
		}
	}

	@TestPropertySource(properties = {
			"spring.cloud.gcp.sql.database-type=postgresql",
			"spring.cloud.gcp.sql.instanceConnectionName=tubular-bells:singapore:test-instance"
	})
	public static class GcpCloudSqlAutoConfigurationPostgresTest
			extends GcpCloudSqlAutoConfigurationMockTests {

		@Test
		@Override
		public void test() {
			assertEquals("jdbc:postgresql://google/test-database?socketFactory="
					+ "com.google.cloud.sql.postgres.SocketFactory&"
					+ "socketFactoryArg=tubular-bells:singapore:test-instance&useSSL=false",
					this.urlProvider.getJdbcUrl());
			assertEquals("org.postgresql.Driver", this.urlProvider.getJdbcDriverClass());
		}
	}
}
