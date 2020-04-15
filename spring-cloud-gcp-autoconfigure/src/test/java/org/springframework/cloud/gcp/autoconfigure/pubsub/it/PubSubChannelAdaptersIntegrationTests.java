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

package org.springframework.cloud.gcp.autoconfigure.pubsub.it;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import org.apache.commons.io.output.TeeOutputStream;
import org.awaitility.Awaitility;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.threeten.bp.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.UserAgentHeaderProvider;
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
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.concurrent.ListenableFutureCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for Pub/Sub channel adapters.
 *
 * @author João André Martins
 * @author Mike Eltsufin
 */
public class PubSubChannelAdaptersIntegrationTests {

	private static final int RECEIVE_TIMEOUT_MS = 10000;

	private static PrintStream systemOut;

	private static ByteArrayOutputStream baos;

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					GcpContextAutoConfiguration.class,
					GcpPubSubAutoConfiguration.class))
			.withUserConfiguration(PollableConfiguration.class, CommonConfiguration.class);

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
		this.contextRunner.run((context) -> {
			try {
				Map<String, Object> headers = new HashMap<>();
				// Only String values for now..
				headers.put("storm", "lift your skinny fists");
				headers.put("static", "lift your skinny fists");
				headers.put("sleep", "lift your skinny fists");

				Message originalMessage = MessageBuilder.createMessage("I am a message.".getBytes(),
						new MessageHeaders(headers));
				context.getBean("inputChannel", MessageChannel.class).send(originalMessage);

				Message<?> message =
						context.getBean("outputChannel", PollableChannel.class).receive(RECEIVE_TIMEOUT_MS);
				assertThat(message).isNotNull();
				assertThat(message.getPayload()).isInstanceOf(byte[].class);
				String payload = new String((byte[]) message.getPayload());
				assertThat(payload).isEqualTo("I am a message.");

				assertThat(message.getHeaders().size()).isEqualTo(6);
				assertThat(message.getHeaders().get("storm")).isEqualTo("lift your skinny fists");
				assertThat(message.getHeaders().get("static")).isEqualTo("lift your skinny fists");
				assertThat(message.getHeaders().get("sleep")).isEqualTo("lift your skinny fists");
				assertThat(message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE)).isNotNull();
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
		this.contextRunner.run((context) -> {
			try {
				context.getBean("inputChannel", MessageChannel.class).send(
						MessageBuilder.withPayload("I am a message.".getBytes()).build());

				Message<?> message =
						context.getBean("outputChannel", PollableChannel.class).receive(RECEIVE_TIMEOUT_MS);
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
		this.contextRunner.run((context) -> {
			try {
				context.getBean(PubSubInboundChannelAdapter.class).setAckMode(AckMode.MANUAL);
				context.getBean("inputChannel", MessageChannel.class).send(
						MessageBuilder.withPayload("I am a message.".getBytes()).build());

				PollableChannel channel = context.getBean("outputChannel", PollableChannel.class);

				Message<?> message = channel.receive(RECEIVE_TIMEOUT_MS);
				assertThat(message).isNotNull();
				BasicAcknowledgeablePubsubMessage origMessage =
						(BasicAcknowledgeablePubsubMessage) message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE);
				assertThat(origMessage).isNotNull();
				origMessage.nack();

				message = channel.receive(RECEIVE_TIMEOUT_MS);
				assertThat(message).isNotNull();
				origMessage = (BasicAcknowledgeablePubsubMessage)
						message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE);
				assertThat(origMessage).isNotNull();
				origMessage.ack();

				message = channel.receive(RECEIVE_TIMEOUT_MS);
				assertThat(message).isNull();
			}
			finally {
				PubSubAdmin pubSubAdmin = context.getBean(PubSubAdmin.class);
				pubSubAdmin.deleteSubscription((String) context.getBean("subscriptionName"));
				pubSubAdmin.deleteTopic((String) context.getBean("topicName"));
			}
		});
	}

	// If this test flakes, delete it.
	// It verifies that in AUTO_ACK mode, the message is neither acked nor nacked, and that
	// redelivery happens after subscription's ackDeadline passes.
	// There is also a client library bug (https://github.com/googleapis/java-pubsub/issues/141) that
	// results in ackDeadline being extended by 60 seconds even when maxAckExtensionPeriod is zero,
	// making minimum redelivery time is ackDeadline + 60.
	@Test
	public void sendAndReceiveMessageAutoAckWithFailure() {
		ApplicationContextRunner customContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						GcpContextAutoConfiguration.class,
						GcpPubSubAutoConfiguration.class))
				.withUserConfiguration(SubscribableConfiguration.class, CommonConfiguration.class);

		customContextRunner.run((context) -> {
			try {
				context.getBean(PubSubInboundChannelAdapter.class).setAckMode(AckMode.AUTO_ACK);
				context.getBean("inputChannel", MessageChannel.class).send(
						MessageBuilder.withPayload("This message is in trouble.".getBytes()).build());

				SubscribableChannel channel = context.getBean("outputChannel", SubscribableChannel.class);

				AtomicInteger numReceivedMessages = new AtomicInteger(0);
				channel.subscribe(msg -> {
					if (numReceivedMessages.incrementAndGet() == 1) {
						throw new RuntimeException("BOOM!");
					}
				});

				// wait for initial delivery
				Awaitility.await().atMost(10, TimeUnit.SECONDS)
						.until(() -> numReceivedMessages.get() > 0);
				assertThat(numReceivedMessages.get()).isEqualTo(1);

				// Expect redelivery after at least 10 seconds but within 1.5 minutes:
				// 10 seconds subscription ackDeadline
				// + 60 seconds https://github.com/googleapis/java-pubsub/issues/141
				// + 20 seconds anti-flake buffer
				Awaitility.await()
						.atLeast(9, TimeUnit.SECONDS)
						.atMost(90, TimeUnit.SECONDS)
						.until(() -> numReceivedMessages.get() > 1);
				assertThat(numReceivedMessages.get()).isEqualTo(2);
			}
			finally {
				context.getBean(PubSubInboundChannelAdapter.class).stop();
				PubSubAdmin pubSubAdmin = context.getBean(PubSubAdmin.class);
				pubSubAdmin.deleteSubscription((String) context.getBean("subscriptionName"));
				pubSubAdmin.deleteTopic((String) context.getBean("topicName"));
			}
		});
	}

	@Test
	@SuppressWarnings("deprecation")
	public void sendAndReceiveMessageManualAckThroughAcknowledgementHeader() {
		this.contextRunner.run((context) -> {
			try {
				context.getBean(PubSubInboundChannelAdapter.class).setAckMode(AckMode.MANUAL);
				context.getBean("inputChannel", MessageChannel.class).send(
						MessageBuilder.withPayload("I am a message.".getBytes()).build());

				PollableChannel channel = context.getBean("outputChannel", PollableChannel.class);

				Message<?> message = channel.receive(RECEIVE_TIMEOUT_MS);
				assertThat(message).isNotNull();
				AckReplyConsumer acker =
						(AckReplyConsumer) message.getHeaders().get(GcpPubSubHeaders.ACKNOWLEDGEMENT);
				assertThat(acker).isNotNull();
				acker.ack();

				message = channel.receive(RECEIVE_TIMEOUT_MS);
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
			catch (InterruptedException ex) {
				ex.printStackTrace();
				fail("Interrupted while waiting for text.");
			}
		}
		fail("Did not find expected text on STDOUT: " + expectedText);
	}

	@Test
	public void sendAndReceiveMessagePublishCallback() {
		this.contextRunner.run((context) -> {
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
						context.getBean("outputChannel", PollableChannel.class).receive(RECEIVE_TIMEOUT_MS);
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

	/**
	 * Spring Boot config for tests.
	 */
	@Configuration
	static class PollableConfiguration {

		@Bean
		public MessageChannel outputChannel() {
			return new QueueChannel();
		}
	}

	@Configuration
	static class SubscribableConfiguration {

		@Bean
		public MessageChannel outputChannel() {
			return new PublishSubscribeChannel();
		}
	}

	@Configuration
	@EnableIntegration
	static class CommonConfiguration {

		public String topicName = "desafinado-" + UUID.randomUUID();

		public String subscriptionName = "doralice-" + UUID.randomUUID();

		@Bean
		public PubSubInboundChannelAdapter inboundChannelAdapter(
				PubSubTemplate pubSubTemplate,
				@Qualifier("outputChannel") MessageChannel outputChannel) {
			PubSubInboundChannelAdapter inboundChannelAdapter =
					new PubSubInboundChannelAdapter(pubSubTemplate, this.subscriptionName);
			inboundChannelAdapter.setOutputChannel(outputChannel);

			return inboundChannelAdapter;
		}

		@Bean
		@ServiceActivator(inputChannel = "inputChannel")
		public PubSubMessageHandler outboundChannelAdapter(PubSubTemplate pubSubTemplate) {
			return new PubSubMessageHandler(pubSubTemplate, this.topicName);
		}

		@Bean
		public SubscriberFactory defaultSubscriberFactory(
				@Qualifier("subscriberExecutorProvider") ExecutorProvider executorProvider,
				TransportChannelProvider transportChannelProvider,
				PubSubAdmin pubSubAdmin,
				GcpProjectIdProvider projectIdProvider,
				CredentialsProvider credentialsProvider) {
			if (pubSubAdmin.getSubscription(this.subscriptionName) == null) {
				pubSubAdmin.createSubscription(this.subscriptionName, this.topicName, 10);
			}

			DefaultSubscriberFactory factory = new DefaultSubscriberFactory(projectIdProvider);
			factory.setExecutorProvider(executorProvider);
			factory.setCredentialsProvider(credentialsProvider);
			factory.setHeaderProvider(
					new UserAgentHeaderProvider(GcpPubSubAutoConfiguration.class));
			factory.setChannelProvider(transportChannelProvider);
			factory.setMaxAckExtensionPeriod(Duration.ZERO);

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
					new UserAgentHeaderProvider(GcpPubSubAutoConfiguration.class));
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
