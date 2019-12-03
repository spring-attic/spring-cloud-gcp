/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.cloudfoundry;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import io.pivotal.cfenv.test.AbstractCfEnvTests;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the Cloud Foundry environment post-processor.
 *
 * @author João André Martins
 * @author Chengyuan Zhao
 * @author Eddú Meléndez
 */
public class GcpCloudFoundryEnvironmentPostProcessorTests extends AbstractCfEnvTests {

	private GcpCloudFoundryEnvironmentPostProcessor initializer = new GcpCloudFoundryEnvironmentPostProcessor();

	private final ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();

	@Before
	public void setup() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"spring.cloud.gcp.sql.instance-connection-name=test-connection",
				"spring.cloud.gcp.sql.database-name=test-dbname",
				"spring.cloud.gcp.sql.credentials.encoded-key=test-key");
	}

	@Test
	public void testConfigurationProperties() throws IOException {
		String vcapFileContents = new String(Files.readAllBytes(
				new ClassPathResource("VCAP_SERVICES").getFile().toPath()));
		mockVcapServices(vcapFileContents);

		this.initializer.postProcessEnvironment(this.context.getEnvironment(), null);

		assertThat(getProperty("spring.cloud.gcp.pubsub.project-id")).isEqualTo("graphite-test-spring-cloud-gcp");
		assertThat(getProperty("spring.cloud.gcp.pubsub.credentials.encoded-key"))
				.isEqualTo(getPrivateKeyDataFromJson(vcapFileContents, "google-pubsub"));

		assertThat(getProperty("spring.cloud.gcp.storage.credentials.encoded-key"))
				.isEqualTo(getPrivateKeyDataFromJson(vcapFileContents, "google-storage"));

		assertThat(getProperty("spring.cloud.gcp.spanner.project-id"))
				.isEqualTo("graphite-test-spring-cloud-gcp");
		assertThat(getProperty("spring.cloud.gcp.spanner.credentials.encoded-key"))
				.isEqualTo(getPrivateKeyDataFromJson(vcapFileContents, "google-spanner"));
		assertThat(getProperty("spring.cloud.gcp.spanner.instance-id"))
				.isEqualTo("pcf-sb-7-1521579042901037743");

		assertThat(getProperty("spring.cloud.gcp.datastore.project-id"))
				.isEqualTo("graphite-test-spring-cloud-gcp");
		assertThat(getProperty("spring.cloud.gcp.datastore.credentials.encoded-key"))
				.isEqualTo(getPrivateKeyDataFromJson(vcapFileContents, "google-datastore"));

		assertThat(getProperty("spring.cloud.gcp.firestore.project-id"))
				.isEqualTo("pcf-dev-01-17031");
		assertThat(getProperty("spring.cloud.gcp.firestore.credentials.encoded-key"))
				.isEqualTo(getPrivateKeyDataFromJson(vcapFileContents, "google-firestore"));

		assertThat(getProperty("spring.cloud.gcp.bigquery.project-id"))
				.isEqualTo("pcf-dev-01-17031");
		assertThat(getProperty("spring.cloud.gcp.bigquery.credentials.encoded-key"))
				.isEqualTo(getPrivateKeyDataFromJson(vcapFileContents, "google-bigquery"));
		assertThat(getProperty("spring.cloud.gcp.bigquery.dataset-name")).isEqualTo("test_dataset");

		assertThat(getProperty("spring.cloud.gcp.trace.project-id"))
				.isEqualTo("graphite-test-spring-cloud-gcp");
		assertThat(getProperty("spring.cloud.gcp.trace.credentials.encoded-key"))
				.isEqualTo(getPrivateKeyDataFromJson(vcapFileContents, "google-stackdriver-trace"));

		assertThat(getProperty("spring.cloud.gcp.sql.credentials.encoded-key"))
				.isEqualTo(getPrivateKeyDataFromJson(vcapFileContents, "google-cloudsql-postgres"));
		assertThat(getProperty("spring.cloud.gcp.sql.instance-connection-name"))
				.isEqualTo("graphite-test-spring-cloud-gcp:us-central1:pcf-sb-3-1521233681947276465");
		assertThat(getProperty("spring.cloud.gcp.sql.database-name"))
				.isEqualTo("pcf-sb-4-1521234236513507790");

		assertThat(getProperty("spring.datasource.username"))
				.isEqualTo("fa6bb781-c76d-42");
		assertThat(getProperty("spring.datasource.password"))
				.isEqualTo("IxEQ63FRxSUSgoDWKbqEHmhY6D9h4nro1fja8lnP48s=");
	}

	@Test
	public void test2Sqls() throws IOException {
		String vcapFileContents = new String(Files.readAllBytes(
				new ClassPathResource("VCAP_SERVICES_2_SQL").getFile().toPath()));
		mockVcapServices(vcapFileContents);

		assertThat(getProperty("spring.cloud.gcp.sql.database-name"))
				.isEqualTo("test-dbname");
		assertThat(getProperty("spring.cloud.gcp.sql.instance-connection-name"))
				.isEqualTo("test-connection");
		assertThat(getProperty("spring.cloud.gcp.sql.credentials.encoded-key"))
				.isEqualTo("test-key");
	}

	private String getPrivateKeyDataFromJson(String json, String serviceName) {
		JsonParser parser = JsonParserFactory.getJsonParser();
		Map<String, Object> vcapMap = parser.parseMap(json);
		return ((Map<String, String>) ((Map<String, Object>) ((List<Object>) vcapMap.get(serviceName)).get(0))
				.get("credentials")).get("PrivateKeyData");
	}

	private String getProperty(String key) {
		return this.context.getEnvironment().getProperty(key);
	}

}
