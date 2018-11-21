/*
 *  Copyright 2018 original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.core.cloudfoundry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.api.client.util.ArrayMap;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.util.StringUtils;

/**
 * Converts GCP service broker metadata into Spring Cloud GCP configuration properties.
 *
 * @author João André Martins
 */
public class GcpCloudFoundryEnvironmentPostProcessor
		implements EnvironmentPostProcessor, Ordered {

	public static final String VCAP_SERVICES_ENVVAR = "VCAP_SERVICES";

	private static final Log LOGGER =
			LogFactory.getLog(GcpCloudFoundryEnvironmentPostProcessor.class);

	private static final String SPRING_CLOUD_GCP_PROPERTY_PREFIX = "spring.cloud.gcp.";

	private JsonParser parser = JsonParserFactory.getJsonParser();

	private int order = ConfigFileApplicationListener.DEFAULT_ORDER - 1;

	private static Map<String, String> sqlPropertyMap;

	static {
		sqlPropertyMap = new ArrayMap<>();
		sqlPropertyMap.put("ProjectId", "project-id");
		sqlPropertyMap.put("PrivateKeyData", "credentials.encoded-key");
		sqlPropertyMap.put("database_name", "database-name");
		sqlPropertyMap.put("region", "region");
		sqlPropertyMap.put("instance_name", "instance-name");
		sqlPropertyMap.put("Username", "username");
		sqlPropertyMap.put("Password", "password");
	}

	private enum GcpCfService {

		PUBSUB("google-pubsub", "pubsub",
				ImmutableMap.of("ProjectId", "project-id",
						"PrivateKeyData", "credentials.encoded-key")),
		STORAGE("google-storage", "storage",
				ImmutableMap.of("ProjectId", "project-id",
						"PrivateKeyData", "credentials.encoded-key")),
		SPANNER("google-spanner", "spanner",
				ImmutableMap.of("ProjectId", "project-id",
						"PrivateKeyData", "credentials.encoded-key",
						"instance_id", "instance-id")),
		DATASTORE("google-datastore", "datastore",
				ImmutableMap.of("ProjectId", "project-id",
						"PrivateKeyData", "credentials.encoded-key")),
		TRACE("google-stackdriver-trace", "trace",
				ImmutableMap.of("ProjectId", "project-id",
						"PrivateKeyData", "credentials.encoded-key")),
		MYSQL("google-cloudsql-mysql", "sql", sqlPropertyMap),
		POSTGRES("google-cloudsql-postgres", "sql", sqlPropertyMap);

		/**
		 * Name of the GCP Cloud Foundry service in the VCAP_SERVICES JSON.
		 */
		private String cfServiceName;

		/**
		 * Name of the Spring Cloud GCP property.
		 */
		private String gcpServiceName;

		/**
		 * Direct mapping of GCP service broker field names in VCAP_SERVICES JSON to Spring Cloud
		 * GCP property names. {@link #retrieveCfProperties(Map, String, String, Map)} uses this map
		 * to perform the actual transformation.
		 *
		 * <p>For instance, "ProjectId" for the "google-storage" service will map to
		 * "spring.cloud.gcp.storage.project-id" field.</p>
		 */
		private Map<String, String> cfPropNameToGcp;

		GcpCfService(String cfServiceName,
				String gcpServiceName,
				Map<String, String> cfPropNameToGcp) {
			this.cfServiceName = cfServiceName;
			this.gcpServiceName = gcpServiceName;
			this.cfPropNameToGcp = cfPropNameToGcp;
		}

		public String getCfServiceName() {
			return this.cfServiceName;
		}

		public Map<String, String> getCfPropNameToGcp() {
			return this.cfPropNameToGcp;
		}

		public String getGcpServiceName() {
			return this.gcpServiceName;
		}
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment,
			SpringApplication application) {
		if (!StringUtils.isEmpty(environment.getProperty(VCAP_SERVICES_ENVVAR))) {
			Map<String, Object> vcapMap =
					this.parser.parseMap(environment.getProperty(VCAP_SERVICES_ENVVAR));

			Properties gcpCfServiceProperties = new Properties();

			Set<GcpCfService> servicesToMap = new HashSet<>(Arrays.asList(GcpCfService.values()));
			if (vcapMap.containsKey(GcpCfService.MYSQL.getCfServiceName())
					&& vcapMap.containsKey(GcpCfService.POSTGRES.getCfServiceName())) {
				LOGGER.warn("Both MySQL and PostgreSQL bound to the app. "
						+ "Not configuring Cloud SQL.");
				servicesToMap.remove(GcpCfService.MYSQL);
				servicesToMap.remove(GcpCfService.POSTGRES);
			}

			servicesToMap.forEach(
					service -> gcpCfServiceProperties.putAll(
							retrieveCfProperties(
									vcapMap,
									service.getGcpServiceName(),
									service.getCfServiceName(),
									service.getCfPropNameToGcp())));

			// For Cloud SQL, there are some exceptions to the rule.
			// The instance connection name must be built from three fields.
			if (gcpCfServiceProperties.containsKey("spring.cloud.gcp.sql.instance-name")) {
				String instanceConnectionName =
						gcpCfServiceProperties.getProperty("spring.cloud.gcp.sql.project-id") + ":"
						+ gcpCfServiceProperties.getProperty("spring.cloud.gcp.sql.region") + ":"
						+ gcpCfServiceProperties.getProperty("spring.cloud.gcp.sql.instance-name");
				gcpCfServiceProperties.put("spring.cloud.gcp.sql.instance-connection-name",
						instanceConnectionName);
			}
			// The username and password should be in the generic DataSourceProperties.
			if (gcpCfServiceProperties.containsKey("spring.cloud.gcp.sql.username")) {
				gcpCfServiceProperties.put("spring.datasource.username",
						gcpCfServiceProperties.getProperty("spring.cloud.gcp.sql.username"));
			}
			if (gcpCfServiceProperties.containsKey("spring.cloud.gcp.sql.password")) {
				gcpCfServiceProperties.put("spring.datasource.password",
						gcpCfServiceProperties.getProperty("spring.cloud.gcp.sql.password"));
			}

			environment.getPropertySources()
					.addFirst(new PropertiesPropertySource("gcpCf", gcpCfServiceProperties));
		}
	}

	@SuppressWarnings("unchecked")
	private static Properties retrieveCfProperties(Map<String, Object> vcapMap,
			String gcpServiceName, String cfServiceName, Map<String, String> fieldsToMap) {
		Properties properties = new Properties();

		try {
			List<Object> serviceBindings = (List<Object>) vcapMap.get(cfServiceName);

			if (serviceBindings == null) {
				return properties;
			}

			if (serviceBindings.size() != 1) {
				LOGGER.warn("The service " + cfServiceName + " has to be bound to a "
						+ "Cloud Foundry application once and only once.");
				return properties;
			}

			Map<String, Object> serviceBinding = (Map<String, Object>) serviceBindings.get(0);
			Map<String, String> credentialsMap = (Map<String, String>) serviceBinding.get("credentials");
			String prefix = SPRING_CLOUD_GCP_PROPERTY_PREFIX + gcpServiceName + ".";
			fieldsToMap.forEach(
					(cfPropKey, gcpPropKey) -> properties.put(
							prefix + gcpPropKey,
							credentialsMap.get(cfPropKey)));
		}
		catch (ClassCastException e) {
			LOGGER.warn("Unexpected format of CF (VCAP) properties", e);
		}

		return properties;
	}

	@Override
	public int getOrder() {
		return this.order;
	}
}
