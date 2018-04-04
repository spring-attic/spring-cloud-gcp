/*
 *  Copyright 2017-2018 original author or authors.
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

import java.io.IOException;
import java.nio.file.Files;

import javax.sql.DataSource;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.cloud.sql.CredentialFactory;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * @author João André Martins
 * @author Artem Bilan
 */
public class GcpCloudSqlAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.gcp.sql.databaseName=test-database")
			.withConfiguration(AutoConfigurations.of(GcpCloudSqlAutoConfiguration.class,
					GcpContextAutoConfiguration.class,
					DataSourceAutoConfiguration.class))
			.withUserConfiguration(GcpCloudSqlTestConfiguration.class);

	@Test
	public void testCloudSqlDataSourceTest() {
		this.contextRunner.withPropertyValues("spring.cloud.gcp.sql.instanceConnectionName="
				+ "tubular-bells:singapore:test-instance")
				.run(context -> {
					HikariDataSource dataSource =
							(HikariDataSource) context.getBean(DataSource.class);
					CloudSqlJdbcInfoProvider urlProvider =
							context.getBean(CloudSqlJdbcInfoProvider.class);
					assertThat(dataSource.getDriverClassName()).matches("com.mysql.jdbc.Driver");
					assertThat(urlProvider.getJdbcUrl()).isEqualTo(
							"jdbc:mysql://google/test-database?cloudSqlInstance="
									+ "tubular-bells:singapore:test-instance&socketFactory="
									+ "com.google.cloud.sql.mysql.SocketFactory&useSSL=false");
					assertThat(dataSource.getUsername()).matches("root");
					assertThat(dataSource.getPassword()).isNull();
					assertThat(urlProvider.getJdbcDriverClass()).matches("com.mysql.jdbc.Driver");
				});
	}

	@Test
	public void testCloudSqlAppEngineDataSourceTest() {
		this.contextRunner.withPropertyValues(
				"spring.cloud.gcp.project-id=im-not-used-for-anything",
				"spring.cloud.gcp.sql.instanceConnectionName=tubular-bells:australia:test-instance")
				.withSystemProperties(
						"com.google.appengine.runtime.version=Google App Engine/Some Server")
				.run(context -> {
					HikariDataSource dataSource =
							(HikariDataSource) context.getBean(DataSource.class);
					CloudSqlJdbcInfoProvider urlProvider =
							context.getBean(CloudSqlJdbcInfoProvider.class);
					assertThat(urlProvider.getJdbcDriverClass())
							.matches("com.mysql.jdbc.GoogleDriver");
					assertThat(dataSource.getDriverClassName())
							.matches("com.mysql.jdbc.GoogleDriver");
					assertThat(urlProvider.getJdbcUrl()).matches("jdbc:google:mysql://"
							+ "tubular-bells:australia:test-instance/test-database");
					assertThat(dataSource.getUsername()).matches("root");
					assertThat(dataSource.getPassword()).isNull();
				});
	}

	@Test
	public void testUserAndPassword() {
		this.contextRunner.withPropertyValues("spring.datasource.username=watchmaker",
				"spring.datasource.password=pass",
				"spring.cloud.gcp.sql.instanceConnectionName=proj:reg:test-instance")
				.run(context -> {
					HikariDataSource dataSource =
							(HikariDataSource) context.getBean(DataSource.class);
					CloudSqlJdbcInfoProvider urlProvider =
							context.getBean(CloudSqlJdbcInfoProvider.class);
					assertThat(urlProvider.getJdbcUrl()).isEqualTo(
							"jdbc:mysql://google/test-database"
									+ "?cloudSqlInstance=proj:reg:test-instance&socketFactory="
									+ "com.google.cloud.sql.mysql.SocketFactory&useSSL=false");
					assertThat(dataSource.getUsername()).matches("watchmaker");
					assertThat(dataSource.getPassword()).matches("pass");
					assertThat(urlProvider.getJdbcDriverClass()).matches("com.mysql.jdbc.Driver");
				});
	}

	@Test
	public void testDataSourceProperties() {
		this.contextRunner.withPropertyValues(
				"spring.cloud.gcp.sql.instanceConnectionName=proj:reg:test-instance",
				"spring.datasource.hikari.connectionTestQuery=select 1",
				"spring.datasource.hikari.maximum-pool-size=19")
				.run(context -> {
					HikariDataSource dataSource =
							(HikariDataSource) context.getBean(DataSource.class);
					CloudSqlJdbcInfoProvider urlProvider =
							context.getBean(CloudSqlJdbcInfoProvider.class);
					assertThat(urlProvider.getJdbcUrl()).isEqualTo(
							"jdbc:mysql://google/test-database"
									+ "?cloudSqlInstance=proj:reg:test-instance&socketFactory="
									+ "com.google.cloud.sql.mysql.SocketFactory&useSSL=false");
					assertThat(urlProvider.getJdbcDriverClass()).matches("com.mysql.jdbc.Driver");
					assertThat(dataSource.getMaximumPoolSize()).isEqualTo(19);
					assertThat(dataSource.getConnectionTestQuery()).matches("select 1");
				});
	}

	@Test
	public void testInstanceConnectionName() {
		this.contextRunner.withPropertyValues(
				"spring.cloud.gcp.sql.instanceConnectionName=world:asia:japan")
				.run(context -> {
					CloudSqlJdbcInfoProvider urlProvider =
							context.getBean(CloudSqlJdbcInfoProvider.class);
					assertThat(urlProvider.getJdbcUrl()).isEqualTo(
							"jdbc:mysql://google/test-database"
									+ "?cloudSqlInstance=world:asia:japan&socketFactory="
									+ "com.google.cloud.sql.mysql.SocketFactory&useSSL=false");
					assertThat(urlProvider.getJdbcDriverClass()).matches("com.mysql.jdbc.Driver");
				});
	}

	@Test
	public void testPostgre() {
		this.contextRunner.withPropertyValues(
				"spring.cloud.gcp.sql.instanceConnectionName=tubular-bells:singapore:test-instance")
				.withClassLoader(
						new FilteredClassLoader("com.google.cloud.sql.mysql"))
				.run(context -> {
					CloudSqlJdbcInfoProvider urlProvider =
							context.getBean(CloudSqlJdbcInfoProvider.class);
					assertThat(urlProvider.getJdbcUrl()).isEqualTo(
							"jdbc:postgresql://google/test-database?socketFactory=com.google.cloud"
									+ ".sql.postgres.SocketFactory&socketFactoryArg="
									+ "tubular-bells:singapore:test-instance&useSSL=false");
					assertThat(urlProvider.getJdbcDriverClass()).matches("org.postgresql.Driver");
				});
	}

	@Test
	public void testNoJdbc() {
		this.contextRunner.withPropertyValues(
				"spring.cloud.gcp.sql.instanceConnectionName=tubular-bells:singapore:test-instance")
				.withClassLoader(
						new FilteredClassLoader(EmbeddedDatabaseType.class, DataSource.class))
				.run(context -> {
					assertThat(context.getBeanNamesForType(DataSource.class)).isEmpty();
					assertThat(context.getBeanNamesForType(DataSourceProperties.class)).isEmpty();
					assertThat(context.getBeanNamesForType(GcpCloudSqlProperties.class)).isEmpty();
					assertThat(context.getBeanNamesForType(CloudSqlJdbcInfoProvider.class)).isEmpty();
				});
	}

	@Test
	public void testNoInstanceConnectionName() {
		this.contextRunner.run(context -> {
			try {
				DataSource dataSource = context.getBean(DataSource.class);
				fail("Test should fail due to no instance connection name.");
			}
			catch (IllegalStateException ise) {
				Throwable cause = ise.getCause();
				assertThat(cause).isInstanceOf(BeanCreationException.class);
				cause = cause.getCause();
				assertThat(cause).isInstanceOf(BeanInstantiationException.class);
				cause = cause.getCause();
				assertThat(cause).isInstanceOf(IllegalArgumentException.class);
				assertThat(cause.getMessage()).isEqualTo(
						"An instance connection name must be provided. Refer to "
						+ GcpCloudSqlAutoConfiguration.INSTANCE_CONNECTION_NAME_HELP_URL
						+ " for more information.");
			}
		});
	}

	@Test
	public void testCfMySqlCredentials() throws IOException {
		Resource vcapServicesFile = new ClassPathResource("VCAP_SERVICES");
		String vcapServicesString =
				new String(Files.readAllBytes(vcapServicesFile.getFile().toPath()));
		this.contextRunner
				.withPropertyValues("spring.cloud.gcp.sql.instanceConnectionName="
						+ "tubular-bells:singapore:test-instance")
				.withClassLoader(new FilteredClassLoader("com.google.cloud.sql.postgres"))
				.withSystemProperties("VCAP_SERVICES=" + vcapServicesString)
				.run(context -> {
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(System.getProperty(SqlCredentialFactory.CF_SQL_SERVICE_ACCOUNT))
							.isEqualTo("google-cloudsql-mysql");
					assertThat(System.getProperty(CredentialFactory.CREDENTIAL_FACTORY_PROPERTY))
							.isEqualTo(SqlCredentialFactory.class.getName());

					GoogleCredential credential =
							(GoogleCredential) new SqlCredentialFactory().create();
					assertThat(credential.getServiceAccountId()).isEqualTo(
							"pcf-binding-f3a128f5@"
									+ "graphite-test-spring-cloud-gcp.iam.gserviceaccount.com");
				});
	}

	@Test
	public void testCfPostgresCredentials() throws IOException {
		Resource vcapServicesFile = new ClassPathResource("VCAP_SERVICES");
		String vcapServicesString =
				new String(Files.readAllBytes(vcapServicesFile.getFile().toPath()));
		this.contextRunner
				.withPropertyValues("spring.cloud.gcp.sql.instanceConnectionName="
						+ "tubular-bells:singapore:test-instance")
				.withClassLoader(new FilteredClassLoader("com.google.cloud.sql.mysql"))
				.withSystemProperties("VCAP_SERVICES=" + vcapServicesString)
				.run(context -> {
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(System.getProperty(SqlCredentialFactory.CF_SQL_SERVICE_ACCOUNT))
							.isEqualTo("google-cloudsql-postgres");
					assertThat(System.getProperty(CredentialFactory.CREDENTIAL_FACTORY_PROPERTY))
							.isEqualTo(SqlCredentialFactory.class.getName());

					GoogleCredential credential =
							(GoogleCredential) new SqlCredentialFactory().create();
					assertThat(credential.getServiceAccountId()).isEqualTo("pcf-binding-fa6bb781@"
							+ "graphite-test-spring-cloud-gcp.iam.gserviceaccount.com");
				});
	}

}
