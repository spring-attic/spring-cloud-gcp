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

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.ProjectTopicName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the publisher factory.
 *
 * @author João André Martins
 * @author Chengyuan Zhao
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultPublisherFactoryTests {

	/**
	 * used to test exception messages and types.
	 */
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Mock
	private CredentialsProvider credentialsProvider;

	@Test
	public void testGetPublisher() {
		DefaultPublisherFactory factory = new DefaultPublisherFactory(() -> "projectId");
		factory.setCredentialsProvider(this.credentialsProvider);
		Publisher publisher = factory.createPublisher("testTopic");

		assertThat(factory.getCache().size()).isEqualTo(1);
		assertThat(publisher).isEqualTo(factory.getCache().get("testTopic"));
		assertThat(((ProjectTopicName) publisher.getTopicName()).getTopic()).isEqualTo("testTopic");
		assertThat(((ProjectTopicName) publisher.getTopicName()).getProject()).isEqualTo("projectId");
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
