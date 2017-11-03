package org.springframework.cloud.gcp.stream.binder.pubsub;

import java.util.concurrent.Executors;

import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedExecutorProvider;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.DefaultPublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.DefaultSubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubConsumerProperties;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubProducerProperties;
import org.springframework.cloud.gcp.stream.binder.pubsub.provisioning.PubSubChannelProvisioner;
import org.springframework.cloud.stream.binder.AbstractTestBinder;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.codec.kryo.PojoCodec;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Andreas Berger
 */
public class PubSubTestBinder extends
		AbstractTestBinder<PubSubMessageChannelBinder, ExtendedConsumerProperties<PubSubConsumerProperties>, ExtendedProducerProperties<PubSubProducerProperties>> {

	private final PubSubAdmin pubSubAdmin;

	public PubSubTestBinder(String project, boolean supportsHeadersNatively, PubSubSupport pubSubSupport) {
		GcpProjectIdProvider projectIdProvider = () -> project;

		pubSubAdmin = new PubSubAdmin(projectIdProvider,
				pubSubSupport.getTopicAdminClient(),
				pubSubSupport.getSubscriptionAdminClient());

		ExecutorProvider executor = FixedExecutorProvider.create(Executors.newScheduledThreadPool(5));

		PublisherFactory publisherFactory = new DefaultPublisherFactory(
				projectIdProvider,
				executor,
				pubSubSupport.getChannelProvider(),
				pubSubSupport.getCredentialsProvider());

		SubscriberFactory subscriberFactory = new DefaultSubscriberFactory(
				projectIdProvider,
				executor,
				pubSubSupport.getChannelProvider(),
				pubSubSupport.getCredentialsProvider());

		PubSubMessageChannelBinder binder = new PubSubMessageChannelBinder(
				supportsHeadersNatively,
				new String[0],
				new PubSubChannelProvisioner(pubSubAdmin),
				new PubSubTemplate(publisherFactory, subscriberFactory));

		GenericApplicationContext context = new GenericApplicationContext();
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.afterPropertiesSet();
		context.getBeanFactory().registerSingleton(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, scheduler);
		context.refresh();
		binder.setApplicationContext(context);
		binder.setCodec(new PojoCodec());
		this.setBinder(binder);

	}

	@Override
	public void cleanup() {
		pubSubAdmin.listSubscriptions().forEach(subscription -> {
			System.out.println("Deleting subscription: " + subscription.getName());
			pubSubAdmin.deleteSubscription(subscription.getNameAsSubscriptionName().getSubscription());
		});
		pubSubAdmin.listTopics().forEach(topic -> {
			System.out.println("Deleting topic: " + topic.getName());
			pubSubAdmin.deleteTopic(topic.getNameAsTopicName().getTopic());
		});
	}
}
