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

import java.io.File;
import java.io.IOException;

import javax.sql.DataSource;

import com.google.cloud.sql.CredentialFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.JndiDataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.XADataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.autoconfigure.core.AppEngineCondition;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.core.GcpProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.util.StringUtils;

/**
 * Provides Google Cloud SQL instance connectivity through Spring JDBC by providing only a
 * database and instance connection name.
 *
 * @author João André Martins
 * @author Artem Bilan
 */
@Configuration
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@ConditionalOnProperty(name = "spring.cloud.gcp.sql.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({ GcpCloudSqlProperties.class, DataSourceProperties.class })
@AutoConfigureBefore({
		DataSourceAutoConfiguration.class,
		JndiDataSourceAutoConfiguration.class,
		XADataSourceAutoConfiguration.class })
@AutoConfigureAfter(GcpContextAutoConfiguration.class)
public class GcpCloudSqlAutoConfiguration {

	public final static String INSTANCE_CONNECTION_NAME_HELP_URL =
			"https://github.com/spring-cloud/spring-cloud-gcp/tree/master/"
					+ "spring-cloud-gcp-starters/spring-cloud-gcp-starter-sql"
					+ "#google-cloud-sql-instance-connection-name";

	private static final Log LOGGER = LogFactory.getLog(GcpCloudSqlAutoConfiguration.class);


	/**
	 * The AppEngine Configuration for the {@link AppEngineCloudSqlJdbcInfoProvider}.
	 */
	@ConditionalOnClass(com.google.cloud.sql.mysql.SocketFactory.class)
	@Conditional(AppEngineCondition.class)
	@ConditionalOnMissingBean(CloudSqlJdbcInfoProvider.class)
	static class AppEngineJdbcInfoProviderConfiguration {

		@Bean
		public CloudSqlJdbcInfoProvider appengineCloudSqlJdbcInfoProvider(
				GcpCloudSqlProperties gcpCloudSqlProperties) {

			CloudSqlJdbcInfoProvider appEngineProvider =
					new AppEngineCloudSqlJdbcInfoProvider(gcpCloudSqlProperties);

			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("App Engine JdbcUrl provider. Connecting to "
						+ appEngineProvider.getJdbcUrl() + " with driver "
						+ appEngineProvider.getJdbcDriverClass());
			}

			return appEngineProvider;
		}

	}

	/**
	 * The MySql Configuration for the {@link DefaultCloudSqlJdbcInfoProvider}
	 * based on the {@link DatabaseType#MYSQL}.
	 */
	@ConditionalOnClass({ com.google.cloud.sql.mysql.SocketFactory.class, com.mysql.jdbc.Driver.class })
	@ConditionalOnMissingBean(CloudSqlJdbcInfoProvider.class)
	static class MySqlJdbcInfoProviderConfiguration {

		@Bean
		public CloudSqlJdbcInfoProvider defaultMySqlJdbcInfoProvider(GcpCloudSqlProperties gcpCloudSqlProperties) {
			CloudSqlJdbcInfoProvider defaultProvider =
					new DefaultCloudSqlJdbcInfoProvider(gcpCloudSqlProperties, DatabaseType.MYSQL);

			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Default " + DatabaseType.MYSQL.name()
						+ " JdbcUrl provider. Connecting to "
						+ defaultProvider.getJdbcUrl() + " with driver "
						+ defaultProvider.getJdbcDriverClass());
			}

			return defaultProvider;
		}

	}

	/**
	 * The PostgreSql Configuration for the {@link DefaultCloudSqlJdbcInfoProvider}
	 * based on the {@link DatabaseType#POSTGRESQL}.
	 */
	@ConditionalOnClass({ com.google.cloud.sql.postgres.SocketFactory.class, org.postgresql.Driver.class })
	@ConditionalOnMissingBean(CloudSqlJdbcInfoProvider.class)
	static class PostgreSqlJdbcInfoProviderConfiguration {

		@Bean
		public CloudSqlJdbcInfoProvider defaultPostgreSqlJdbcInfoProvider(GcpCloudSqlProperties gcpCloudSqlProperties) {
			CloudSqlJdbcInfoProvider defaultProvider =
					new DefaultCloudSqlJdbcInfoProvider(gcpCloudSqlProperties, DatabaseType.POSTGRESQL);

			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Default " + DatabaseType.POSTGRESQL.name()
						+ " JdbcUrl provider. Connecting to "
						+ defaultProvider.getJdbcUrl() + " with driver "
						+ defaultProvider.getJdbcDriverClass());
			}

			return defaultProvider;
		}

	}

	/**
	 * The Configuration to populated {@link DataSourceProperties} bean
	 * based on the cloud-specific properties.
	 */
	@Configuration
	@Import({ GcpCloudSqlAutoConfiguration.AppEngineJdbcInfoProviderConfiguration.class,
			GcpCloudSqlAutoConfiguration.MySqlJdbcInfoProviderConfiguration.class,
			GcpCloudSqlAutoConfiguration.PostgreSqlJdbcInfoProviderConfiguration.class })
	static class CloudSqlDataSourcePropertiesConfiguration {

		@Bean
		@Primary
		@ConditionalOnBean(CloudSqlJdbcInfoProvider.class)
		public DataSourceProperties cloudSqlDataSourceProperties(GcpCloudSqlProperties gcpCloudSqlProperties,
				DataSourceProperties properties,
				GcpProperties gcpProperties,
				CloudSqlJdbcInfoProvider cloudSqlJdbcInfoProvider) {

			if (StringUtils.isEmpty(properties.getUsername())) {
				properties.setUsername("root");
				LOGGER.warn("spring.datasource.username is not specified. Setting default username.");
			}
			if (StringUtils.isEmpty(properties.getDriverClassName())) {
				properties.setDriverClassName(cloudSqlJdbcInfoProvider.getJdbcDriverClass());
			}
			else {
				LOGGER.warn("spring.datasource.driver-class-name is specified. " +
								"Not using generated Cloud SQL configuration");
			}
			if (StringUtils.isEmpty(properties.getUrl())) {
				properties.setUrl(cloudSqlJdbcInfoProvider.getJdbcUrl());
			}
			else {
				LOGGER.warn("spring.datasource.url is specified. Not using generated Cloud SQL configuration");
			}

			setCredentialsProperty(gcpCloudSqlProperties, gcpProperties);

			return properties;
		}

		/**
		 * Set credentials to be used by the Google Cloud SQL socket factory.
		 *
		 * <p>The only way to pass a {@link CredentialFactory} to the socket factory is by passing a
		 * class name through a system property. The socket factory creates an instance of
		 * {@link CredentialFactory} using reflection without any arguments. Because of that, the
		 * credential location needs to be stored somewhere where the class can read it without
		 * any context. It could be possible to pass in a Spring context to
		 * {@link SqlCredentialFactory}, but this is a tricky solution that needs some thinking
		 * about.
		 *
		 * <p>If user didn't specify credentials, the socket factory already does the right thing by
		 * using the application default credentials by default. So we don't need to do anything.
		 */
		private void setCredentialsProperty(GcpCloudSqlProperties gcpCloudSqlProperties, GcpProperties gcpProperties) {
			File credentialsLocationFile;

			try {
				// First tries the SQL configuration credential.
				if (gcpCloudSqlProperties != null
						&& gcpCloudSqlProperties.getCredentials() != null) {
					credentialsLocationFile = gcpCloudSqlProperties.getCredentials().getLocation().getFile();
				}
				// Then, the global credential.
				else if (gcpProperties != null && gcpProperties.getCredentials().getLocation() != null) {
					// A resource might not be in the filesystem, but the Cloud SQL credential must.
					credentialsLocationFile = gcpProperties.getCredentials().getLocation().getFile();
				}
				else {
					// Do nothing, let sockets factory use application default credentials.
					return;
				}
			}
			catch (IOException ioe) {
				LOGGER.info("Error reading Cloud SQL credentials file.", ioe);
				return;
			}

			// This should happen if the Spring resource isn't in the filesystem, but a URL,
			// classpath file, etc.
			if (credentialsLocationFile == null) {
				LOGGER.info("The private key of the Google Cloud SQL credential must "
						+ "be in a file on the filesystem.");
				return;
			}

			System.setProperty(SqlCredentialFactory.CREDENTIAL_LOCATION_PROPERTY_NAME,
					credentialsLocationFile.getAbsolutePath());

			// If there are specified credentials, tell sockets factory to use them.
			System.setProperty(CredentialFactory.CREDENTIAL_FACTORY_PROPERTY,
					SqlCredentialFactory.class.getName());
		}

	}

}
