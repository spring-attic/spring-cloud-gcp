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

import java.util.List;
import java.util.Map;
import java.util.Properties;

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

	private enum GcpCfService {

		pubsub("google-pubsub",
				ImmutableMap.of("ProjectId", "project-id",
						"PrivateKeyData", "credentials.encoded-key")),
		storage("google-storage",
				ImmutableMap.of("ProjectId", "project-id",
						"PrivateKeyData", "credentials.encoded-key")),
		spanner("google-spanner",
				ImmutableMap.of("ProjectId", "project-id",
						"PrivateKeyData", "credentials.encoded-key",
						"instance_id", "instance-id")),
		trace("google-stackdriver-trace",
				ImmutableMap.of("ProjectId", "project-id",
						"PrivateKeyData", "credentials.encoded-key"));

		/**
		 * Name of the GCP Cloud Foundry service in the VCAP_SERVICES JSON.
		 */
		private String cfServiceName;

		/**
		 * Direct mapping of GCP service broker field names in VCAP_SERVICES JSON to Spring Cloud
		 * GCP property names. {@link #retrieveCfProperties(Map, GcpCfService)} uses this map to
		 * perform the actual transformation.
		 *
		 * <p>For instance, "ProjectId" for the "google-storage" service will map to
		 * "spring.cloud.gcp.storage.project-id" field.</p>
		 */
		private Map<String, String> cfPropNameToGcp;

		GcpCfService(String cfServiceName,
				Map<String, String> cfPropNameToGcp) {
			this.cfServiceName = cfServiceName;
			this.cfPropNameToGcp = cfPropNameToGcp;
		}

		public String getCfServiceName() {
			return this.cfServiceName;
		}

		public Map<String, String> getCfPropNameToGcp() {
			return this.cfPropNameToGcp;
		}
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment,
			SpringApplication application) {
		if (!StringUtils.isEmpty(environment.getProperty(VCAP_SERVICES_ENVVAR))) {
			Map<String, Object> vcapMap =
					this.parser.parseMap(environment.getProperty(VCAP_SERVICES_ENVVAR));

			Properties gcpCfServiceProperties = new Properties();

			for (GcpCfService cfService : GcpCfService.values()) {
				gcpCfServiceProperties.putAll(retrieveCfProperties(vcapMap, cfService));
			}

			environment.getPropertySources()
					.addFirst(new PropertiesPropertySource("gcpCf", gcpCfServiceProperties));
		}
	}

	private static Properties retrieveCfProperties(Map<String, Object> vcapMap,
			GcpCfService service) {
		Properties properties = new Properties();
		List<Object> serviceBindings = (List<Object>) vcapMap.get(service.getCfServiceName());

		if (serviceBindings == null) {
			return properties;
		}

		if (serviceBindings.size() != 1) {
			LOGGER.warn("The service " + service.getCfServiceName() + " has to be bound to a "
					+ "Cloud Foundry application once and only once.");
			return properties;
		}

		Map<String, Object> serviceBinding = (Map<String, Object>) serviceBindings.get(0);
		Map<String, String> credentialsMap = (Map<String, String>) serviceBinding.get("credentials");
		String prefix = SPRING_CLOUD_GCP_PROPERTY_PREFIX + service.name() + ".";
		service.getCfPropNameToGcp().forEach(
				(cfPropKey, gcpPropKey) -> properties.put(
						prefix + gcpPropKey,
						credentialsMap.get(cfPropKey)));

		return properties;
	}

	@Override
	public int getOrder() {
		return this.order;
	}
}
