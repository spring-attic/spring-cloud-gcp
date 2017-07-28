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
import java.security.GeneralSecurityException;

import javax.sql.DataSource;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.auth.http.HttpCredentialsAdapter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.autoconfig.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.sql.CloudSqlJdbcUrlProvider;
import org.springframework.cloud.gcp.sql.GcpCloudSqlProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Google Cloud SQL starter.
 *
 * <p>It simplifies database configuration in that only an instance and database name are provided,
 * instead of a JDBC URL. If the instance region isn't specified, this starter attempts to get it
 * through the Cloud SQL API.
 *
 * @author João André Martins
 */
@Configuration
@EnableConfigurationProperties(GcpCloudSqlProperties.class)
@ConditionalOnClass({GcpProjectIdProvider.class, SQLAdmin.class})
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@AutoConfigureAfter(GcpContextAutoConfiguration.class)
public class GcpCloudSqlAutoConfiguration {

	@Autowired
	private GcpCloudSqlProperties gcpCloudSqlProperties;

	@Bean
	@ConditionalOnMissingBean
	protected SQLAdmin sqlAdminService(CredentialsProvider credentialsProvider)
			throws GeneralSecurityException, IOException {
		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

		return new SQLAdmin.Builder(httpTransport, jsonFactory,
				new HttpCredentialsAdapter(credentialsProvider.getCredentials()))
				.build();
	}

	@Bean
	@ConditionalOnMissingBean(CloudSqlJdbcUrlProvider.class)
	public CloudSqlJdbcUrlProvider cloudSqlJdbcUrlProvider(SQLAdmin sqlAdmin,
			GcpProjectIdProvider projectIdProvider)
			throws IOException, ClassNotFoundException {
		Assert.hasText(projectIdProvider.getProjectId(),
				"A project ID must be provided.");

		// TODO(joaomartins): Set the credential factory to be used by SocketFactory when
		// https://github.com/GoogleCloudPlatform/cloud-sql-jdbc-socket-factory/issues/41 is
		// resolved. For now, it uses application default creds.

		// Load MySQL driver to register it, so the user doesn't have to.
		Class.forName("com.mysql.jdbc.Driver");

		// Look for the region if the user didn't specify one.
		String region = this.gcpCloudSqlProperties.getRegion();
		if (!StringUtils.hasText(region)) {
			region = sqlAdmin.instances().get(projectIdProvider.getProjectId(),
					this.gcpCloudSqlProperties.getInstanceName()).execute().getRegion();
		}

		String instanceConnectionName = projectIdProvider.getProjectId() + ":" + region + ":"
				+ this.gcpCloudSqlProperties.getInstanceName();

		return () -> String.format("jdbc:mysql://google/%s?cloudSqlInstance=%s&"
						+ "socketFactory=com.google.cloud.sql.mysql.SocketFactory",
				this.gcpCloudSqlProperties.getDatabaseName(),
				instanceConnectionName);
	}

	@Bean
	@ConditionalOnMissingBean(DataSource.class)
	public DataSource cloudSqlDataSource(CloudSqlJdbcUrlProvider cloudSqlJdbcUrlProvider)
			throws GeneralSecurityException, IOException {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl(cloudSqlJdbcUrlProvider.getJdbcUrl());
		hikariConfig.setUsername(this.gcpCloudSqlProperties.getUserName());
		hikariConfig.setPassword(this.gcpCloudSqlProperties.getPassword());
		// Setting this is useful to disable connection initialization. Especially in unit tests.
		hikariConfig.setInitializationFailFast(this.gcpCloudSqlProperties.getInitFailFast());

		return new HikariDataSource(hikariConfig);
	}
}
