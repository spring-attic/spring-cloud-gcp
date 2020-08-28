/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.stream.binder.pubsub;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.outbound.PubSubMessageHandler;
import org.springframework.cloud.gcp.stream.binder.pubsub.config.PubSubBinderConfiguration;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubConsumerProperties;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubExtendedBindingProperties;
import org.springframework.cloud.gcp.stream.binder.pubsub.provisioning.PubSubChannelProvisioner;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for channel binder.
 *
 * @author Mike Eltsufin
 * @author Elena Felder
 *
 * @since 1.1
 */
@RunWith(MockitoJUnitRunner.class)
public class PubSubMessageChannelBinderTests {

	PubSubMessageChannelBinder binder;

	@Mock
	PubSubChannelProvisioner channelProvisioner;

	@Mock
	PubSubTemplate pubSubTemplate;

	@Mock
	PubSubAdmin pubSubAdmin;

	@Mock
	PubSubExtendedBindingProperties properties;

	@Mock
	ConsumerDestination consumerDestination;

	@Mock
	ProducerDestination producerDestination;

	@Mock
	ExtendedConsumerProperties<PubSubConsumerProperties> consumerProperties;

	@Mock
	MessageChannel errorChannel;

	ApplicationContextRunner baseContext = new ApplicationContextRunner()
			.withBean(PubSubTemplate.class, () -> pubSubTemplate)
			.withBean(PubSubAdmin.class, () -> pubSubAdmin)
			.withConfiguration(
					AutoConfigurations.of(PubSubBinderConfiguration.class, PubSubExtendedBindingProperties.class));

	@Before
	public void before() {
		this.binder = new PubSubMessageChannelBinder(new String[0], this.channelProvisioner, this.pubSubTemplate,
				this.properties);

		when(producerDestination.getName()).thenReturn("test-topic");
	}

	@Test
	public void testAfterUnbindConsumer() {
		this.binder.afterUnbindConsumer(this.consumerDestination, "group1", this.consumerProperties);

		verify(this.channelProvisioner).afterUnbindConsumer(this.consumerDestination);
	}

	@Test
	public void producerSyncPropertyFalseByDefault() {
		baseContext
				.run(ctx -> {
					PubSubMessageChannelBinder binder = ctx.getBean(PubSubMessageChannelBinder.class);

					PubSubExtendedBindingProperties props = ctx.getBean("pubSubExtendedBindingProperties", PubSubExtendedBindingProperties.class);
					PubSubMessageHandler messageHandler = (PubSubMessageHandler) binder.createProducerMessageHandler(
							producerDestination,
							new ExtendedProducerProperties<>(props.getExtendedProducerProperties("test")),
							errorChannel
					);
					assertThat(messageHandler.isSync()).isFalse();
				});
	}

	@Test
	public void producerSyncPropertyPropagatesToMessageHandler() {
		baseContext
				.withPropertyValues("spring.cloud.stream.gcp.pubsub.default.producer.sync=true")
				.run(ctx -> {
					PubSubMessageChannelBinder binder = ctx.getBean(PubSubMessageChannelBinder.class);

					PubSubExtendedBindingProperties props = ctx.getBean("pubSubExtendedBindingProperties", PubSubExtendedBindingProperties.class);
					PubSubMessageHandler messageHandler = (PubSubMessageHandler) binder.createProducerMessageHandler(
							producerDestination,
							new ExtendedProducerProperties<>(props.getExtendedProducerProperties("test")),
							errorChannel
					);
					assertThat(messageHandler.isSync()).isTrue();
				});
	}

}
