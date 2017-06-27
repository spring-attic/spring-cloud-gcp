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

import com.google.api.gax.grpc.ChannelProvider;
import com.google.api.gax.grpc.ExecutorProvider;
import com.google.cloud.pubsub.spi.v1.Publisher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author João André Martins
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultPublisherFactoryTest {

	private DefaultPublisherFactory factory;
	@Mock
	private ExecutorProvider executorProvider;
	@Mock
	private ChannelProvider channelProvider;

	@Before
	public void setUp() {
		this.factory = new DefaultPublisherFactory(
				"projectId", this.executorProvider, this.channelProvider);
	}

	@Test
	public void testGetPublisher() {
		Publisher publisher = this.factory.getPublisher("testTopic");

		assertEquals(this.factory.getCache().size(), 1);
		assertTrue(this.factory.getCache().get("testTopic") == publisher);
	}
}
