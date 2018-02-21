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

package org.springframework.cloud.gcp.autoconfigure.pubsub;

import java.io.IOException;
import java.nio.file.Files;

import com.google.auth.oauth2.ServiceAccountCredentials;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author João André Martins
 */
public class GcpPubSubAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GcpPubSubAutoConfiguration.class,
					GcpContextAutoConfiguration.class))
			.withPropertyValues("spring.cloud.gcp.project-id=freebird");

	@Test
	public void testCfPubSubCredentials() throws IOException {
		Resource vcapServicesFile = new ClassPathResource("VCAP_SERVICES");
		String vcapServicesString =
				new String(Files.readAllBytes(vcapServicesFile.getFile().toPath()));
		this.contextRunner.withSystemProperties("VCAP_SERVICES=" + vcapServicesString)
				.run(context -> {
					GcpPubSubAutoConfiguration pubSubAutoConfiguration =
							context.getBean(GcpPubSubAutoConfiguration.class);
					ServiceAccountCredentials credentials =
							(ServiceAccountCredentials) pubSubAutoConfiguration
									.getCredentialsProvider().getCredentials();
					assertThat(credentials.getClientEmail()).isEqualTo(
							"pcf-binding-3352ec74@graphite-test-spring-cloud-gcp.iam."
									+ "gserviceaccount.com");
				});
	}
}
