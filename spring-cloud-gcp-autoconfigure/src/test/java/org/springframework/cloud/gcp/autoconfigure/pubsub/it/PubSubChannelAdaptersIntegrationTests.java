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

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubEmulatorConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubProperties;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.cloud.gcp.pubsub.integration.outbound.PubSubMessageHandler;
import org.springframework.cloud.gcp.pubsub.support.GcpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.concurrent.ListenableFutureCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author João André Martins
 */
@SpringBootTest(
		properties = {"spring.cloud.gcp.pubsub.emulatorHost=${PUBSUB_EMULATOR_HOST}"},
		classes = {PubSubChannelAdaptersIntegrationTests.IntegrationConfiguration.class,
				GcpPubSubEmulatorConfiguration.class,
				GcpPubSubAutoConfiguration.class
		}
)
@RunWith(SpringRunner.class)
public class PubSubChannelAdaptersIntegrationTests {

	private static final String EMULATOR_HOST_ENVVAR_NAME = "PUBSUB_EMULATOR_HOST";

	@Autowired
	@Qualifier("outputChannel")
	private PollableChannel channel;

	@Autowired
	private PubSubAdmin pubSubAdmin;

	@Autowired
	private GcpPubSubProperties pubSubProperties;

	@Autowired
	@Qualifier("inputChannel")
	private MessageChannel inputChannel;

	@Autowired
	private PubSubInboundChannelAdapter inboundChannelAdapter;

	@Autowired
	private PubSubMessageHandler outboundChannelAdapter;

	@BeforeClass
	public static void checkEmulatorIsRunning() {
		assumeThat(System.getenv(EMULATOR_HOST_ENVVAR_NAME)).isNotNull();
	}

	@Before
	public void setUp() {
		this.pubSubAdmin.createTopic("desafinado");
		this.pubSubAdmin.createSubscription("doralice", "desafinado");

		// Sets the defaults of the fields we'll change in later tests, so we don't need to use
		// @DirtiesContext, which is performance unfriendly.
		StringMessageConverter stringMessageConverter = new StringMessageConverter();
		stringMessageConverter.setSerializedPayloadClass(String.class);
		this.inboundChannelAdapter.setMessageConverter(stringMessageConverter);
		this.inboundChannelAdapter.setAckMode(AckMode.AUTO);
		this.outboundChannelAdapter.setPublishCallback(null);
	}

	@After
	public void tearDown() {
		this.pubSubAdmin.deleteTopic("desafinado");
		this.pubSubAdmin.deleteSubscription("doralice");
	}

	@Test
	public void sendAndReceiveMessage() {
		Map<String, Object> headers = new HashMap<>();
		// Only String values for now..
		headers.put("storm", "lift your skinny fists");
		headers.put("static", "lift your skinny fists");
		headers.put("sleep", "lift your skinny fists");

		this.inputChannel.send(
				MessageBuilder.createMessage("I am a message.",  new MessageHeaders(headers)));

		Message<?> message = this.channel.receive(5000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(String.class);
		String payload = (String) message.getPayload();
		assertThat(payload).isEqualTo("I am a message.");

		assertThat(message.getHeaders().size()).isEqualTo(6);
		assertThat(message.getHeaders().get("storm")).isEqualTo("lift your skinny fists");
		assertThat(message.getHeaders().get("static")).isEqualTo("lift your skinny fists");
		assertThat(message.getHeaders().get("sleep")).isEqualTo("lift your skinny fists");
	}

	@Test
	public void sendAndReceiveMessageInBytes() {
		this.inboundChannelAdapter.setMessageConverter(null);
		this.inputChannel.send(MessageBuilder.withPayload("I am a message.").build());

		Message<?> message = this.channel.receive(5000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(byte[].class);
		String stringPayload = new String((byte[]) message.getPayload());
		assertThat(stringPayload).isEqualTo("I am a message.");
	}

	@Test
	public void sendAndReceiveMessageManualAck() {
		this.inboundChannelAdapter.setAckMode(AckMode.MANUAL);
		this.inputChannel.send(MessageBuilder.withPayload("I am a message.").build());

		Message<?> message = this.channel.receive(5000);
		assertThat(message).isNotNull();
		AckReplyConsumer acker =
				(AckReplyConsumer) message.getHeaders().get(GcpHeaders.ACKNOWLEDGEMENT);
		assertThat(acker).isNotNull();
		acker.nack();
		message = this.channel.receive(1000);
		assertThat(message).isNotNull();
		acker = (AckReplyConsumer) message.getHeaders().get(GcpHeaders.ACKNOWLEDGEMENT);
		assertThat(acker).isNotNull();
		acker.ack();
		message = this.channel.receive(1000);
		assertThat(message).isNull();
	}

	@Test
	public void sendAndReceiveMessagePublishCallback() {
		ListenableFutureCallback<String> callbackSpy = Mockito.spy(new ListenableFutureCallback<String>() {
			@Override
			public void onFailure(Throwable ex) {

			}

			@Override
			public void onSuccess(String result) {

			}
		});
		this.outboundChannelAdapter.setPublishCallback(callbackSpy);
		this.inputChannel.send(MessageBuilder.withPayload("I am a message.").build());

		Message<?> message = this.channel.receive(5000);
		assertThat(message).isNotNull();
		verify(callbackSpy, times(1)).onSuccess(any());
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
	}
}
