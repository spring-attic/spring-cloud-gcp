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
import com.google.api.gax.grpc.ChannelProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author João André Martins
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultSubscriberFactoryTests {

	@Mock
	private ExecutorProvider executorProvider;

	@Mock
	private ChannelProvider channelProvider;

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
}
