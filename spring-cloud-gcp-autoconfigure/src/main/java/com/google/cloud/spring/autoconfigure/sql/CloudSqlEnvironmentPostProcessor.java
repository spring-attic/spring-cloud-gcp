/*
 * Copyright 2017-2020 the original author or authors.
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

package com.google.cloud.spring.autoconfigure.sql;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.cloud.spring.autoconfigure.core.GcpProperties;
import com.google.cloud.spring.core.Credentials;
import com.google.cloud.sql.CredentialFactory;
import com.google.cloud.sql.core.CoreSocketFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;

/**
 * Provides Google Cloud SQL instance connectivity through Spring JDBC by providing only a
 * database and instance connection name.
 *
 * @author João André Martins
 * @author Artem Bilan
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 * @author Eddú Meléndez
 */
public class CloudSqlEnvironmentPostProcessor implements EnvironmentPostProcessor {
	private static final Log LOGGER =
			LogFactory.getLog(CloudSqlEnvironmentPostProcessor.class);

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		DatabaseType databaseType = getEnabledDatabaseType(environment);

		if (databaseType != null) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("post-processing Cloud SQL properties for + " + databaseType.name());
			}

			// Bind properties
			Binder binder = Binder.get(environment);
			String cloudSqlPropertiesPrefix = GcpCloudSqlProperties.class.getAnnotation(ConfigurationProperties.class).value();
			String gcpPropertiesPrefix = GcpProperties.class.getAnnotation(ConfigurationProperties.class).value();
			GcpCloudSqlProperties sqlProperties = binder
					.bind(cloudSqlPropertiesPrefix, GcpCloudSqlProperties.class)
					.orElse(new GcpCloudSqlProperties());
			GcpProperties gcpProperties = binder
					.bind(cloudSqlPropertiesPrefix, GcpProperties.class)
					.orElse(new GcpProperties());

			CloudSqlJdbcInfoProvider cloudSqlJdbcInfoProvider = new DefaultCloudSqlJdbcInfoProvider(sqlProperties, databaseType);
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Default " + databaseType.name()
						+ " JdbcUrl provider. Connecting to "
						+ cloudSqlJdbcInfoProvider.getJdbcUrl() + " with driver "
						+ cloudSqlJdbcInfoProvider.getJdbcDriverClass());
			}

			// configure default JDBC driver and username as fallback values when not specified
			Map<String, Object> fallbackMap = new HashMap<>();
			fallbackMap.put("spring.datasource.username", databaseType.getDefaultUsername());
			fallbackMap.put("spring.datasource.driver-class-name", cloudSqlJdbcInfoProvider.getJdbcDriverClass());
			environment.getPropertySources()
					.addLast(new MapPropertySource("CLOUD_SQL_DATA_SOURCE_FALLBACK", fallbackMap));

			// always set the spring.datasource.url property in the environment
			Map<String, Object> primaryMap = new HashMap<>();
			primaryMap.put("spring.datasource.url", cloudSqlJdbcInfoProvider.getJdbcUrl());
			environment.getPropertySources()
					.addFirst(new MapPropertySource("CLOUD_SQL_DATA_SOURCE_URL", primaryMap));

			setCredentials(sqlProperties, gcpProperties);

			// support usage metrics
			CoreSocketFactory.setApplicationName("spring-cloud-gcp-sql/"
					+ this.getClass().getPackage().getImplementationVersion());
		}
	}

	private DatabaseType getEnabledDatabaseType(ConfigurableEnvironment environment) {
		if (Boolean.parseBoolean(environment.getProperty("spring.cloud.gcp.sql.enabled", "true"))
				&& isOnClasspath("javax.sql.DataSource")
				&& isOnClasspath("org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType")
				&& isOnClasspath("com.google.cloud.sql.CredentialFactory")) {
			if (isOnClasspath("com.google.cloud.sql.mysql.SocketFactory")
					&& isOnClasspath("com.mysql.cj.jdbc.Driver")) {
				return DatabaseType.MYSQL;
			}
			else if (isOnClasspath("com.google.cloud.sql.postgres.SocketFactory")
					&& isOnClasspath("org.postgresql.Driver")) {
				return DatabaseType.POSTGRESQL;
			}
		}
		return null;
	}

	private boolean isOnClasspath(String className) {
		return ClassUtils.isPresent(className, null);
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
	private void setCredentials(GcpCloudSqlProperties sqlProperties, GcpProperties gcpProperties) {
		Credentials credentials = null;

		// First tries the SQL configuration credential.
		if (sqlProperties.getCredentials().hasKey()) {
			credentials = sqlProperties.getCredentials();
		}
		// Then, the global credential.
		else {
			credentials = gcpProperties.getCredentials();
		}

		if (credentials.getEncodedKey() != null) {
			setCredentialsEncodedKeyProperty(credentials.getEncodedKey());
		}
		else if (credentials.getLocation() != null) {
			setCredentialsFileProperty(credentials.getLocation());
		}
		// Else do nothing, let sockets factory use application default credentials.
	}

	private void setCredentialsEncodedKeyProperty(String encodedKey) {
		System.setProperty(SqlCredentialFactory.CREDENTIAL_ENCODED_KEY_PROPERTY_NAME,
				encodedKey);

		System.setProperty(CredentialFactory.CREDENTIAL_FACTORY_PROPERTY,
				SqlCredentialFactory.class.getName());
	}


	private void setCredentialsFileProperty(Resource credentialsLocation) {
		try {
			// A resource might not be in the filesystem, but the Cloud SQL credential must.
			File credentialsLocationFile = credentialsLocation.getFile();

			System.setProperty(SqlCredentialFactory.CREDENTIAL_LOCATION_PROPERTY_NAME,
					credentialsLocationFile.getAbsolutePath());

			// If there are specified credentials, tell sockets factory to use them.
			System.setProperty(CredentialFactory.CREDENTIAL_FACTORY_PROPERTY,
					SqlCredentialFactory.class.getName());
		}
		catch (IOException ioe) {
			LOGGER.info("Error reading Cloud SQL credentials file.", ioe);
		}
	}

}
