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

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.ProjectTopicName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

/**
 * @author João André Martins
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultPublisherFactoryTests {

	@Mock
	private CredentialsProvider credentialsProvider;

	@Test
	public void testGetPublisher() {
		DefaultPublisherFactory factory = new DefaultPublisherFactory(() -> "projectId");
		factory.setCredentialsProvider(this.credentialsProvider);
		Publisher publisher = factory.createPublisher("testTopic");

		assertEquals(factory.getCache().size(), 1);
		assertEquals(publisher, factory.getCache().get("testTopic"));
		assertEquals("testTopic", ((ProjectTopicName) publisher.getTopicName()).getTopic());
		assertEquals("projectId", ((ProjectTopicName) publisher.getTopicName()).getProject());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNewDefaultPublisherFactory_nullProjectIdProvider() {
		new DefaultPublisherFactory(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNewDefaultPublisherFactory_nullProjectId() {
		new DefaultPublisherFactory(() -> null);
	}
}
