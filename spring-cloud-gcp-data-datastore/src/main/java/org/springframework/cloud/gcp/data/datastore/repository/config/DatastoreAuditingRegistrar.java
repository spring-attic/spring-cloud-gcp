/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.datastore.repository.config;

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.gcp.data.datastore.repository.support.DatastoreAuditingEventListener;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport;
import org.springframework.data.auditing.config.AuditingConfiguration;

/**
 * Registers the annotations and classes for providing auditing support in Spring Data
 * Cloud Datastore.
 *
 * @author Chengyuan Zhao
 * @since 1.2
 */
public class DatastoreAuditingRegistrar extends AuditingBeanDefinitionRegistrarSupport {

	private static final String AUDITING_HANDLER_BEAN_NAME = "datastoreAuditingHandler";

	private static final String MAPPING_CONTEXT_BEAN_NAME = "datastoreMappingContext";

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableDatastoreAuditing.class;
	}

	@Override
	protected void registerAuditListenerBeanDefinition(BeanDefinition auditingHandlerDefinition,
			BeanDefinitionRegistry registry) {
		Class<?> listenerClass = DatastoreAuditingEventListener.class;
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(listenerClass)
				.addConstructorArgReference(AUDITING_HANDLER_BEAN_NAME);

		registerInfrastructureBeanWithId(builder.getRawBeanDefinition(), listenerClass.getName(), registry);
	}

	@Override
	protected BeanDefinitionBuilder getAuditHandlerBeanDefinitionBuilder(AuditingConfiguration configuration) {
		BeanDefinitionBuilder builder = configureDefaultAuditHandlerAttributes(configuration,
				BeanDefinitionBuilder.rootBeanDefinition(AuditingHandler.class));
		return builder.addConstructorArgReference(MAPPING_CONTEXT_BEAN_NAME);
	}

	@Override
	protected String getAuditingHandlerBeanName() {
		return AUDITING_HANDLER_BEAN_NAME;
	}
}
