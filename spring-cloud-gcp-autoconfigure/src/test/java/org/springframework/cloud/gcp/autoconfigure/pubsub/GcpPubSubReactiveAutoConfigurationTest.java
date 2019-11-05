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

package org.springframework.cloud.gcp.autoconfigure.pubsub;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations;
import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberTemplate;
import org.springframework.cloud.gcp.pubsub.reactive.PubSubReactiveFactory;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class GcpPubSubReactiveAutoConfigurationTest {

	@Mock
	PubSubSubscriberTemplate mockSubscriberTemplate;

	@Mock
	AcknowledgeablePubsubMessage mockMessage;

	@Before
	public void setUpMocks() {
		this.mockSubscriberTemplate = Mockito.mock(PubSubSubscriberTemplate.class);
		this.mockMessage = Mockito.mock(AcknowledgeablePubsubMessage.class);

	}

	@Test
	public void defaultSchedulerUsedWhenNoneProvided() {

		setUpThreadPrefixVerification("pubSubReactiveScheduler");

		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withBean(PubSubSubscriberOperations.class, () -> mockSubscriberTemplate)
				.withConfiguration(AutoConfigurations.of(GcpPubSubReactiveAutoConfiguration.class));

		contextRunner.run(ctx -> {
			pollAndVerify(ctx);
		});

	}

	@Test
	public void customSchedulerUsedWhenAvailable() {

		setUpThreadPrefixVerification("myCustomScheduler");

		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withBean(PubSubSubscriberOperations.class, () -> mockSubscriberTemplate)
				.withConfiguration(AutoConfigurations.of(GcpPubSubReactiveAutoConfiguration.class))
				.withUserConfiguration(TestConfigWithOverriddenScheduler.class);

		contextRunner.run(ctx -> {
			pollAndVerify(ctx);
		});
	}

	private void setUpThreadPrefixVerification(String threadPrefix) {
		when(mockSubscriberTemplate.pull("testSubscription", 3, false))
				.then(arg -> {
					assertThat(Thread.currentThread().getName()).startsWith(threadPrefix);

					return Arrays.asList(mockMessage, mockMessage, mockMessage);
				});
	}

	private void pollAndVerify(ApplicationContext ctx) {
		PubSubReactiveFactory reactiveFactory = ctx.getBean(PubSubReactiveFactory.class);

		StepVerifier.create(
				reactiveFactory.poll("testSubscription", 2)
						.limitRequest(3))
				.expectNext(mockMessage, mockMessage, mockMessage)
				.verifyComplete();
	}


	@Configuration
	static class TestConfigWithOverriddenScheduler {
		@Bean
		@Qualifier("pubSubReactiveScheduler")
		public Scheduler customScheduler() {
			return Schedulers.newSingle("myCustomScheduler");
		}
	}

}
