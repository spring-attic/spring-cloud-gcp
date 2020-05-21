/*
 * Copyright 2019-2019 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.pubsub;

import com.google.api.gax.grpc.GrpcStatusCode;
import com.google.api.gax.rpc.ApiException;
import io.grpc.Status.Code;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.pubsub.health.PubSubHealthIndicator;
import org.springframework.cloud.gcp.autoconfigure.pubsub.health.PubSubHealthIndicatorAutoConfiguration;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for the PubSub Health Indicator.
 *
 * @author Vinicius Carvalho
 */
@RunWith(MockitoJUnitRunner.class)
public class PubSubHealthIndicatorTests {

	@Mock
	private PubSubTemplate pubSubTemplate;

	@Test
	public void healthUpFor404() throws Exception {
		when(pubSubTemplate.pull(anyString(), anyInt(), anyBoolean())).thenThrow(new ApiException(
				new IllegalStateException("Illegal State"), GrpcStatusCode.of(io.grpc.Status.Code.NOT_FOUND), false));
		PubSubHealthIndicator healthIndicator = new PubSubHealthIndicator(pubSubTemplate);
		assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.UP);
	}

	@Test
	public void healthUpFor403() throws Exception {
		when(pubSubTemplate.pull(anyString(), anyInt(), anyBoolean())).thenThrow(new ApiException(
				new IllegalStateException("Illegal State"), GrpcStatusCode.of(Code.PERMISSION_DENIED), false));
		PubSubHealthIndicator healthIndicator = new PubSubHealthIndicator(pubSubTemplate);
		assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.UP);
	}

	@Test
	public void healthDown() {
		when(pubSubTemplate.pull(anyString(), anyInt(), anyBoolean()))
				.thenThrow(new ApiException(new IllegalStateException("Illegal State"),
						GrpcStatusCode.of(io.grpc.Status.Code.INVALID_ARGUMENT), false));
		PubSubHealthIndicator healthIndicator = new PubSubHealthIndicator(pubSubTemplate);
		assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
	}

	@Test
	public void healthDownGenericException() {
		when(pubSubTemplate.pull(anyString(), anyInt(), anyBoolean()))
				.thenThrow(new IllegalStateException("Illegal State"));
		PubSubHealthIndicator healthIndicator = new PubSubHealthIndicator(pubSubTemplate);
		assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
	}

	@Test
	public void healthIndicatorDisabledWhenPubSubTurnedOff() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						GcpPubSubAutoConfiguration.class, PubSubHealthIndicatorAutoConfiguration.class))
				.withPropertyValues("spring.cloud.gcp.datastore.project-id=test-project",
						"management.health.pubsub.enabled=true",
						"spring.cloud.gcp.pubsub.enabled=false");
		contextRunner.run(ctx -> {
			assertThat(ctx.getBeansOfType(PubSubHealthIndicator.class)).isEmpty();
		});
	}
}
