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

package org.springframework.cloud.gcp.stream.binder.pubsub.provisioning;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubConsumerProperties;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubProducerProperties;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.util.StringUtils;

/**
 * Provisioning provider for Pub/Sub.
 *
 * @author João André Martins
 * @author Mike Eltsufin
 */
public class PubSubChannelProvisioner
		implements ProvisioningProvider<ExtendedConsumerProperties<PubSubConsumerProperties>,
		ExtendedProducerProperties<PubSubProducerProperties>> {

	private static final Log LOGGER = LogFactory.getLog(PubSubChannelProvisioner.class);

	private final PubSubAdmin pubSubAdmin;

	private final Set<String> anonymousGroupSubscriptionNames = new HashSet<>();

	public PubSubChannelProvisioner(PubSubAdmin pubSubAdmin) {
		this.pubSubAdmin = pubSubAdmin;
	}

	@Override
	public ProducerDestination provisionProducerDestination(String topic,
			ExtendedProducerProperties<PubSubProducerProperties> properties)
			throws ProvisioningException {
		makeSureTopicExists(topic, properties.getExtension().isAutoCreateResources());

		return new PubSubProducerDestination(topic);
	}

	@Override
	public ConsumerDestination provisionConsumerDestination(String topicName, String group,
			ExtendedConsumerProperties<PubSubConsumerProperties> properties)
			throws ProvisioningException {

		Topic topic = makeSureTopicExists(topicName, properties.getExtension().isAutoCreateResources());

		String subscriptionName;
		Subscription subscription;
		if (StringUtils.hasText(group)) {
			// Use <topicName>.<group> as subscription name
			subscriptionName = topicName + "." + group;
			subscription = this.pubSubAdmin.getSubscription(subscriptionName);
		}
		else {
			// Generate anonymous random group since one wasn't provided
			subscriptionName = "anonymous." + topicName + "." + UUID.randomUUID().toString();
			subscription = this.pubSubAdmin.createSubscription(subscriptionName, topicName);
			this.anonymousGroupSubscriptionNames.add(subscriptionName);
		}

		// make sure subscription exists
		if (subscription == null) {
			if (properties.getExtension().isAutoCreateResources()) {
				this.pubSubAdmin.createSubscription(subscriptionName, topicName);
			}
			else {
				throw new ProvisioningException("Non-existing '" + subscriptionName + "' subscription.");
			}
		}
		else if (!subscription.getTopic().equals(topic.getName())) {
			throw new ProvisioningException(
					"Existing '" + subscriptionName + "' subscription is for a different topic '"
							+ subscription.getTopic() + "'.");
		}
		return new PubSubConsumerDestination(subscriptionName);
	}

	public void afterUnbindConsumer(ConsumerDestination destination) {
		if (this.anonymousGroupSubscriptionNames.remove(destination.getName())) {
			try {
				this.pubSubAdmin.deleteSubscription(destination.getName());
			}
			catch (Exception ex) {
				LOGGER.warn("Failed to delete auto-created anonymous subscription '" + destination.getName() + "'.");
			}
		}
	}

	private Topic makeSureTopicExists(String topicName, boolean autoCreate) {
		Topic topic = this.pubSubAdmin.getTopic(topicName);
		if (topic == null) {
			if (autoCreate) {
				try {
					topic = this.pubSubAdmin.createTopic(topicName);
				}
				catch (AlreadyExistsException alreadyExistsException) {
					// Ignore concurrent topic creation - we're good as long as topic was created and exists
					LOGGER.info("Failed to auto-create topic '" + topicName + "' because it already exists.");
				}
			}
			else {
				throw new ProvisioningException("Non-existing '" + topicName + "' topic.");
			}
		}

		return topic;
	}
}
