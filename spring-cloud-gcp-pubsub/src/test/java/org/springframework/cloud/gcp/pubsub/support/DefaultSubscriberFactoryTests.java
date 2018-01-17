/*
 *  Copyright 2017-2018 original author or authors.
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
import com.google.cloud.pubsub.v1.Subscriber;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

/**
 * @author João André Martins
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultSubscriberFactoryTests {

	@Mock
	private CredentialsProvider credentialsProvider;

	@Test
	public void testNewSubscriber() {
		DefaultSubscriberFactory factory = new DefaultSubscriberFactory(() -> "angeldust");
		factory.setCredentialsProvider(this.credentialsProvider);

		Subscriber subscriber = factory.createSubscriber("midnight cowboy", (message, consumer) -> { });

		assertEquals("midnight cowboy", subscriber.getSubscriptionName().getSubscription());
		assertEquals("angeldust", subscriber.getSubscriptionName().getProject());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNewDefaultSubscriberFactory_nullProjectProvider() {
		new DefaultSubscriberFactory(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNewDefaultSubscriberFactory_nullProject() {
		new DefaultSubscriberFactory(() -> null);
	}

}
