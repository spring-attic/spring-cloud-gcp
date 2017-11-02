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

package org.springframework.cloud.gcp.stream.binder.pubsub;

import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubConsumerProperties;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubExtendedBindingProperties;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubProducerProperties;
import org.springframework.cloud.gcp.stream.binder.pubsub.provisioning.PubSubChannelProvisioner;
import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.ExtendedPropertiesBinder;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.gcp.pubsub.inbound.PubSubInboundChannelAdapter;
import org.springframework.integration.gcp.pubsub.outbound.PubSubMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * @author João André Martins
 */
public class PubSubMessageChannelBinder
		extends AbstractMessageChannelBinder<ExtendedConsumerProperties<PubSubConsumerProperties>,
		ExtendedProducerProperties<PubSubProducerProperties>,
		PubSubChannelProvisioner>
	implements ExtendedPropertiesBinder<MessageChannel, PubSubConsumerProperties,
		PubSubProducerProperties> {

	private PubSubTemplate pubSubTemplate;

	private PubSubExtendedBindingProperties pubSubExtendedBindingProperties =
			new PubSubExtendedBindingProperties();

	public PubSubMessageChannelBinder(boolean supportsHeadersNatively, String[] headersToEmbed,
			PubSubChannelProvisioner provisioningProvider, PubSubTemplate pubSubTemplate) {
		super(supportsHeadersNatively, headersToEmbed, provisioningProvider);
		this.pubSubTemplate = pubSubTemplate;
	}

	@Override
	protected MessageHandler createProducerMessageHandler(ProducerDestination destination,
			ExtendedProducerProperties<PubSubProducerProperties> producerProperties,
			MessageChannel errorChannel) throws Exception {

		this.provisioningProvider.provisionProducerDestination(
				destination.getName(), producerProperties);

		return new PubSubMessageHandler(this.pubSubTemplate, destination.getName());
	}

	@Override
	protected MessageProducer createConsumerEndpoint(ConsumerDestination destination, String group,
			ExtendedConsumerProperties<PubSubConsumerProperties> properties) throws Exception {

		String consumerName = group == null
				? destination.getName() : destination.getName() + "-" + group;

		this.provisioningProvider.provisionConsumerDestination(
				consumerName, group, properties);

		return new PubSubInboundChannelAdapter(this.pubSubTemplate, destination.getName());
	}

	@Override
	public PubSubConsumerProperties getExtendedConsumerProperties(String channelName) {
		return this.pubSubExtendedBindingProperties.getExtendedConsumerProperties(channelName);
	}

	@Override
	public PubSubProducerProperties getExtendedProducerProperties(String channelName) {
		return this.pubSubExtendedBindingProperties.getExtendedProducerProperties(channelName);
	}


}
