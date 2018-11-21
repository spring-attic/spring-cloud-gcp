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

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.datastore.GcpDatastoreProperties;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubProperties;
import org.springframework.cloud.gcp.autoconfigure.spanner.GcpSpannerProperties;
import org.springframework.cloud.gcp.autoconfigure.sql.GcpCloudSqlProperties;
import org.springframework.cloud.gcp.autoconfigure.storage.GcpStorageProperties;
import org.springframework.cloud.gcp.autoconfigure.trace.GcpTraceProperties;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author João André Martins
 */
public class GcpCloudFoundryEnvironmentPostProcessorTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withInitializer(context ->
					new GcpCloudFoundryEnvironmentPostProcessor()
							.postProcessEnvironment(context.getEnvironment(), null))
			.withSystemProperties(new String[] {
					"spring.cloud.gcp.sql.instance-connection-name=test-connection",
					"spring.cloud.gcp.sql.database-name=test-dbname",
					"spring.cloud.gcp.sql.credentials.encoded-key=test-key"
			})
			.withUserConfiguration(GcpCfEnvPPTestConfiguration.class);

	@Test
	public void testConfigurationProperties() throws IOException {
		String vcapFileContents = new String(Files.readAllBytes(
				new ClassPathResource("VCAP_SERVICES").getFile().toPath()));
		this.contextRunner.withSystemProperties("VCAP_SERVICES=" + vcapFileContents)
				.run(context -> {
					GcpPubSubProperties pubSubProperties =
							context.getBean(GcpPubSubProperties.class);
					assertThat(pubSubProperties.getProjectId())
							.isEqualTo("graphite-test-spring-cloud-gcp");
					assertThat(pubSubProperties.getCredentials().getEncodedKey())
					.isEqualTo(getPrivateKeyDataFromJson(vcapFileContents, "google-pubsub"));

					GcpStorageProperties storageProperties =
							context.getBean(GcpStorageProperties.class);
					assertThat(storageProperties.getCredentials().getEncodedKey())
					.isEqualTo(getPrivateKeyDataFromJson(vcapFileContents, "google-storage"));

					GcpSpannerProperties spannerProperties =
							context.getBean(GcpSpannerProperties.class);
					assertThat(spannerProperties.getProjectId())
							.isEqualTo("graphite-test-spring-cloud-gcp");
					assertThat(spannerProperties.getCredentials().getEncodedKey())
							.isEqualTo(getPrivateKeyDataFromJson(vcapFileContents, "google-spanner"));
					assertThat(spannerProperties.getInstanceId())
							.isEqualTo("pcf-sb-7-1521579042901037743");

					GcpDatastoreProperties datastoreProperties =
							context.getBean(GcpDatastoreProperties.class);
					assertThat(datastoreProperties.getProjectId())
							.isEqualTo("graphite-test-spring-cloud-gcp");
					assertThat(datastoreProperties.getCredentials().getEncodedKey())
							.isEqualTo(getPrivateKeyDataFromJson(vcapFileContents, "google-datastore"));

					GcpTraceProperties traceProperties = context.getBean(GcpTraceProperties.class);
					assertThat(traceProperties.getProjectId())
							.isEqualTo("graphite-test-spring-cloud-gcp");
					assertThat(traceProperties.getCredentials().getEncodedKey())
							.isEqualTo(getPrivateKeyDataFromJson(vcapFileContents, "google-stackdriver-trace"));

					GcpCloudSqlProperties sqlProperties = context.getBean(GcpCloudSqlProperties.class);
					assertThat(sqlProperties.getCredentials().getEncodedKey())
							.isEqualTo(getPrivateKeyDataFromJson(vcapFileContents, "google-cloudsql-postgres"));
					assertThat(sqlProperties.getInstanceConnectionName())
							.isEqualTo("graphite-test-spring-cloud-gcp:us-central1:pcf-sb-3-1521233681947276465");
					assertThat(sqlProperties.getDatabaseName())
							.isEqualTo("pcf-sb-4-1521234236513507790");

					DataSourceProperties dataSourceProperties = context.getBean(DataSourceProperties.class);
					assertThat(dataSourceProperties.getUsername()).isEqualTo("fa6bb781-c76d-42");
					assertThat(dataSourceProperties.getPassword())
							.isEqualTo("IxEQ63FRxSUSgoDWKbqEHmhY6D9h4nro1fja8lnP48s=");
				});
	}

	@Test
	public void test2Sqls() throws IOException {
		String vcapFileContents = new String(Files.readAllBytes(
				new ClassPathResource("VCAP_SERVICES_2_SQL").getFile().toPath()));
		this.contextRunner.withSystemProperties("VCAP_SERVICES=" + vcapFileContents)
				.run(context -> {
					GcpCloudSqlProperties sqlProperties =
							context.getBean(GcpCloudSqlProperties.class);

					//Both mysql and postgres settings present in VCAP_SERVICES_2_SQL file,
					//so db name and connection name should not change
					assertThat(sqlProperties.getDatabaseName()).isEqualTo("test-dbname");
					assertThat(sqlProperties.getInstanceConnectionName()).isEqualTo("test-connection");
					assertThat(sqlProperties.getCredentials().getEncodedKey()).isEqualTo("test-key");
				});
	}

	private String getPrivateKeyDataFromJson(String json, String serviceName) {
		JsonParser parser = JsonParserFactory.getJsonParser();
		Map<String, Object> vcapMap = parser.parseMap(json);
		return ((Map<String, String>) ((Map<String, Object>) ((List<Object>) vcapMap.get(serviceName)).get(0))
				.get("credentials")).get("PrivateKeyData");
	}

	@EnableConfigurationProperties({ GcpPubSubProperties.class, GcpStorageProperties.class,
			GcpSpannerProperties.class, GcpDatastoreProperties.class, GcpTraceProperties.class,
			GcpCloudSqlProperties.class, DataSourceProperties.class })
	static class GcpCfEnvPPTestConfiguration {

	}
}
