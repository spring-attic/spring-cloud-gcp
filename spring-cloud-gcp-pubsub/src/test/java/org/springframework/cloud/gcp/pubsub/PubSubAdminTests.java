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

package org.springframework.cloud.gcp.pubsub;

import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author João André Martins
 */
@RunWith(MockitoJUnitRunner.class)
public class PubSubAdminTests {

	@Mock
	private TopicAdminClient mockTopicAdminClient;

	@Mock
	private SubscriptionAdminClient mockSubscriptionAdminClient;

	@Test(expected = IllegalArgumentException.class)
	public void testNewPubSubAdmin_nullProjectProvider() {
		new PubSubAdmin(null, this.mockTopicAdminClient, this.mockSubscriptionAdminClient);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNewPubSubAdmin_nullTopicAdminClient() {
		new PubSubAdmin(() -> "test-project", null, this.mockSubscriptionAdminClient);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNewPubSubAdmin_nullSubscriptionAdminClient() {
		new PubSubAdmin(() -> "test-project", this.mockTopicAdminClient, null);
	}
}
