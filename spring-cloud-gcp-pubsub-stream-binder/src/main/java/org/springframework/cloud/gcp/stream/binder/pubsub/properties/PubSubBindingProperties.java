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

/**
 * @author João André Martins
 */
public class PubSubBindingProperties {

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
