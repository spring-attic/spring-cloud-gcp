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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import org.apache.commons.io.output.TeeOutputStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.UsageTrackingHeaderProvider;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.cloud.gcp.pubsub.integration.outbound.PubSubMessageHandler;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
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
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author João André Martins
 * @author Mike Eltsufin
 */
public class PubSubChannelAdaptersIntegrationTests {
	private static PrintStream systemOut;

	private static ByteArrayOutputStream baos;

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					GcpContextAutoConfiguration.class,
					GcpPubSubAutoConfiguration.class))
			.withUserConfiguration(
					PubSubChannelAdaptersIntegrationTests.IntegrationConfiguration.class);

	@BeforeClass
	public static void enableTests() {
		assumeThat(System.getProperty("it.pubsub")).isEqualTo("true");
	}

	@BeforeClass
	public static void captureStdout() {
		systemOut = System.out;
		baos = new ByteArrayOutputStream();
		TeeOutputStream out = new TeeOutputStream(systemOut, baos);
		System.setOut(new PrintStream(out));
	}

	@AfterClass
	public static void resetStdout() {
		System.setOut(systemOut);
	}

	@Test
	public void sendAndReceiveMessageAsString() {
		this.contextRunner.run(context -> {
			try {
				Map<String, Object> headers = new HashMap<>();
				// Only String values for now..
				headers.put("storm", "lift your skinny fists");
				headers.put("static", "lift your skinny fists");
				headers.put("sleep", "lift your skinny fists");

				context.getBean("inputChannel", MessageChannel.class).send(
						MessageBuilder.createMessage("I am a message.".getBytes(),
								new MessageHeaders(headers)));

				Message<?> message =
						context.getBean("outputChannel", PollableChannel.class).receive(5000);
				assertThat(message).isNotNull();
				assertThat(message.getPayload()).isInstanceOf(byte[].class);
				String payload = new String((byte[]) message.getPayload());
				assertThat(payload).isEqualTo("I am a message.");

				assertThat(message.getHeaders().size()).isEqualTo(5);
				assertThat(message.getHeaders().get("storm")).isEqualTo("lift your skinny fists");
				assertThat(message.getHeaders().get("static")).isEqualTo("lift your skinny fists");
				assertThat(message.getHeaders().get("sleep")).isEqualTo("lift your skinny fists");
			}
			finally {
				PubSubAdmin pubSubAdmin = context.getBean(PubSubAdmin.class);
				pubSubAdmin.deleteSubscription((String) context.getBean("subscriptionName"));
				pubSubAdmin.deleteTopic((String) context.getBean("topicName"));
			}
		});
	}

	@Test
	public void sendAndReceiveMessage() {
		this.contextRunner.run(context -> {
			try {
				context.getBean("inputChannel", MessageChannel.class).send(
						MessageBuilder.withPayload("I am a message.".getBytes()).build());

				Message<?> message =
						context.getBean("outputChannel", PollableChannel.class).receive(5000);
				assertThat(message).isNotNull();
				assertThat(message.getPayload()).isInstanceOf(byte[].class);
				String stringPayload = new String((byte[]) message.getPayload());
				assertThat(stringPayload).isEqualTo("I am a message.");
			}
			finally {
				PubSubAdmin pubSubAdmin = context.getBean(PubSubAdmin.class);
				pubSubAdmin.deleteSubscription((String) context.getBean("subscriptionName"));
				pubSubAdmin.deleteTopic((String) context.getBean("topicName"));
			}
		});
	}

	@Test
	public void sendAndReceiveMessageManualAck() {
		this.contextRunner.run(context -> {
			try {
				context.getBean(PubSubInboundChannelAdapter.class).setAckMode(AckMode.MANUAL);
				context.getBean("inputChannel", MessageChannel.class).send(
						MessageBuilder.withPayload("I am a message.".getBytes()).build());

				PollableChannel channel = context.getBean("outputChannel", PollableChannel.class);

				Message<?> message = channel.receive(10000);
				assertThat(message).isNotNull();
				BasicAcknowledgeablePubsubMessage origMessage =
						(BasicAcknowledgeablePubsubMessage) message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE);
				assertThat(origMessage).isNotNull();
				origMessage.nack();

				message = channel.receive(10000);
				assertThat(message).isNotNull();
				origMessage = (BasicAcknowledgeablePubsubMessage)
						message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE);
				assertThat(origMessage).isNotNull();
				origMessage.ack();

				message = channel.receive(10000);
				assertThat(message).isNull();
			}
			finally {
				PubSubAdmin pubSubAdmin = context.getBean(PubSubAdmin.class);
				pubSubAdmin.deleteSubscription((String) context.getBean("subscriptionName"));
				pubSubAdmin.deleteTopic((String) context.getBean("topicName"));
			}
		});
	}

	@Test
	@SuppressWarnings("deprecation")
	public void sendAndReceiveMessageManualAckThroughAcknowledgementHeader() {
		this.contextRunner.run(context -> {
			try {
				context.getBean(PubSubInboundChannelAdapter.class).setAckMode(AckMode.MANUAL);
				context.getBean("inputChannel", MessageChannel.class).send(
						MessageBuilder.withPayload("I am a message.".getBytes()).build());

				PollableChannel channel = context.getBean("outputChannel", PollableChannel.class);

				Message<?> message = channel.receive(10000);
				assertThat(message).isNotNull();
				AckReplyConsumer acker =
						(AckReplyConsumer) message.getHeaders().get(GcpPubSubHeaders.ACKNOWLEDGEMENT);
				assertThat(acker).isNotNull();
				acker.ack();

				message = channel.receive(10000);
				assertThat(message).isNull();

				validateOutput("ACKNOWLEDGEMENT header is deprecated");
			}
			finally {
				PubSubAdmin pubSubAdmin = context.getBean(PubSubAdmin.class);
				pubSubAdmin.deleteSubscription((String) context.getBean("subscriptionName"));
				pubSubAdmin.deleteTopic((String) context.getBean("topicName"));
			}
		});
	}

	private void validateOutput(String expectedText) {
		for (int i = 0; i < 100; i++) {
			if (baos.toString().contains(expectedText)) {
				return;
			}
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				fail("Interrupted while waiting for text.");
			}
		}
		fail("Did not find expected text on STDOUT: " + expectedText);
	}

	@Test
	public void sendAndReceiveMessagePublishCallback() {
		this.contextRunner.run(context -> {
			try {
				ListenableFutureCallback<String> callbackSpy = Mockito.spy(
						new ListenableFutureCallback<String>() {
							@Override
							public void onFailure(Throwable ex) {

							}

							@Override
							public void onSuccess(String result) {

							}
						});
				context.getBean(PubSubMessageHandler.class).setPublishCallback(callbackSpy);
				context.getBean("inputChannel", MessageChannel.class).send(
						MessageBuilder.withPayload("I am a message.".getBytes()).build());

				Message<?> message =
						context.getBean("outputChannel", PollableChannel.class).receive(5000);
				assertThat(message).isNotNull();
				verify(callbackSpy, times(1)).onSuccess(any());
			}
			finally {
				PubSubAdmin pubSubAdmin = context.getBean(PubSubAdmin.class);
				pubSubAdmin.deleteSubscription((String) context.getBean("subscriptionName"));
				pubSubAdmin.deleteTopic((String) context.getBean("topicName"));
			}
		});
	}

	@Configuration
	@EnableIntegration
	static class IntegrationConfiguration {

		public String topicName = "desafinado-" + UUID.randomUUID();

		public String subscriptionName = "doralice-" + UUID.randomUUID();

		@Autowired
		private PubSubTemplate pubSubTemplate;

		@Bean
		public PubSubInboundChannelAdapter inboundChannelAdapter(
				@Qualifier("outputChannel") MessageChannel outputChannel) {
			PubSubInboundChannelAdapter inboundChannelAdapter =
					new PubSubInboundChannelAdapter(this.pubSubTemplate, this.subscriptionName);
			inboundChannelAdapter.setOutputChannel(outputChannel);

			return inboundChannelAdapter;
		}

		@Bean
		@ServiceActivator(inputChannel = "inputChannel")
		public PubSubMessageHandler outboundChannelAdapter() {
			return new PubSubMessageHandler(this.pubSubTemplate, this.topicName);
		}

		@Bean
		public MessageChannel outputChannel() {
			return new QueueChannel();
		}

		@Bean
		public SubscriberFactory defaultSubscriberFactory(
				@Qualifier("subscriberExecutorProvider") ExecutorProvider executorProvider,
				TransportChannelProvider transportChannelProvider,
				PubSubAdmin pubSubAdmin,
				GcpProjectIdProvider projectIdProvider,
				CredentialsProvider credentialsProvider) {
			if (pubSubAdmin.getSubscription(this.subscriptionName) == null) {
				pubSubAdmin.createSubscription(this.subscriptionName, this.topicName);
			}

			DefaultSubscriberFactory factory = new DefaultSubscriberFactory(projectIdProvider);
			factory.setExecutorProvider(executorProvider);
			factory.setCredentialsProvider(credentialsProvider);
			factory.setHeaderProvider(
					new UsageTrackingHeaderProvider(GcpPubSubAutoConfiguration.class));
			factory.setChannelProvider(transportChannelProvider);

			return factory;
		}

		@Bean
		public PublisherFactory defaultPublisherFactory(
				@Qualifier("publisherExecutorProvider") ExecutorProvider executorProvider,
				TransportChannelProvider transportChannelProvider,
				PubSubAdmin pubSubAdmin,
				GcpProjectIdProvider projectIdProvider,
				CredentialsProvider credentialsProvider) {
			if (pubSubAdmin.getTopic(this.topicName) == null) {
				pubSubAdmin.createTopic(this.topicName);
			}

			DefaultPublisherFactory factory = new DefaultPublisherFactory(projectIdProvider);
			factory.setExecutorProvider(executorProvider);
			factory.setCredentialsProvider(credentialsProvider);
			factory.setHeaderProvider(
					new UsageTrackingHeaderProvider(GcpPubSubAutoConfiguration.class));
			factory.setChannelProvider(transportChannelProvider);
			return factory;
		}

		@Bean
		public String topicName() {
			return this.topicName;
		}

		@Bean
		public String subscriptionName() {
			return this.subscriptionName;
		}
	}
}
