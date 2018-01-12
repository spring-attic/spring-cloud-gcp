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

import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.pubsub.v1.PullRequest;

/**
 * Generates {@link Subscriber} objects.
 *
 * @author João André Martins
 * @author Mike Eltsufin
 */
public interface SubscriberFactory {

	Subscriber createSubscriber(String subscriptionName, MessageReceiver receiver);

	PullRequest createPullRequest(String subscriptionName, Integer maxMessages, Boolean returnImmediately);

	SubscriberStub createSubscriberStub(RetrySettings retrySettings);
}
