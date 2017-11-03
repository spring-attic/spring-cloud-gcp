package org.springframework.cloud.gcp.stream.binder.pubsub;

import org.junit.Rule;

import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubConsumerProperties;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubProducerProperties;
import org.springframework.cloud.stream.binder.AbstractBinderTests;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.Spy;

/**
 * @author Andreas Berger
 */
public class PubSubMessageChannelBinderTest extends
		AbstractBinderTests<PubSubTestBinder, ExtendedConsumerProperties<PubSubConsumerProperties>, ExtendedProducerProperties<PubSubProducerProperties>> {

	@Rule
	public PubSubTestSupport rule = new PubSubTestSupport();



	@Override
	protected PubSubTestBinder getBinder() throws Exception {
		if (testBinder == null) {
			testBinder = new PubSubTestBinder("test", true, rule.getResource());
		}
		return testBinder;
	}

	@Override
	protected ExtendedConsumerProperties<PubSubConsumerProperties> createConsumerProperties() {
		return new ExtendedConsumerProperties<>(new PubSubConsumerProperties());
	}

	@Override
	protected ExtendedProducerProperties<PubSubProducerProperties> createProducerProperties() {
		PubSubProducerProperties properties = new PubSubProducerProperties();
		return new ExtendedProducerProperties<>(properties);
	}

	@Override
	public Spy spyOn(String name) {
		return null;
	}
}