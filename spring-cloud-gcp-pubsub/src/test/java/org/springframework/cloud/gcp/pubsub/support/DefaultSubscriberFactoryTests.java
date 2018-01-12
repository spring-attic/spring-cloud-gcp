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
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.threeten.bp.Duration;

import static org.junit.Assert.assertNotNull;

/**
 * @author João André Martins
 * @author Mike Eltsufin
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultSubscriberFactoryTests {

	@Mock
	private ExecutorProvider executorProvider;

	@Mock
	private TransportChannelProvider channelProvider;

	@Mock
	private CredentialsProvider credentialsProvider;

	@Test(expected = IllegalArgumentException.class)
	public void testNewDefaultSubscriberFactory_nullProjectProvider() {
		new DefaultSubscriberFactory(null, this.executorProvider, this.channelProvider,
				this.credentialsProvider);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNewDefaultSubscriberFactory_nullProject() {
		new DefaultSubscriberFactory(() -> null, this.executorProvider, this.channelProvider,
				this.credentialsProvider);
	}

	@Test
	public void testCreateSubscriberStub_nullRetrySettings() {
		DefaultSubscriberFactory factory = new DefaultSubscriberFactory(() -> "test_project_id",
				this.executorProvider, this.channelProvider, this.credentialsProvider);

		SubscriberStub subscriberStub = factory.createSubscriberStub(null);
		assertNotNull(subscriberStub);
	}

	@Test
	public void testCreateSubscriberStub_notNullRetrySettings() {
		DefaultSubscriberFactory factory = new DefaultSubscriberFactory(() -> "test_project_id",
				this.executorProvider, this.channelProvider, this.credentialsProvider);

		SubscriberStub subscriberStub = factory.createSubscriberStub(RetrySettings.newBuilder().setTotalTimeout(
				Duration.ofMillis(1000)).build());
		assertNotNull(subscriberStub);
	}
}
