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
import java.util.Collections;
import java.util.function.Supplier;

import javax.sql.DataSource;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.core.GcpProperties;
import org.springframework.cloud.gcp.sql.GcpCloudSqlProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Google Cloud SQL starter.
 *
 * <p>>It simplifies database configuration in that only an instance and database name are provided,
 * instead of a JDBC URL. If the instance region isn't specified, this starter attempts to get it
 * through the Cloud SQL API.
 *
 * @author João André Martins
 */
@Configuration
@EnableConfigurationProperties(GcpCloudSqlProperties.class)
@ConditionalOnClass({GcpProperties.class, GoogleCredential.class})
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class GcpCloudSqlAutoConfiguration {

	@Autowired
	private GcpCloudSqlProperties gcpCloudSqlProperties;

	@Autowired
	private GcpProperties gcpProperties;

	@Bean
	@ConditionalOnMissingBean(DataSource.class)
	public DataSource cloudSqlDataSource(SQLAdmin sqlAdmin)
			throws GeneralSecurityException, IOException {
		Assert.hasText(this.gcpCloudSqlProperties.getDatabaseName(),
				"spring.cloud.gcp.sql.databaseName name can't be empty.");
		Assert.hasText(this.gcpCloudSqlProperties.getInstanceName(),
				"spring.cloud.gcp.sql.instanceName name can't be empty.");
		Assert.hasText(this.gcpCloudSqlProperties.getUserName(),
				"spring.cloud.gcp.sql.userName name can't be null.");

		// Look for the region if the user didn't specify one.
		String region = this.gcpCloudSqlProperties.getRegion();
		if (!StringUtils.hasText(region)) {
			DatabaseInstance instance =	sqlAdmin.instances().get(
					this.gcpProperties.getProjectId(),
					this.gcpCloudSqlProperties.getInstanceName()).execute();

			region = instance.getRegion();
		}
		String instanceConnectionName = this.gcpProperties.getProjectId() + ":"
				+ region + ":" + this.gcpCloudSqlProperties.getInstanceName();

		String jdbcUrl = String.format("jdbc:mysql://google/%s?cloudSqlInstance=%s&"
						+ "socketFactory=com.google.cloud.sql.mysql.SocketFactory",
				this.gcpCloudSqlProperties.getDatabaseName(),
				instanceConnectionName);

		// TODO(joaomartins): Set the credential factory to be used by SocketFactory when
		// https://github.com/GoogleCloudPlatform/cloud-sql-jdbc-socket-factory/issues/41 is
		// resolved. For now, it uses application default creds.

		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl(jdbcUrl);
		hikariConfig.setUsername(this.gcpCloudSqlProperties.getUserName());
		hikariConfig.setPassword(this.gcpCloudSqlProperties.getPassword());

		return new HikariDataSource(hikariConfig);
	}

	@Bean
	@ConditionalOnMissingBean
	protected SQLAdmin createSqlAdminService(Supplier<GoogleCredential> credentialProvider)
			throws GeneralSecurityException, IOException {
		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

		GoogleCredential credential = credentialProvider.get();
		if (credential.createScopedRequired()) {
			credential = credential.createScoped(
					Collections.singleton("https://www.googleapis.com/auth/cloud-platform"));
		}

		return new SQLAdmin.Builder(httpTransport, jsonFactory, credential)
				.setApplicationName("Google-SQLAdminSample/0.1")
				.build();
	}
}
