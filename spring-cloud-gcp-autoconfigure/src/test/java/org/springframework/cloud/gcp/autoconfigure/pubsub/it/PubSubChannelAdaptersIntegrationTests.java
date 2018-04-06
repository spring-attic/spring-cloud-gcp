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

package org.springframework.cloud.gcp.autoconfigure.pubsub.it;

import java.util.HashMap;
import java.util.Map;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubEmulatorConfiguration;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.UsageTrackingHeaderProvider;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.cloud.gcp.pubsub.integration.outbound.PubSubMessageHandler;
import org.springframework.cloud.gcp.pubsub.support.DefaultPublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.DefaultSubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.concurrent.ListenableFutureCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author João André Martins
 */
public class PubSubChannelAdaptersIntegrationTests {

	private static final String EMULATOR_HOST_ENVVAR_NAME = "PUBSUB_EMULATOR_HOST";

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					GcpPubSubEmulatorConfiguration.class,
					GcpPubSubAutoConfiguration.class))
			.withUserConfiguration(
					PubSubChannelAdaptersIntegrationTests.IntegrationConfiguration.class)
			.withPropertyValues("spring.cloud.gcp.pubsub.emulatorHost=${PUBSUB_EMULATOR_HOST}");

	@BeforeClass
	public static void checkEmulatorIsRunning() {
		assumeThat(System.getenv(EMULATOR_HOST_ENVVAR_NAME)).isNotNull();
	}

	@Test
	public void sendAndReceiveMessage() {
		this.contextRunner.run(context -> {
			Map<String, Object> headers = new HashMap<>();
			// Only String values for now..
			headers.put("storm", "lift your skinny fists");
			headers.put("static", "lift your skinny fists");
			headers.put("sleep", "lift your skinny fists");

			context.getBean("inputChannel", MessageChannel.class).send(
					MessageBuilder.createMessage("I am a message.",  new MessageHeaders(headers)));

			Message<?> message =
					context.getBean("outputChannel", PollableChannel.class).receive(5000);
			assertThat(message).isNotNull();
			assertThat(message.getPayload()).isInstanceOf(String.class);
			String payload = (String) message.getPayload();
			assertThat(payload).isEqualTo("I am a message.");

			assertThat(message.getHeaders().size()).isEqualTo(6);
			assertThat(message.getHeaders().get("storm")).isEqualTo("lift your skinny fists");
			assertThat(message.getHeaders().get("static")).isEqualTo("lift your skinny fists");
			assertThat(message.getHeaders().get("sleep")).isEqualTo("lift your skinny fists");
		});
	}

	@Test
	public void sendAndReceiveMessageInBytes() {
		this.contextRunner.run(context -> {
			context.getBean(PubSubInboundChannelAdapter.class).setMessageConverter(null);
			context.getBean("inputChannel", MessageChannel.class).send(
					MessageBuilder.withPayload("I am a message.").build());

			Message<?> message =
					context.getBean("outputChannel", PollableChannel.class).receive(5000);
			assertThat(message).isNotNull();
			assertThat(message.getPayload()).isInstanceOf(byte[].class);
			String stringPayload = new String((byte[]) message.getPayload());
			assertThat(stringPayload).isEqualTo("I am a message.");
		});
	}

	@Test
	public void sendAndReceiveMessageManualAck() {
		this.contextRunner.run(context -> {
			context.getBean(PubSubInboundChannelAdapter.class).setAckMode(AckMode.MANUAL);
			context.getBean("inputChannel", MessageChannel.class).send(
					MessageBuilder.withPayload("I am a message.").build());

			PollableChannel channel = context.getBean("outputChannel", PollableChannel.class);

			Message<?> message = channel.receive(5000);
			assertThat(message).isNotNull();
			AckReplyConsumer acker =
					(AckReplyConsumer) message.getHeaders().get(GcpPubSubHeaders.ACKNOWLEDGEMENT);
			assertThat(acker).isNotNull();
			acker.nack();
			message = channel.receive(1000);
			assertThat(message).isNotNull();
			acker = (AckReplyConsumer) message.getHeaders().get(GcpPubSubHeaders.ACKNOWLEDGEMENT);
			assertThat(acker).isNotNull();
			acker.ack();
			message = channel.receive(1000);
			assertThat(message).isNull();
		});
	}

	@Test
	public void sendAndReceiveMessagePublishCallback() {
		this.contextRunner.run(context -> {
			ListenableFutureCallback<String> callbackSpy = Mockito.spy(new ListenableFutureCallback<String>() {
				@Override
				public void onFailure(Throwable ex) {

				}

				@Override
				public void onSuccess(String result) {

				}
			});
			context.getBean(PubSubMessageHandler.class).setPublishCallback(callbackSpy);
			context.getBean("inputChannel", MessageChannel.class).send(
					MessageBuilder.withPayload("I am a message.").build());

			Message<?> message =
					context.getBean("outputChannel", PollableChannel.class).receive(5000);
			assertThat(message).isNotNull();
			verify(callbackSpy, times(1)).onSuccess(any());
		});
	}

	@Configuration
	@EnableIntegration
	static class IntegrationConfiguration {

		@Autowired
		private PubSubTemplate pubSubTemplate;

		@Bean
		public PubSubInboundChannelAdapter inboundChannelAdapter(
				@Qualifier("outputChannel") MessageChannel outputChannel) {
			PubSubInboundChannelAdapter inboundChannelAdapter =
					new PubSubInboundChannelAdapter(this.pubSubTemplate, "doralice");
			inboundChannelAdapter.setOutputChannel(outputChannel);

			return inboundChannelAdapter;
		}

		@Bean
		@ServiceActivator(inputChannel = "inputChannel")
		public PubSubMessageHandler outboundChannelAdapter() {
			return new PubSubMessageHandler(this.pubSubTemplate, "desafinado");
		}

		@Bean
		public MessageChannel outputChannel() {
			return new QueueChannel();
		}

		@Bean
		public GcpProjectIdProvider gcpProjectIdProvider() {
			return () -> "bliss";
		}

		@Bean
		public CredentialsProvider credentialsProvider() {
			return new NoCredentialsProvider();
		}

		@Bean
		public SubscriberFactory defaultSubscriberFactory(
				@Qualifier("subscriberExecutorProvider") ExecutorProvider executorProvider,
				TransportChannelProvider transportChannelProvider,
				PubSubAdmin pubSubAdmin) {
			if (pubSubAdmin.getSubscription("doralice") == null) {
				pubSubAdmin.createSubscription("doralice", "desafinado");
			}

			DefaultSubscriberFactory factory = new DefaultSubscriberFactory(gcpProjectIdProvider());
			factory.setExecutorProvider(executorProvider);
			factory.setCredentialsProvider(credentialsProvider());
			factory.setHeaderProvider(
					new UsageTrackingHeaderProvider(GcpPubSubAutoConfiguration.class));
			factory.setChannelProvider(transportChannelProvider);

			return factory;
		}

		@Bean
		public PublisherFactory defaultPublisherFactory(
				@Qualifier("publisherExecutorProvider") ExecutorProvider executorProvider,
				TransportChannelProvider transportChannelProvider,
				PubSubAdmin pubSubAdmin) {
			if (pubSubAdmin.getTopic("desafinado") == null) {
				pubSubAdmin.createTopic("desafinado");
			}

			DefaultPublisherFactory factory = new DefaultPublisherFactory(gcpProjectIdProvider());
			factory.setExecutorProvider(executorProvider);
			factory.setCredentialsProvider(credentialsProvider());
			factory.setHeaderProvider(
					new UsageTrackingHeaderProvider(GcpPubSubAutoConfiguration.class));
			factory.setChannelProvider(transportChannelProvider);
			return factory;
		}
	}
}
