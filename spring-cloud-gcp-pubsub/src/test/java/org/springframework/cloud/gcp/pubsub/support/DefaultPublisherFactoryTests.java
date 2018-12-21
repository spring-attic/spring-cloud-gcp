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

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

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
import org.threeten.bp.Duration;

import org.springframework.cloud.gcp.pubsub.core.PubSubException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

	private static final String TEST_TOPIC = "testTopic";
	private static final String PROJECT_ID = "projectId";

	@Test
	public void testCreatePublisher() throws IOException {
		when(this.credentialsProvider.getCredentials()).thenReturn(this.credentials);
		this.factory.setCredentialsProvider(this.credentialsProvider);

		ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
		ExecutorProvider executorProvider = mock(ExecutorProvider.class);
		when(executorProvider.getExecutor()).thenReturn(executor);
		this.factory.setExecutorProvider(executorProvider);

		ApiCallContext apiCallContext = mock(ApiCallContext.class);
		when(apiCallContext.withCredentials(any())).thenReturn(apiCallContext);
		when(apiCallContext.withTransportChannel(any())).thenReturn(apiCallContext);

		TransportChannel transportChannel = mock(TransportChannel.class);
		when(transportChannel.getEmptyCallContext()).thenReturn(apiCallContext);

		TransportChannelProvider channelProvider = mock(TransportChannelProvider.class);
		when(channelProvider.getTransportChannel()).thenReturn(transportChannel);
		this.factory.setChannelProvider(channelProvider);

		HeaderProvider headerProvider = mock(HeaderProvider.class);
		this.factory.setHeaderProvider(headerProvider);

		RetrySettings retrySettings = mock(RetrySettings.class);
		when(retrySettings.getTotalTimeout()).thenReturn(Duration.ofSeconds(10L));
		when(retrySettings.getInitialRpcTimeout()).thenReturn(Duration.ofMillis(10L));
		when(retrySettings.getMaxAttempts()).thenReturn(1);
		this.factory.setRetrySettings(retrySettings);

		BatchingSettings batchingSettings = mock(BatchingSettings.class);
		when(batchingSettings.getElementCountThreshold()).thenReturn(1L);
		when(batchingSettings.getRequestByteThreshold()).thenReturn(1L);
		when(batchingSettings.getDelayThreshold()).thenReturn(Duration.ofSeconds(1L));
		this.factory.setBatchingSettings(batchingSettings);

		Publisher publisher = this.factory.createPublisher(TEST_TOPIC);

		assertThat(this.factory.getCache().size()).isEqualTo(1);
		assertThat(publisher).isEqualTo(this.factory.getCache().get(TEST_TOPIC));
		assertThat(((ProjectTopicName) publisher.getTopicName()).getTopic()).isEqualTo(TEST_TOPIC);
		assertThat(((ProjectTopicName) publisher.getTopicName()).getProject()).isEqualTo(PROJECT_ID);

		verify(this.credentialsProvider, times(1)).getCredentials();
		verify(executorProvider, times(1)).getExecutor();
		verify(channelProvider, times(1)).getTransportChannel();
	}

	@Test
	public void testGetPublisher_ioException() throws IOException {
		when(this.credentialsProvider.getCredentials()).thenThrow(IOException.class);
		this.factory.setCredentialsProvider(this.credentialsProvider);

		this.expectedException.expect(PubSubException.class);
		this.expectedException.expectMessage(
				"An error creating the Google Cloud Pub/Sub publisher occurred.; nested exception is java.io.IOException");

		this.factory.createPublisher(TEST_TOPIC);
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
