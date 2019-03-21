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

package org.springframework.cloud.gcp.stream.binder.pubsub.properties;

import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;

/**
 * Binder-specific properties for Pub/Sub.
 *
 * @author João André Martins
 * @author Artem Bilan
 */
public class PubSubBindingProperties implements BinderSpecificPropertiesProvider {

	private PubSubConsumerProperties consumer = new PubSubConsumerProperties();

	private PubSubProducerProperties producer = new PubSubProducerProperties();

	public PubSubConsumerProperties getConsumer() {
		return this.consumer;
	}

	public void setConsumer(PubSubConsumerProperties consumer) {
		this.consumer = consumer;
	}

	public PubSubProducerProperties getProducer() {
		return this.producer;
	}

	public void setProducer(PubSubProducerProperties producer) {
		this.producer = producer;
	}

}
