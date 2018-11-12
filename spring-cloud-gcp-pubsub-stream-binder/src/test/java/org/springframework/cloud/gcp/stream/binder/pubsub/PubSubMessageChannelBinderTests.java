/*
 *  Copyright 2018 original author or authors.
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

import org.junit.ClassRule;

import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubConsumerProperties;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubProducerProperties;
import org.springframework.cloud.stream.binder.AbstractBinderTests;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.Spy;

/**
 * Integration tests that require the Pub/Sub emulator to be installed.
 *
 * @author João André Martins
 * @author Elena Felder
 */
public class PubSubMessageChannelBinderTests extends
		AbstractBinderTests<PubSubTestBinder, ExtendedConsumerProperties<PubSubConsumerProperties>,
				ExtendedProducerProperties<PubSubProducerProperties>> {

	@ClassRule
	public static PubSubEmulator emulator = new PubSubEmulator();

	@Override
	protected PubSubTestBinder getBinder() {
		return new PubSubTestBinder(emulator.getEmulatorHostPort());
	}

	@Override
	protected ExtendedConsumerProperties<PubSubConsumerProperties> createConsumerProperties() {
		return new ExtendedConsumerProperties<>(new PubSubConsumerProperties());
	}

	@Override
	protected ExtendedProducerProperties<PubSubProducerProperties> createProducerProperties() {
		return new ExtendedProducerProperties<>(new PubSubProducerProperties());
	}

	@Override
	public Spy spyOn(String name) {
		return null;
	}

	@Override
	public void testClean() {
		// Do nothing. Original test tests for Lifecycle logic that we don't need.
	}

}
