/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.pubsub.core;

import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberTemplate;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PubSubSubscriberTemplate} for functionality not exposed in
 * {@link PubSubTemplate}.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
@RunWith(MockitoJUnitRunner.class)
public class PubSubSubscriberTemplateTests {

	@Mock
	SubscriberFactory mockSubscriberFactory;

	@Mock
	SubscriberStub mockSubscriberStub;

	PubSubSubscriberTemplate subscriberTemplate;

	@Before
	public void setUp() {
		when(this.mockSubscriberFactory.createSubscriberStub()).thenReturn(this.mockSubscriberStub);
		subscriberTemplate = new PubSubSubscriberTemplate(this.mockSubscriberFactory);
	}

	@Test
	public void destroyingBeanClosesSubscriberStub() {
		verify(this.mockSubscriberFactory).createSubscriberStub();

		verify(this.mockSubscriberStub, times(0)).close();
		this.subscriberTemplate.destroy();
		verify(this.mockSubscriberStub, times(1)).close();

	}
}
