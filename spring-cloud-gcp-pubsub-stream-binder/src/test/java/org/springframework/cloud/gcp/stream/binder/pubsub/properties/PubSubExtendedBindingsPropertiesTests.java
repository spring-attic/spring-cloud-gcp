package org.springframework.cloud.gcp.stream.binder.pubsub.properties;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubEmulatorConfiguration;
import org.springframework.cloud.gcp.stream.binder.pubsub.PubSubMessageChannelBinder;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubExtendedBindingsPropertiesTests.PubSubBindingsTestConfiguration.CustomTestSink;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.BinderFactory;
import org.springframework.cloud.stream.config.BindingServiceConfiguration;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

import static org.assertj.core.api.Assertions.assertThat;

public class PubSubExtendedBindingsPropertiesTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues(
				"spring.cloud.stream.gcp.pubsub.bindings.input.consumer.auto-create-resources=true",
					"spring.cloud.stream.gcp.pubsub.default.consumer.auto-create-resources=false")
			.withConfiguration(AutoConfigurations.of(
					GcpPubSubEmulatorConfiguration.class,
					GcpContextAutoConfiguration.class,
					GcpPubSubAutoConfiguration.class,
					BindingServiceConfiguration.class,
					PubSubBindingsTestConfiguration.class));

	@Test
	public void testExtendedPropertiesOverrideDefaults() {
		this.contextRunner.run(context -> {
			BinderFactory binderFactory = context.getBeanFactory().getBean(BinderFactory.class);
			PubSubMessageChannelBinder binder = (PubSubMessageChannelBinder) binderFactory.getBinder("pubsub",
					MessageChannel.class);

			assertThat(binder.getExtendedConsumerProperties("custom-in").isAutoCreateResources()).isFalse();
			assertThat(binder.getExtendedConsumerProperties("input").isAutoCreateResources()).isTrue();
		});
	}

	@Configuration
	@EnableBinding(CustomTestSink.class)
	static class PubSubBindingsTestConfiguration {

		@StreamListener("input")
		public void process(String payload) {
			System.out.println(payload);
		}

		@StreamListener("custom-in")
		public void processCustom(String payload) {
			System.out.println(payload);
		}

		interface CustomTestSink extends Sink {
			@Input("custom-in")
			SubscribableChannel customIn();
		}
	}
}

