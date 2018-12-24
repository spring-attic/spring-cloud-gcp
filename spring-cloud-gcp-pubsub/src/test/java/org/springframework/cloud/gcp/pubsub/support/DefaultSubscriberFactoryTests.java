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

package org.springframework.cloud.gcp.pubsub.support;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.google.api.core.ApiClock;
import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.batching.FlowController;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.HeaderProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.PullRequest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.threeten.bp.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author João André Martins
 * @author Chengyuan Zhao
 * @author Eric Goetschalckx
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultSubscriberFactoryTests {

	@Mock
	private CredentialsProvider credentialsProvider;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private DefaultSubscriberFactory factory = new DefaultSubscriberFactory(() -> PROJECT_ID);

	private static final String PROJECT_ID = "projectId";
	private static final String TOPIC = "topic";
	private static final String SUBSCRIPTION = String.format("projects/%s/subscriptions/%s", PROJECT_ID, TOPIC);

	@Test
	public void testCreateSubscriber() {
		this.factory.setCredentialsProvider(this.credentialsProvider);
		this.factory.setChannelProvider(mock(TransportChannelProvider.class));
		this.factory.setExecutorProvider(mock(ExecutorProvider.class));
		this.factory.setHeaderProvider(mock(HeaderProvider.class));
		this.factory.setSystemExecutorProvider(mock(ExecutorProvider.class));
		this.factory.setPullEndpoint(UUID.randomUUID().toString());
		this.factory.setApiClock(mock(ApiClock.class));
		this.factory.setSubscriberStubRetrySettings(mock(RetrySettings.class));

		FlowControlSettings flowControlSettings = mock(FlowControlSettings.class);
		FlowControlSettings.Builder flowControlSettingsBuilder = mock(FlowControlSettings.Builder.class);
		when(flowControlSettingsBuilder.setLimitExceededBehavior(any())).thenReturn(flowControlSettingsBuilder);
		when(flowControlSettingsBuilder.build()).thenReturn(flowControlSettings);
		when(flowControlSettings.toBuilder()).thenReturn(flowControlSettingsBuilder);
		when(flowControlSettings.getLimitExceededBehavior()).thenReturn(FlowController.LimitExceededBehavior.Ignore);
		this.factory.setFlowControlSettings(flowControlSettings);

		this.factory.setMaxAckExtensionPeriod(Duration.ZERO);
		this.factory.setParallelPullCount(42);

		Subscriber subscriber = this.factory.createSubscriber(TOPIC, (message, consumer) -> { });

		assertThat(this.factory.getProjectId()).isEqualTo(PROJECT_ID);
		assertThat(subscriber.getSubscriptionNameString()).isEqualTo(SUBSCRIPTION);
		assertThat(subscriber.getFlowControlSettings()).isSameAs(flowControlSettings);
	}

	@Test
	public void testCreatePullRequest() {
		PullRequest pullRequest = this.factory.createPullRequest(TOPIC, 42, true);

		assertThat(pullRequest.getSubscription()).isEqualTo(SUBSCRIPTION);
		assertThat(pullRequest.getReturnImmediately()).isTrue();
		assertThat(pullRequest.getMaxMessages()).isEqualTo(42);
	}

	@Test
	public void testNewDefaultSubscriberFactory_nullProjectProvider() {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("The project ID provider can't be null.");
		new DefaultSubscriberFactory(null);
	}

	@Test
	public void testNewDefaultSubscriberFactory_nullProject() {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("The project ID can't be null or empty.");
		new DefaultSubscriberFactory(() -> null);
	}

}
