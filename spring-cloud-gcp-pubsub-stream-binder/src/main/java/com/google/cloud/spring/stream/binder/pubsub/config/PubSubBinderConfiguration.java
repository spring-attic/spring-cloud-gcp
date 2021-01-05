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

package com.google.cloud.spring.stream.binder.pubsub.config;

import java.util.Collections;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.integration.outbound.PubSubMessageHandler;
import com.google.cloud.spring.stream.binder.pubsub.PubSubMessageChannelBinder;
import com.google.cloud.spring.stream.binder.pubsub.properties.PubSubExtendedBindingProperties;
import com.google.cloud.spring.stream.binder.pubsub.provisioning.PubSubChannelProvisioner;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.config.BindingHandlerAdvise.MappingsProvider;
import org.springframework.cloud.stream.config.ConsumerEndpointCustomizer;
import org.springframework.cloud.stream.config.ProducerMessageHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

/**
 * Pub/Sub binder configuration.
 *
 * @author João André Martins
 * @author Daniel Zou
 * @author Mike Eltsufin
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(Binder.class)
@EnableConfigurationProperties(PubSubExtendedBindingProperties.class)
public class PubSubBinderConfiguration {

	@Bean
	public PubSubChannelProvisioner pubSubChannelProvisioner(PubSubAdmin pubSubAdmin) {
		return new PubSubChannelProvisioner(pubSubAdmin);
	}

	@Bean
	public PubSubMessageChannelBinder pubSubBinder(
			PubSubChannelProvisioner pubSubChannelProvisioner,
			PubSubTemplate pubSubTemplate,
			PubSubExtendedBindingProperties pubSubExtendedBindingProperties,
			@Nullable ProducerMessageHandlerCustomizer<PubSubMessageHandler> producerCustomizer,
			@Nullable ConsumerEndpointCustomizer<PubSubInboundChannelAdapter> consumerCustomizer
	) {
		PubSubMessageChannelBinder binder = new PubSubMessageChannelBinder(null, pubSubChannelProvisioner, pubSubTemplate,
				pubSubExtendedBindingProperties);
		binder.setProducerMessageHandlerCustomizer(producerCustomizer);
		binder.setConsumerEndpointCustomizer(consumerCustomizer);
		return binder;
	}

	@Bean
	public MappingsProvider pubSubExtendedPropertiesDefaultMappingsProvider() {
		return () -> Collections.singletonMap(
				ConfigurationPropertyName.of("spring.cloud.stream.gcp.pubsub.bindings"),
				ConfigurationPropertyName.of("spring.cloud.stream.gcp.pubsub.default"));
	}
}
