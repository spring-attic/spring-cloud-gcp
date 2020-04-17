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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.cloud.gcp.core.Credentials;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.cloud.gcp.pubsub.integration.outbound.PubSubMessageHandler;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for Pub/Sub channel adapters.
 *
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Elena Felder
 */
public class PubSubChannelAdaptersIntegrationTests {

	private static final int RECEIVE_TIMEOUT_MS = 10000;

	static PubSubAdmin pubSubAdmin;

	/** Capture output for verification. */
	@Rule
	public OutputCaptureRule outputCaptureRule = new OutputCaptureRule();

	String topicName;

	String subscriptionName;

	ApplicationContextRunner contextRunner;

	@BeforeClass
	public static void enableTests() {
		assumeThat(System.getProperty("it.pubsub")).isEqualTo("true");
	}

	@BeforeClass
	public static void initializePubSubAdmin() throws Exception {
		pubSubAdmin = new PubSubAdmin(
				new DefaultGcpProjectIdProvider(),
				new DefaultCredentialsProvider(() -> new Credentials())
		);
	}

	@Before
	public void setUpPubSubResources() {
		this.topicName = "desafinado-" + UUID.randomUUID();
		this.subscriptionName = "doralice-" + UUID.randomUUID();

		if (pubSubAdmin.getTopic(this.topicName) == null) {
			pubSubAdmin.createTopic(this.topicName);
		}

		if (pubSubAdmin.getSubscription(this.subscriptionName) == null) {
			pubSubAdmin.createSubscription(this.subscriptionName, this.topicName, 10);
		}

		this.contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						GcpContextAutoConfiguration.class,
						GcpPubSubAutoConfiguration.class))
				.withBean("topicName", String.class, this.topicName)
				.withBean("subscriptionName", String.class, this.subscriptionName);
	}

	@After
	public void tearDownPubSubResources() {
		pubSubAdmin.deleteSubscription(this.subscriptionName);
		pubSubAdmin.deleteTopic(this.topicName);
	}

	@Test
	public void sendAndReceiveMessageAsString() {
		this.contextRunner
				.withUserConfiguration(PollableConfiguration.class, CommonConfiguration.class)
				.run((context) -> {
			Map<String, Object> headers = new HashMap<>();
			// Only String values for now..
			headers.put("storm", "lift your skinny fists");
			headers.put("static", "lift your skinny fists");
			headers.put("sleep", "lift your skinny fists");

			Message originalMessage = MessageBuilder.createMessage("I am a message (sendAndReceiveMessageAsString).".getBytes(),
					new MessageHeaders(headers));
			context.getBean("inputChannel", MessageChannel.class).send(originalMessage);

			Message<?> message =
					context.getBean("outputChannel", PollableChannel.class).receive(RECEIVE_TIMEOUT_MS);
			assertThat(message).isNotNull();
			assertThat(message.getPayload()).isInstanceOf(byte[].class);
			String payload = new String((byte[]) message.getPayload());
			assertThat(payload).isEqualTo("I am a message (sendAndReceiveMessageAsString).");

			assertThat(message.getHeaders().size()).isEqualTo(6);
			assertThat(message.getHeaders().get("storm")).isEqualTo("lift your skinny fists");
			assertThat(message.getHeaders().get("static")).isEqualTo("lift your skinny fists");
			assertThat(message.getHeaders().get("sleep")).isEqualTo("lift your skinny fists");
			assertThat(message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE)).isNotNull();
		});
	}

	@Test
	public void sendAndReceiveMessage() {
		this.contextRunner
				.withUserConfiguration(PollableConfiguration.class, CommonConfiguration.class)
				.run((context) -> {
				context.getBean("inputChannel", MessageChannel.class).send(
						MessageBuilder.withPayload("I am a message (sendAndReceiveMessage).".getBytes()).build());

				Message<?> message =
						context.getBean("outputChannel", PollableChannel.class).receive(RECEIVE_TIMEOUT_MS);
				assertThat(message).isNotNull();
				assertThat(message.getPayload()).isInstanceOf(byte[].class);
				String stringPayload = new String((byte[]) message.getPayload());
				assertThat(stringPayload).isEqualTo("I am a message (sendAndReceiveMessage).");
		});
	}

	@Test
	public void sendAndReceiveMessageManualAck() {
		this.contextRunner
				.withUserConfiguration(PollableConfiguration.class, CommonConfiguration.class)
				.run((context) -> {

				context.getBean(PubSubInboundChannelAdapter.class).setAckMode(AckMode.MANUAL);
				context.getBean("inputChannel", MessageChannel.class).send(
						MessageBuilder.withPayload("I am a message (sendAndReceiveMessageManualAck).".getBytes()).build());

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

		this.contextRunner
				.withUserConfiguration(SubscribableConfiguration.class, CommonConfiguration.class)
				.withPropertyValues("spring.cloud.gcp.pubsub.subscriber.max-ack-extension-period=0")
				.run((context) -> {
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
		});
	}

	@Test
	@SuppressWarnings("deprecation")
	public void sendAndReceiveMessageManualAckThroughAcknowledgementHeader() {
		this.contextRunner
				.withUserConfiguration(PollableConfiguration.class, CommonConfiguration.class)
				.run((context) -> {
				context.getBean(PubSubInboundChannelAdapter.class).setAckMode(AckMode.MANUAL);
				context.getBean("inputChannel", MessageChannel.class).send(
						MessageBuilder.withPayload("I am a message (sendAndReceiveMessageManualAckThroughAcknowledgementHeader).".getBytes()).build());

				PollableChannel channel = context.getBean("outputChannel", PollableChannel.class);

				Message<?> message = channel.receive(RECEIVE_TIMEOUT_MS);
				assertThat(message).isNotNull();
				AckReplyConsumer acker =
						(AckReplyConsumer) message.getHeaders().get(GcpPubSubHeaders.ACKNOWLEDGEMENT);
				assertThat(acker).isNotNull();
				acker.ack();

				message = channel.receive(RECEIVE_TIMEOUT_MS);
				assertThat(message).isNull();

				assertThat(this.outputCaptureRule.getOut()).contains("ACKNOWLEDGEMENT header is deprecated");
		});
	}

	@Test
	public void sendAndReceiveMessagePublishCallback() {
		this.contextRunner
				.withUserConfiguration(PollableConfiguration.class, CommonConfiguration.class)
				.run((context) -> {
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
						MessageBuilder.withPayload("I am a message (sendAndReceiveMessagePublishCallback).".getBytes()).build());

				Message<?> message =
						context.getBean("outputChannel", PollableChannel.class).receive(RECEIVE_TIMEOUT_MS);
				assertThat(message).isNotNull();
				Awaitility.await().atMost(1, TimeUnit.SECONDS)
						.untilAsserted(() -> verify(callbackSpy, times(1)).onSuccess(any()));
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

		@Bean
		public PubSubInboundChannelAdapter inboundChannelAdapter(
				PubSubTemplate pubSubTemplate,
				@Qualifier("outputChannel") MessageChannel outputChannel,
				@Qualifier("subscriptionName") String subscriptionName) {
			PubSubInboundChannelAdapter inboundChannelAdapter =
					new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
			inboundChannelAdapter.setOutputChannel(outputChannel);

			return inboundChannelAdapter;
		}

		@Bean
		@ServiceActivator(inputChannel = "inputChannel")
		public PubSubMessageHandler outboundChannelAdapter(
				PubSubTemplate pubSubTemplate, @Qualifier("topicName") String topicName) {
			return new PubSubMessageHandler(pubSubTemplate, topicName);
		}
	}
}
