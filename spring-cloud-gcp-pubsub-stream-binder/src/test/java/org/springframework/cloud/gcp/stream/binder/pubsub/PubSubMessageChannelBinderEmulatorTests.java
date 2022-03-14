/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.stream.binder.pubsub;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubConsumerProperties;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubProducerProperties;
import org.springframework.cloud.gcp.test.EmulatorRule;
import org.springframework.cloud.gcp.test.pubsub.PubSubEmulator;
import org.springframework.cloud.stream.binder.AbstractBinderTests;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.Spy;

import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Integration tests that require the Pub/Sub emulator to be installed.
 *
 * @author João André Martins
 * @author Elena Felder
 * @author Artem Bilan
 */
public class PubSubMessageChannelBinderEmulatorTests extends
		AbstractBinderTests<PubSubTestBinder, ExtendedConsumerProperties<PubSubConsumerProperties>,
				ExtendedProducerProperties<PubSubProducerProperties>> {
	@BeforeClass
	public static void checkToRun() {
		assumeThat(System.getProperty("it.pubsub-emulator"))
				.as("PubSub emulator tests are disabled. "
						+ "Please use '-Dit.pubsub-emulator=true' to enable them. ")
				.isEqualTo("true");
	}

	/**
	 * The emulator instance, shared across tests.
	 */
	@ClassRule
	public static EmulatorRule emulator = new EmulatorRule(new PubSubEmulator());

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

	@Test
	@Ignore("Looks like there is no Kryo support in SCSt")
	public void testSendPojoReceivePojoKryoWithStreamListener() {
	}

}
