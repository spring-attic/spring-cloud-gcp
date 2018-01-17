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

package org.springframework.cloud.gcp.stream.binder.pubsub.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.stream.binder.pubsub.PubSubMessageChannelBinder;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubExtendedBindingProperties;
import org.springframework.cloud.gcp.stream.binder.pubsub.provisioning.PubSubChannelProvisioner;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author João André Martins
 */
@Configuration
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
			PubSubTemplate pubSubTemplate) {
		return new PubSubMessageChannelBinder(true, null, pubSubChannelProvisioner, pubSubTemplate);
	}
}
