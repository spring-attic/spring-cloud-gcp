/*
 *  Copyright 2017 original author or authors.
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

package org.springframework.cloud.gcp.pubsub.support;

import java.io.IOException;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author João André Martins
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultPublisherFactoryTests {

	private DefaultPublisherFactory factory;
	@Mock
	private ExecutorProvider executorProvider;
	@Mock
	private TransportChannelProvider channelProvider;
	@Mock
	private CredentialsProvider credentialsProvider;
	@Mock
	private GrpcTransportChannel transportChannel;

	@Before
	public void setUp() throws IOException {
		when(this.channelProvider.getTransportChannel()).thenReturn(this.transportChannel);
		when(this.channelProvider.shouldAutoClose()).thenReturn(false);

		this.factory = new DefaultPublisherFactory(
				() -> "projectId", this.executorProvider, this.channelProvider,
				this.credentialsProvider);
	}

	@Test
	public void testGetPublisher() {
		Publisher publisher = this.factory.getPublisher("testTopic");

		assertEquals(this.factory.getCache().size(), 1);
		assertTrue(this.factory.getCache().get("testTopic") == publisher);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNewDefaultPublisherFactory_nullProjectIdProvider() {
		new DefaultPublisherFactory(null, this.executorProvider, this.channelProvider,
				this.credentialsProvider);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNewDefaultPublisherFactory_nullProjectId() {
		new DefaultPublisherFactory(() -> null, this.executorProvider, this.channelProvider,
				this.credentialsProvider);
	}
}
