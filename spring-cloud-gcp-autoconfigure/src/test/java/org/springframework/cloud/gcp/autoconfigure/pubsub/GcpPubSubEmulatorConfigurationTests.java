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

import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.batching.FlowController.LimitExceededBehavior;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import org.junit.Assert;
import org.junit.Test;
import org.threeten.bp.Duration;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;

/**
 * @author Andreas Berger
 * @author João André Martins
 * @author Chengyuan Zhao
 */
public class GcpPubSubEmulatorConfigurationTests {

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
		this.contextRunner.run(context -> {
			CredentialsProvider defaultCredentialsProvider = context.getBean(CredentialsProvider.class);
			Assert.assertFalse("CredentialsProvider is not correct",
					defaultCredentialsProvider instanceof NoCredentialsProvider);

			TopicAdminSettings topicAdminSettings = context.getBean(TopicAdminSettings.class);
			CredentialsProvider credentialsProvider = topicAdminSettings.getCredentialsProvider();
			Assert.assertTrue("CredentialsProvider for emulator is not correct",
					credentialsProvider instanceof NoCredentialsProvider);

			TransportChannelProvider transportChannelProvider = context.getBean(TransportChannelProvider.class);
			Assert.assertTrue("TransportChannelProvider is not correct",
					transportChannelProvider instanceof FixedTransportChannelProvider);
		});
	}

	@Test
	public void testSubscriberPullConfig() {
		this.contextRunner.run(context -> {
			GcpPubSubProperties gcpPubSubProperties = context
					.getBean(GcpPubSubProperties.class);
			Assert.assertEquals("fake-endpoint",
					gcpPubSubProperties.getSubscriber().getPullEndpoint());
			Assert.assertEquals(333,
					(int) gcpPubSubProperties.getSubscriber().getParallelPullCount());
			Assert.assertEquals("max-ack-extension-period should be set to 1", new Long(1),
					gcpPubSubProperties.getSubscriber().getMaxAckExtensionPeriod());
		});
	}

	@Test
	public void testSubscriberRetrySettings() {
		this.contextRunner.run(context -> {
			RetrySettings settings = context.getBean("subscriberRetrySettings",
					RetrySettings.class);
			Assert.assertEquals(Duration.ofSeconds(1), settings.getTotalTimeout());
			Assert.assertEquals(Duration.ofSeconds(2), settings.getInitialRetryDelay());
			Assert.assertEquals(3, settings.getRetryDelayMultiplier(), 0.0001);
			Assert.assertEquals(Duration.ofSeconds(4), settings.getMaxRetryDelay());
			Assert.assertEquals(5, settings.getMaxAttempts());
			Assert.assertTrue(settings.isJittered());
			Assert.assertEquals(Duration.ofSeconds(6), settings.getInitialRpcTimeout());
			Assert.assertEquals(7, settings.getRpcTimeoutMultiplier(), 0.0001);
			Assert.assertEquals(Duration.ofSeconds(8), settings.getMaxRpcTimeout());
		});
	}

	@Test
	public void testPublisherRetrySettings() {
		this.contextRunner.run(context -> {
			RetrySettings settings = context.getBean("publisherRetrySettings",
					RetrySettings.class);
			Assert.assertEquals(Duration.ofSeconds(9), settings.getTotalTimeout());
			Assert.assertEquals(Duration.ofSeconds(10), settings.getInitialRetryDelay());
			Assert.assertEquals(11, settings.getRetryDelayMultiplier(), 0.0001);
			Assert.assertEquals(Duration.ofSeconds(12), settings.getMaxRetryDelay());
			Assert.assertEquals(13, settings.getMaxAttempts());
			Assert.assertTrue(settings.isJittered());
			Assert.assertEquals(Duration.ofSeconds(14), settings.getInitialRpcTimeout());
			Assert.assertEquals(15, settings.getRpcTimeoutMultiplier(), 0.0001);
			Assert.assertEquals(Duration.ofSeconds(16), settings.getMaxRpcTimeout());
		});
	}

	@Test
	public void testSubscriberFlowControlSettings() {
		this.contextRunner.run(context -> {
			FlowControlSettings settings = context
					.getBean("subscriberFlowControlSettings", FlowControlSettings.class);
			Assert.assertEquals(17, (long) settings.getMaxOutstandingElementCount());
			Assert.assertEquals(18, (long) settings.getMaxOutstandingRequestBytes());
			Assert.assertEquals(LimitExceededBehavior.Ignore,
					settings.getLimitExceededBehavior());
		});
	}

	@Test
	public void testPublisherBatchingSettings() {
		this.contextRunner.run(context -> {
			BatchingSettings settings = context.getBean("publisherBatchSettings",
					BatchingSettings.class);
			Assert.assertEquals(19, (long) settings.getFlowControlSettings()
					.getMaxOutstandingElementCount());
			Assert.assertEquals(20, (long) settings.getFlowControlSettings()
					.getMaxOutstandingRequestBytes());
			Assert.assertEquals(LimitExceededBehavior.Ignore,
					settings.getFlowControlSettings().getLimitExceededBehavior());
			Assert.assertEquals(21, (long) settings.getElementCountThreshold());
			Assert.assertEquals(22, (long) settings.getRequestByteThreshold());
			Assert.assertEquals(Duration.ofSeconds(23), settings.getDelayThreshold());
			Assert.assertTrue(settings.getIsEnabled());
		});
	}
}
