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
import org.springframework.cloud.gcp.core.AppEngineCondition;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.autoconfig.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.sql.CloudSqlJdbcInfoProvider;
import org.springframework.cloud.gcp.sql.GcpCloudSqlProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Google Cloud SQL starter.
 *
 * <p>
 * It simplifies database configuration in that only an instance and database name are
 * provided, instead of a JDBC URL. If the instance region isn't specified, this starter
 * attempts to get it through the Cloud SQL API.
 *
 * @author João André Martins
 */
@Configuration
@EnableConfigurationProperties(GcpCloudSqlProperties.class)
@ConditionalOnClass({ GcpProjectIdProvider.class, SQLAdmin.class })
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
	@ConditionalOnMissingBean(CloudSqlJdbcInfoProvider.class)
	@Conditional(AppEngineCondition.class)
	public CloudSqlJdbcInfoProvider appengineCloudSqlJdbcInfoProvider(GcpProjectIdProvider projectIdProvider) {
		return new AppEngineCloudSqlJdbcInfoProvider(
				projectIdProvider.getProjectId(),
				this.gcpCloudSqlProperties);
	}

	@Bean
	@ConditionalOnMissingBean(CloudSqlJdbcInfoProvider.class)
	public CloudSqlJdbcInfoProvider cloudSqlJdbcInfoProvider(SQLAdmin sqlAdmin,
			GcpProjectIdProvider projectIdProvider) {
		return new DefaultCloudSqlJdbcInfoProvider(
				projectIdProvider.getProjectId(),
				sqlAdmin,
				this.gcpCloudSqlProperties);
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
		hikariConfig.setInitializationFailFast(this.gcpCloudSqlProperties.getInitFailFast());

		return new HikariDataSource(hikariConfig);
	}
}
