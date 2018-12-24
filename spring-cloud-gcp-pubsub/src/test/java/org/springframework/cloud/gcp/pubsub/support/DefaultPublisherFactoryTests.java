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

import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.ApiCallContext;
import com.google.api.gax.rpc.HeaderProvider;
import com.google.api.gax.rpc.TransportChannel;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.Credentials;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.ProjectTopicName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.gcp.pubsub.core.PubSubException;
import org.threeten.bp.Duration;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * @author João André Martins
 * @author Chengyuan Zhao
 * @author Eric Goetschalckx
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultPublisherFactoryTests {

	private DefaultPublisherFactory factory = new DefaultPublisherFactory(() -> PROJECT_ID);

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Mock
	private CredentialsProvider credentialsProvider;

	@Mock
	private Credentials credentials;

	@Mock
	private ExecutorProvider executorProvider;

	@Mock
	private ScheduledExecutorService executor;

	@Mock
	private TransportChannelProvider channelProvider;

	@Mock
	private TransportChannel transportChannel;

	@Mock
	private ApiCallContext apiCallContext;

	@Mock
	private HeaderProvider headerProvider;

	@Mock
	private RetrySettings retrySettings;

	@Mock
	private RetrySettings.Builder retrySettingsBuilder;

	@Mock
	private BatchingSettings batchingSettings;

	private static final String TEST_TOPIC = "testTopic";
	private static final String PROJECT_ID = "projectId";

	@Test
	public void testGetPublisher() throws IOException {
		DefaultPublisherFactory factory = new DefaultPublisherFactory(() -> PROJECT_ID);
		factory.setCredentialsProvider(this.credentialsProvider);
		factory.setExecutorProvider(this.executorProvider);
		factory.setChannelProvider(channelProvider);
		factory.setHeaderProvider(headerProvider);
		factory.setRetrySettings(retrySettings);
		factory.setBatchingSettings(batchingSettings);

		when(credentialsProvider.getCredentials()).thenReturn(credentials);

		when(executorProvider.getExecutor()).thenReturn(executor);

		when(apiCallContext.withCredentials(any())).thenReturn(apiCallContext);
		when(apiCallContext.withTransportChannel(any())).thenReturn(apiCallContext);
		when(transportChannel.getEmptyCallContext()).thenReturn(apiCallContext);
		when(channelProvider.getTransportChannel()).thenReturn(transportChannel);

		when(retrySettings.getTotalTimeout()).thenReturn(Duration.ofSeconds(42L));
		when(retrySettings.getInitialRpcTimeout()).thenReturn(Duration.ofMillis(9001L));
		when(retrySettingsBuilder.setMaxAttempts(anyInt())).thenReturn(retrySettingsBuilder);
		when(retrySettingsBuilder.build()).thenReturn(retrySettings);
		when(retrySettings.toBuilder()).thenReturn(retrySettingsBuilder);

		when(batchingSettings.getElementCountThreshold()).thenReturn(1L);
		when(batchingSettings.getRequestByteThreshold()).thenReturn(2L);
		when(batchingSettings.getDelayThreshold()).thenReturn(Duration.ofSeconds(3L));

		Publisher publisher = factory.createPublisher(TEST_TOPIC);

		assertEquals(factory.getCache().size(), 1);
		assertEquals(publisher, factory.getCache().get(TEST_TOPIC));
		assertEquals(TEST_TOPIC, ((ProjectTopicName) publisher.getTopicName()).getTopic());
		assertEquals(PROJECT_ID, ((ProjectTopicName) publisher.getTopicName()).getProject());
	}

	@Test
	public void testGetPublisher_noProviders() throws IOException {
		DefaultPublisherFactory factory = new DefaultPublisherFactory(() -> PROJECT_ID);

		factory.setCredentialsProvider(this.credentialsProvider);
		when(credentialsProvider.getCredentials()).thenReturn(credentials);

		Publisher publisher = factory.createPublisher(TEST_TOPIC);

		assertEquals(factory.getCache().size(), 1);
		assertEquals(publisher, factory.getCache().get(TEST_TOPIC));
		assertEquals(TEST_TOPIC, ((ProjectTopicName) publisher.getTopicName()).getTopic());
		assertEquals(PROJECT_ID, ((ProjectTopicName) publisher.getTopicName()).getProject());
	}

	@Test
	public void testGetPublisher_ioException() throws IOException {
		DefaultPublisherFactory factory = new DefaultPublisherFactory(() -> PROJECT_ID);

		factory.setCredentialsProvider(this.credentialsProvider);
		when(credentialsProvider.getCredentials()).thenThrow(IOException.class);

		expectedException.expect(PubSubException.class);
		expectedException.expectMessage(
				"An error creating the Google Cloud Pub/Sub publisher occurred.; nested exception is java.io.IOException");

		Publisher publisher = factory.createPublisher(TEST_TOPIC);
	}

	@Test
	public void testNewDefaultPublisherFactory_nullProjectIdProvider() {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("The project ID provider can't be null.");
		new DefaultPublisherFactory(null);
	}

	@Test
	public void testNewDefaultPublisherFactory_nullProjectId() {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("The project ID can't be null or empty.");
		new DefaultPublisherFactory(() -> null);
	}
}
