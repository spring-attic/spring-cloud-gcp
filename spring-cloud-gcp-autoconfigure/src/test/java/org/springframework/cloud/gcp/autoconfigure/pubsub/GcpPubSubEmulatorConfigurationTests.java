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

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;

/**
 * @author Andreas Berger
 */
public class GcpPubSubEmulatorConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.gcp.pubsub.emulatorHost=localhost:8085",
					"spring.cloud.gcp.projectId=test-project")
			.withConfiguration(AutoConfigurations.of(GcpPubSubEmulatorConfiguration.class,
					GcpContextAutoConfiguration.class,
					GcpPubSubAutoConfiguration.class));

	@Test
	public void testEmulatorConfig() {
		this.contextRunner.run(context -> {
			CredentialsProvider credentialsProvider = context.getBean(CredentialsProvider.class);
			Assert.assertTrue("CredentialsProvider is not correct",
					credentialsProvider instanceof NoCredentialsProvider);

			TransportChannelProvider transportChannelProvider = context.getBean(TransportChannelProvider.class);
			Assert.assertTrue("TransportChannelProvider is not correct",
					transportChannelProvider instanceof FixedTransportChannelProvider);
		});
	}

}
