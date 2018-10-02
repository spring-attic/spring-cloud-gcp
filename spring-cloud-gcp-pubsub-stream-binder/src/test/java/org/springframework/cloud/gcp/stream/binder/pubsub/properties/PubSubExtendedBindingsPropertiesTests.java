package org.springframework.cloud.gcp.stream.binder.pubsub.properties;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gcp.stream.binder.pubsub.PubSubMessageChannelBinder;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.BinderFactory;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
				"spring.cloud.stream.gcp.pubsub.bindings.input.consumer.auto-create-resources=true",
				"spring.cloud.stream.gcp.pubsub.default.consumer.auto-create-resources=false",
		})
public class PubSubExtendedBindingsPropertiesTests {

	@Autowired
	private ConfigurableApplicationContext context;

	@Test
	public void testExtendedProperties() {
		BinderFactory binderFactory = context.getBeanFactory().getBean(BinderFactory.class);
		PubSubMessageChannelBinder binder = (PubSubMessageChannelBinder) binderFactory.getBinder("pubsub", MessageChannel.class);

		assertThat(binder.getExtendedConsumerProperties("custom-in").isAutoCreateResources()).isFalse();
		assertThat(binder.getExtendedConsumerProperties("input").isAutoCreateResources()).isTrue();
	}

	@EnableBinding(CustomTestSink.class)
	public static class PubSubTestBindings {

		@StreamListener("input")
		public void process(String payload) {
			System.out.println(payload);
		}

		@StreamListener("custom-in")
		public void processCustom(String payload) {
			System.out.println(payload);
		}
	}

	interface CustomTestSink extends Sink {
		@Input("custom-in")
		SubscribableChannel customIn();
	}
}


