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

package org.springframework.cloud.gcp.stream.binder.pubsub.properties;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;
import org.springframework.cloud.stream.binder.ExtendedBindingProperties;

/**
 * @author João André Martins
 * @author Artem Bilan
 */
@ConfigurationProperties("spring.cloud.stream.gcp.pubsub")
public class PubSubExtendedBindingProperties
		implements ExtendedBindingProperties<PubSubConsumerProperties, PubSubProducerProperties> {

	private static final String DEFAULTS_PREFIX = "spring.cloud.stream.gcp.pubsub.default";

	private Map<String, PubSubBindingProperties> bindings = new HashMap<>();

	public Map<String, PubSubBindingProperties> getBindings() {
		return this.bindings;
	}

	@Override
	public synchronized PubSubConsumerProperties getExtendedConsumerProperties(String channelName) {
		PubSubConsumerProperties properties;
		if (this.bindings.containsKey(channelName)) {
			if (this.bindings.get(channelName).getConsumer() != null) {
				properties = this.bindings.get(channelName).getConsumer();
			}
			else {
				properties = new PubSubConsumerProperties();
				this.bindings.get(channelName).setConsumer(properties);
			}
		}
		else {
			properties = new PubSubConsumerProperties();
			PubSubBindingProperties rbp = new PubSubBindingProperties();
			rbp.setConsumer(properties);
			this.bindings.put(channelName, rbp);
		}
		return properties;
	}

	@Override
	public synchronized PubSubProducerProperties getExtendedProducerProperties(String channelName) {
		PubSubProducerProperties properties;
		if (this.bindings.containsKey(channelName)) {
			if (this.bindings.get(channelName).getProducer() != null) {
				properties = this.bindings.get(channelName).getProducer();
			}
			else {
				properties = new PubSubProducerProperties();
				this.bindings.get(channelName).setProducer(properties);
			}
		}
		else {
			properties = new PubSubProducerProperties();
			PubSubBindingProperties rbp = new PubSubBindingProperties();
			rbp.setProducer(properties);
			this.bindings.put(channelName, rbp);
		}
		return properties;
	}

	@Override
	public String getDefaultsPrefix() {
		return DEFAULTS_PREFIX;
	}

	@Override
	public Class<? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
		return PubSubBindingProperties.class;
	}

}
