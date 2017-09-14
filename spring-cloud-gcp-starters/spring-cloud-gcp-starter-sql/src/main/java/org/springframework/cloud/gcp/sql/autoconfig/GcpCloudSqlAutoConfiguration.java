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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;

import javax.sql.DataSource;

import com.google.cloud.sql.CredentialFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.core.AppEngineCondition;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpProperties;
import org.springframework.cloud.gcp.core.autoconfig.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.sql.CloudSqlJdbcInfoProvider;
import org.springframework.cloud.gcp.sql.GcpCloudSqlProperties;
import org.springframework.cloud.gcp.sql.SqlCredentialFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Google Cloud SQL starter.
 *
 * <p>
 * Provides Google Cloud SQL instance connectivity through Spring JDBC by providing only a database
 * and instance connection name.
 *
 * @author João André Martins
 */
@Configuration
@EnableConfigurationProperties(GcpCloudSqlProperties.class)
@ConditionalOnClass(GcpProjectIdProvider.class)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@AutoConfigureAfter(GcpContextAutoConfiguration.class)
public class GcpCloudSqlAutoConfiguration {

	private static final Log LOGGER = LogFactory.getLog(GcpCloudSqlAutoConfiguration.class);

	public final static String INSTANCE_CONNECTION_NAME_HELP_URL =
			"https://github.com/spring-cloud/spring-cloud-gcp/tree/master/"
					+ "spring-cloud-gcp-starters/spring-cloud-gcp-starter-sql"
					+ "#google-cloud-sql-instance-connection-name";

	@Autowired
	private GcpCloudSqlProperties gcpCloudSqlProperties;

	@Autowired
	private GcpProperties gcpProperties;

	@Bean
	@ConditionalOnMissingBean(CloudSqlJdbcInfoProvider.class)
	@Conditional(AppEngineCondition.class)
	public CloudSqlJdbcInfoProvider appengineCloudSqlJdbcInfoProvider() {
		CloudSqlJdbcInfoProvider appEngineProvider =
				new AppEngineCloudSqlJdbcInfoProvider(this.gcpCloudSqlProperties);

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("App Engine JdbcUrl provider. Connecting to "
					+ appEngineProvider.getJdbcUrl() + " with driver "
					+ appEngineProvider.getJdbcDriverClass());
		}

		setCredentialsProperty();

		return appEngineProvider;
	}

	@Bean
	@ConditionalOnMissingBean(CloudSqlJdbcInfoProvider.class)
	public CloudSqlJdbcInfoProvider defaultJdbcInfoProvider() {
		CloudSqlJdbcInfoProvider defaultProvider =
				new DefaultCloudSqlJdbcInfoProvider(this.gcpCloudSqlProperties);

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Default " + this.gcpCloudSqlProperties.getDatabaseType().name()
					+ " JdbcUrl provider. Connecting to "
					+ defaultProvider.getJdbcUrl() + " with driver "
					+ defaultProvider.getJdbcDriverClass());
		}

		setCredentialsProperty();

		return defaultProvider;
	}

	@Bean
	@ConditionalOnMissingBean(DataSource.class)
	public DataSource cloudSqlDataSource(CloudSqlJdbcInfoProvider cloudSqlJdbcInfoProvider)
			throws GeneralSecurityException, IOException, ClassNotFoundException {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setDriverClassName(cloudSqlJdbcInfoProvider.getJdbcDriverClass());
		hikariConfig.setJdbcUrl(cloudSqlJdbcInfoProvider.getJdbcUrl());
		hikariConfig.setUsername(this.gcpCloudSqlProperties.getUserName());
		hikariConfig.setPassword(this.gcpCloudSqlProperties.getPassword());
		// Setting this is useful to disable connection initialization. Especially in unit tests.
		hikariConfig.setInitializationFailFast(this.gcpCloudSqlProperties.isInitFailFast());

		return new HikariDataSource(hikariConfig);
	}

	/**
	 * Set credentials to be used by the Google Cloud SQL socket factory.
	 *
	 * <p>
	 * The only way to pass a {@link CredentialFactory} to the socket factory is by passing a class
	 * name through a system property. The socket factory creates an instance of
	 * {@link CredentialFactory} using reflection without any arguments. Because of that, the
	 * credential location needs to be stored somewhere where the class can read it without any
	 * context. It could be possible to pass in a Spring context to {@link SqlCredentialFactory},
	 * but this is a tricky solution that needs some thinking about.
	 *
	 * <p>
	 * If user didn't specify credentials, the socket factory already does the right thing by using
	 * the application default credentials by default. So we don't need to do anything.
	 */
	private void setCredentialsProperty() {
		try {
			// First tries the SQL configuration credential.
			if (this.gcpCloudSqlProperties != null
					&& this.gcpCloudSqlProperties.getCredentialsLocation() != null
					&& Files.exists(this.gcpCloudSqlProperties.getCredentialsLocation())) {
				System.setProperty(SqlCredentialFactory.CREDENTIAL_LOCATION_PROPERTY_NAME,
						this.gcpCloudSqlProperties.getCredentialsLocation().toString());
			}
			// Then, the global credential.
			else if (this.gcpProperties != null
					&& this.gcpProperties.getCredentials() != null
					&& this.gcpProperties.getCredentials().getLocation() != null
					&& this.gcpProperties.getCredentials().getLocation().exists()) {
				// A resource might not be in the filesystem, but the Cloud SQL credential must.
				File credentialsLocationFile =
						this.gcpProperties.getCredentials().getLocation().getFile();

				// This should happen if the Spring resource isn't in the filesystem, but a URL,
				// classpath file, etc.
				if (credentialsLocationFile == null) {
					if (LOGGER.isWarnEnabled()) {
						LOGGER.warn("The private key of the Google Cloud SQL credential must "
								+ "be in a file on the filesystem.");
					}
					return;
				}
				System.setProperty(SqlCredentialFactory.CREDENTIAL_LOCATION_PROPERTY_NAME,
						credentialsLocationFile.getAbsolutePath());
			}
			else {
				return;
			}

			// If there are specified credentials, tell sockets factory to use them.
			System.setProperty(CredentialFactory.CREDENTIAL_FACTORY_PROPERTY,
					SqlCredentialFactory.class.getName());
		}
		catch (IOException ioe) {
			// Do nothing, let sockets factory use application default credentials.
			LOGGER.debug(" Error reading Cloud SQL credentials file.", ioe);
		}
	}
}
