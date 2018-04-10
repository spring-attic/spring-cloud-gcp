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

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubProperties;
import org.springframework.cloud.gcp.autoconfigure.spanner.GcpSpannerProperties;
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

					GcpTraceProperties traceProperties = context.getBean(GcpTraceProperties.class);
					assertThat(traceProperties.getProjectId())
							.isEqualTo("graphite-test-spring-cloud-gcp");
					assertThat(traceProperties.getCredentials().getEncodedKey())
					.isEqualTo(getPrivateKeyDataFromJson(vcapFileContents, "google-stackdriver-trace"));
				});
	}

	private String getPrivateKeyDataFromJson(String json, String serviceName) {
		JsonParser parser = JsonParserFactory.getJsonParser();
		Map<String, Object> vcapMap = parser.parseMap(json);
		return ((Map<String, String>) ((Map<String, Object>) ((List<Object>) vcapMap.get(serviceName)).get(0))
				.get("credentials")).get("PrivateKeyData");
	}

	@EnableConfigurationProperties({GcpPubSubProperties.class, GcpStorageProperties.class,
			GcpSpannerProperties.class, GcpTraceProperties.class})
	static class GcpCfEnvPPTestConfiguration {

	}
}
