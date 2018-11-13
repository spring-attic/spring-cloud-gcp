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

package org.springframework.cloud.gcp.stream.binder.pubsub.provisioning;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.google.pubsub.v1.Subscription;
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
 * @author João André Martins
 * @author Mike Eltsufin
 */
public class PubSubChannelProvisioner
		implements ProvisioningProvider<ExtendedConsumerProperties<PubSubConsumerProperties>,
		ExtendedProducerProperties<PubSubProducerProperties>> {

	private static final Log LOGGER = LogFactory.getLog(PubSubChannelProvisioner.class);

	private final PubSubAdmin pubSubAdmin;

	private final Set<String> anonymousSubscriptions = new HashSet<>();

	public PubSubChannelProvisioner(PubSubAdmin pubSubAdmin) {
		this.pubSubAdmin = pubSubAdmin;
	}

	@Override
	public ProducerDestination provisionProducerDestination(String name,
			ExtendedProducerProperties<PubSubProducerProperties> properties)
			throws ProvisioningException {
		if (this.pubSubAdmin.getTopic(name) == null) {
			this.pubSubAdmin.createTopic(name);
		}

		return new PubSubProducerDestination(name);
	}

	@Override
	public ConsumerDestination provisionConsumerDestination(String name, String group,
			ExtendedConsumerProperties<PubSubConsumerProperties> properties)
			throws ProvisioningException {
		String subscription;
		// Use <topic>.<groupName> as subscription name
		// Generate anonymous random group, if one not provided
		boolean anonymous = false;
		if (StringUtils.hasText(group)) {
			subscription = name + "." + group;
		}
		else {
			subscription = "anonymous." + name + "." + UUID.randomUUID().toString();
			anonymous = true;
		}

		Subscription pubSubSubscription = this.pubSubAdmin.getSubscription(subscription);
		if (pubSubSubscription == null) {
			if (properties.getExtension().isAutoCreateResources()) {
				if (this.pubSubAdmin.getTopic(name) == null) {
					this.pubSubAdmin.createTopic(name);
				}

				this.pubSubAdmin.createSubscription(subscription, name);

				if (anonymous) {
					this.anonymousSubscriptions.add(subscription);
				}
			}
			else {
				throw new ProvisioningException("Non-existing '" + subscription + "' subscription.");
			}
		}
		else if (!pubSubSubscription.getTopic().equals(name)) {
			throw new ProvisioningException("Existing '" + subscription + "' subscription is for a different topic '"
					+ pubSubSubscription.getTopic() + "'.");
		}
		return new PubSubConsumerDestination(subscription);
	}

	public void afterUnbindConsumer(ConsumerDestination destination) {
		if (this.anonymousSubscriptions.remove(destination.getName())) {
			try {
				this.pubSubAdmin.deleteSubscription(destination.getName());
			}
			catch (Exception ex) {
				LOGGER.warn("Failed to delete auto-created anonymous subscription '" + destination.getName() + "'.");
			}
		}
	}
}
