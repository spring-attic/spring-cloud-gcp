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

package org.springframework.cloud.gcp.pubsub;

import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for the Pub/Sub admin operations.
 *
 * @author João André Martins
 * @author Chengyuan Zhao
 */
@RunWith(MockitoJUnitRunner.class)
public class PubSubAdminTests {

	/**
	 * used to check exception messages and types.
	 */
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Mock
	private TopicAdminClient mockTopicAdminClient;

	@Mock
	private SubscriptionAdminClient mockSubscriptionAdminClient;

	@Test
	public void testNewPubSubAdmin_nullProjectProvider() {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("The project ID provider can't be null.");
		new PubSubAdmin(null, this.mockTopicAdminClient, this.mockSubscriptionAdminClient);
	}

	@Test
	public void testNewPubSubAdmin_nullTopicAdminClient() {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("The topic administration client can't be null");
		new PubSubAdmin(() -> "test-project", null, this.mockSubscriptionAdminClient);
	}

	@Test
	public void testNewPubSubAdmin_nullSubscriptionAdminClient() {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("The subscription administration client can't be null");
		new PubSubAdmin(() -> "test-project", this.mockTopicAdminClient, null);
	}
}
