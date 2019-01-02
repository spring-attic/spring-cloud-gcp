/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.autoconfigure.pubsub;

import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.batching.FlowController.LimitExceededBehavior;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import org.assertj.core.data.Offset;
import org.junit.Test;
import org.threeten.bp.Duration;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the Pub/Sub emulator config.
 *
 * @author Andreas Berger
 * @author João André Martins
 * @author Chengyuan Zhao
 */
public class GcpPubSubEmulatorConfigurationTests {

	private static final Offset<Double> DELTA = Offset.offset(0.0001);

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.gcp.pubsub.emulator-host=localhost:8085",
					"spring.cloud.gcp.projectId=test-project",
					"spring.cloud.gcp.pubsub.subscriber.pull-endpoint=fake-endpoint",
					"spring.cloud.gcp.pubsub.subscriber.parallel-pull-count=333",
					"spring.cloud.gcp.pubsub.subscriber.retry.total-timeout-seconds=1",
					"spring.cloud.gcp.pubsub.subscriber.retry.initial-retry-delay-seconds=2",
					"spring.cloud.gcp.pubsub.subscriber.retry.retry-delay-multiplier=3",
					"spring.cloud.gcp.pubsub.subscriber.retry.max-retry-delay-seconds=4",
					"spring.cloud.gcp.pubsub.subscriber.retry.max-attempts=5",
					"spring.cloud.gcp.pubsub.subscriber.retry.jittered=true",
					"spring.cloud.gcp.pubsub.subscriber.retry.initial-rpc-timeout-seconds=6",
					"spring.cloud.gcp.pubsub.subscriber.retry.rpc-timeout-multiplier=7",
					"spring.cloud.gcp.pubsub.subscriber.retry.max-rpc-timeout-seconds=8",
					"spring.cloud.gcp.pubsub.publisher.retry.total-timeout-seconds=9",
					"spring.cloud.gcp.pubsub.publisher.retry.initial-retry-delay-seconds=10",
					"spring.cloud.gcp.pubsub.publisher.retry.retry-delay-multiplier=11",
					"spring.cloud.gcp.pubsub.publisher.retry.max-retry-delay-seconds=12",
					"spring.cloud.gcp.pubsub.publisher.retry.max-attempts=13",
					"spring.cloud.gcp.pubsub.publisher.retry.jittered=true",
					"spring.cloud.gcp.pubsub.publisher.retry.initial-rpc-timeout-seconds=14",
					"spring.cloud.gcp.pubsub.publisher.retry.rpc-timeout-multiplier=15",
					"spring.cloud.gcp.pubsub.publisher.retry.max-rpc-timeout-seconds=16",
					"spring.cloud.gcp.pubsub.subscriber.flow-control.max-outstanding-element-Count=17",
					"spring.cloud.gcp.pubsub.subscriber.flow-control.max-outstanding-request-Bytes=18",
					"spring.cloud.gcp.pubsub.subscriber.flow-control.limit-exceeded-behavior=Ignore",
					"spring.cloud.gcp.pubsub.subscriber.max-ack-extension-period=1",
					"spring.cloud.gcp.pubsub.publisher.batching.flow-control.max-outstanding-element-Count=19",
					"spring.cloud.gcp.pubsub.publisher.batching.flow-control.max-outstanding-request-Bytes=20",
					"spring.cloud.gcp.pubsub.publisher.batching.flow-control.limit-exceeded-behavior=Ignore",
					"spring.cloud.gcp.pubsub.publisher.batching.element-Count-threshold=21",
					"spring.cloud.gcp.pubsub.publisher.batching.request-byte-threshold=22",
					"spring.cloud.gcp.pubsub.publisher.batching.delay-threshold-seconds=23",
					"spring.cloud.gcp.pubsub.publisher.batching.enabled=true")
			.withConfiguration(AutoConfigurations.of(GcpPubSubEmulatorConfiguration.class,
					GcpContextAutoConfiguration.class,
					GcpPubSubAutoConfiguration.class));

	@Test
	public void testEmulatorConfig() {
		this.contextRunner.run((context) -> {
			CredentialsProvider defaultCredentialsProvider = context.getBean(CredentialsProvider.class);
			assertThat(defaultCredentialsProvider).isNotInstanceOf(NoCredentialsProvider.class);

			TopicAdminSettings topicAdminSettings = context.getBean(TopicAdminSettings.class);
			CredentialsProvider credentialsProvider = topicAdminSettings.getCredentialsProvider();
			assertThat(credentialsProvider).isInstanceOf(NoCredentialsProvider.class);

			TransportChannelProvider transportChannelProvider = context.getBean(TransportChannelProvider.class);
			assertThat(transportChannelProvider).isInstanceOf(FixedTransportChannelProvider.class);
		});
	}

	@Test
	public void testSubscriberPullConfig() {
		this.contextRunner.run((context) -> {
			GcpPubSubProperties gcpPubSubProperties = context
					.getBean(GcpPubSubProperties.class);
			assertThat(gcpPubSubProperties.getSubscriber().getPullEndpoint()).isEqualTo("fake-endpoint");
			assertThat((int) gcpPubSubProperties.getSubscriber().getParallelPullCount()).isEqualTo(333);
			assertThat(gcpPubSubProperties.getSubscriber().getMaxAckExtensionPeriod())
					.as("max-ack-extension-period should be set to 1")
					.isEqualTo(1);
		});
	}

	@Test
	public void testSubscriberRetrySettings() {
		this.contextRunner.run((context) -> {
			RetrySettings settings = context.getBean("subscriberRetrySettings",
					RetrySettings.class);
			assertThat(settings.getTotalTimeout()).isEqualTo(Duration.ofSeconds(1));
			assertThat(settings.getInitialRetryDelay()).isEqualTo(Duration.ofSeconds(2));
			assertThat(settings.getRetryDelayMultiplier()).isEqualTo(3, DELTA);
			assertThat(settings.getMaxRetryDelay()).isEqualTo(Duration.ofSeconds(4));
			assertThat(settings.getMaxAttempts()).isEqualTo(5);
			assertThat(settings.isJittered()).isTrue();
			assertThat(settings.getInitialRpcTimeout()).isEqualTo(Duration.ofSeconds(6));
			assertThat(settings.getRpcTimeoutMultiplier()).isEqualTo(7, DELTA);
			assertThat(settings.getMaxRpcTimeout()).isEqualTo(Duration.ofSeconds(8));
		});
	}

	@Test
	public void testPublisherRetrySettings() {
		this.contextRunner.run((context) -> {
			RetrySettings settings = context.getBean("publisherRetrySettings",
					RetrySettings.class);
			assertThat(settings.getTotalTimeout()).isEqualTo(Duration.ofSeconds(9));
			assertThat(settings.getInitialRetryDelay()).isEqualTo(Duration.ofSeconds(10));
			assertThat(settings.getRetryDelayMultiplier()).isEqualTo(11, DELTA);
			assertThat(settings.getMaxRetryDelay()).isEqualTo(Duration.ofSeconds(12));
			assertThat(settings.getMaxAttempts()).isEqualTo(13);
			assertThat(settings.isJittered()).isTrue();
			assertThat(settings.getInitialRpcTimeout()).isEqualTo(Duration.ofSeconds(14));
			assertThat(settings.getRpcTimeoutMultiplier()).isEqualTo(15, DELTA);
			assertThat(settings.getMaxRpcTimeout()).isEqualTo(Duration.ofSeconds(16));
		});
	}

	@Test
	public void testSubscriberFlowControlSettings() {
		this.contextRunner.run((context) -> {
			FlowControlSettings settings = context
					.getBean("subscriberFlowControlSettings", FlowControlSettings.class);
			assertThat(settings.getMaxOutstandingElementCount()).isEqualTo(17);
			assertThat(settings.getMaxOutstandingRequestBytes()).isEqualTo(18);
			assertThat(settings.getLimitExceededBehavior()).isEqualTo(LimitExceededBehavior.Ignore);
		});
	}

	@Test
	public void testPublisherBatchingSettings() {
		this.contextRunner.run((context) -> {
			BatchingSettings settings = context.getBean("publisherBatchSettings",
					BatchingSettings.class);
			assertThat(settings.getFlowControlSettings().getMaxOutstandingElementCount()).isEqualTo(19);
			assertThat(settings.getFlowControlSettings().getMaxOutstandingRequestBytes()).isEqualTo(20);
			assertThat(settings.getFlowControlSettings().getLimitExceededBehavior())
					.isEqualTo(LimitExceededBehavior.Ignore);
			assertThat(settings.getElementCountThreshold()).isEqualTo(21);
			assertThat(settings.getRequestByteThreshold()).isEqualTo(22);
			assertThat(settings.getDelayThreshold()).isEqualTo(Duration.ofSeconds(23));
			assertThat(settings.getIsEnabled()).isTrue();
		});
	}
}
