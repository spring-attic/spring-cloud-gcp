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

import java.io.IOException;

import javax.sql.DataSource;

import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gcp.core.autoconfig.GcpContextAutoConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author João André Martins
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
		classes = {GcpCloudSqlAutoConfiguration.class, GcpContextAutoConfiguration.class},
		properties = {"spring.cloud.gcp.projectId=proj",
				"spring.cloud.gcp.sql.instanceName=test-instance",
				"spring.cloud.gcp.sql.databaseName=test-database",
				"spring.cloud.gcp.sql.userName=watchmaker",
				"spring.cloud.gcp.sql.password=pass"}
)
public class GcpCloudSqlAutoConfigurationTests {

	@Autowired
	private DataSource dataSource;

	@MockBean
	private SQLAdmin mockSqlAdmin;

	@Mock
	private SQLAdmin.Instances mockInstances;

	@Mock
	private SQLAdmin.Instances.Get mockGet;

	private DatabaseInstance databaseInstance = new DatabaseInstance();

	@Before
	public void setUp() throws IOException {
		when(this.mockSqlAdmin.instances()).thenReturn(this.mockInstances);
		when(this.mockInstances.get(eq("proj"), eq("test-instance"))).thenReturn(this.mockGet);
		when(this.mockGet.execute()).thenReturn(this.databaseInstance);
		this.databaseInstance.setRegion("reg");
	}

	@Test
	public void testGetCloudSqlDataSource() {
		HikariDataSource dataSource = (HikariDataSource) this.dataSource;
		assertEquals("jdbc:mysql://google/test-database?cloudSqlInstance=proj:reg:test-instance"
						+ "&socketFactory=com.google.cloud.sql.mysql.SocketFactory",
				dataSource.getJdbcUrl());
		assertEquals("watchmaker", dataSource.getUsername());
		assertEquals("pass", dataSource.getPassword());
	}

//	private AnnotationConfigApplicationContext context;
//
//	@Mock
//	private SQLAdmin mockSqlAdmin;
//
//	@Mock
//	private SQLAdmin.Instances mockInstances;
//
//	@Mock
//	private SQLAdmin.Instances.Get mockGet;
//
//	private DatabaseInstance databaseInstance = new DatabaseInstance();
//
//	@Before
//	public void setUp() throws IOException {
//		MockitoAnnotations.initMocks(this);
//
//		when(this.mockSqlAdmin.instances()).thenReturn(this.mockInstances);
//		when(this.mockInstances.get(eq("proj"), eq("test-instance"))).thenReturn(this.mockGet);
//		when(this.mockGet.execute()).thenReturn(this.databaseInstance);
//		this.databaseInstance.setRegion("reg");
//	}
//
//	@After
//	public void closeContext() {
//		if (this.context != null) {
//			this.context.close();
//		}
//	}
//
//	@Test
//	public void testGetCloudSqlDataSource() {
//		loadEnvironment("spring.cloud.gcp.projectId=proj",
//				"spring.cloud.gcp.sql.instanceName=test-instance",
//				"spring.cloud.gcp.sql.databaseName=test-database",
//				"spring.cloud.gcp.sql.userName=watchmaker",
//				"spring.cloud.gcp.sql.password=pass");
//
//		HikariDataSource dataSource = (HikariDataSource) this.context.getBean(DataSource.class);
//		assertEquals("jdbc:mysql://google/test-database?cloudSqlInstance=proj:reg:test-instance"
//						+ "&socketFactory=com.google.cloud.sql.mysql.SocketFactory",
//				dataSource.getJdbcUrl());
//		assertEquals("watchmaker", dataSource.getUsername());
//		assertEquals("pass", dataSource.getPassword());
//	}
//
//	@Test
//	public void testGetCloudSqlDataSource_withRegion() {
//		loadEnvironment("spring.cloud.gcp.projectId=proj",
//				"spring.cloud.gcp.sql.instanceName=test-instance",
//				"spring.cloud.gcp.sql.databaseName=test-database",
//				"spring.cloud.gcp.sql.region=panama",
//				"spring.cloud.gcp.sql.userName=watchmaker",
//				"spring.cloud.gcp.sql.password=pass");
//
//		HikariDataSource dataSource = (HikariDataSource) this.context.getBean(DataSource.class);
//		assertEquals("jdbc:mysql://google/test-database?cloudSqlInstance=proj:panama:test-instance"
//						+ "&socketFactory=com.google.cloud.sql.mysql.SocketFactory",
//				dataSource.getJdbcUrl());
//		assertEquals("watchmaker", dataSource.getUsername());
//		assertEquals("pass", dataSource.getPassword());
//	}
//
//	private void loadEnvironment(String... environment) {
//		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
//		context.register(GcpContextAutoConfiguration.class);
//		context.register(GcpCloudSqlAutoConfiguration.class);
//		EnvironmentTestUtils.addEnvironment(context, environment);
//		context.refresh();
//		this.context = context;
//	}
}
