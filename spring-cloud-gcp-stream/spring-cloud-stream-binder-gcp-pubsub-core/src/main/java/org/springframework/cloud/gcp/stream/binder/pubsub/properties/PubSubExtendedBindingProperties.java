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

package org.springframework.cloud.gcp.stream.binder.pubsub.properties;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.stream.binder.ExtendedBindingProperties;

/**
 * @author João André Martins
 */
@ConfigurationProperties("spring.cloud.stream.gcp.pubsub")
public class PubSubExtendedBindingProperties
		implements ExtendedBindingProperties<PubSubConsumerProperties, PubSubProducerProperties> {

	private Map<String, PubSubBindingProperties> bindings = new HashMap<>();

	public Map<String, PubSubBindingProperties> getBindings() {
		return this.bindings;
	}

	@Override
	public PubSubConsumerProperties getExtendedConsumerProperties(String channelName) {
		if (this.bindings.containsKey(channelName)
				&& this.bindings.get(channelName).getConsumer() != null) {
			return this.bindings.get(channelName).getConsumer();
		}

		return new PubSubConsumerProperties();
	}

	@Override
	public PubSubProducerProperties getExtendedProducerProperties(String channelName) {
		if (this.bindings.containsKey(channelName)
				&& this.bindings.get(channelName).getProducer() != null) {
			return this.bindings.get(channelName).getProducer();
		}

		return new PubSubProducerProperties();
	}
}
