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

package org.springframework.cloud.gcp.storage;

import com.google.cloud.storage.Storage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

/**
 * A {@link BeanFactoryPostProcessor} implementation to register
 * {@link GoogleStorageProtocolResolver} with the {@link DefaultResourceLoader}. The
 * registration is performed only if {@link Storage} bean is declared in the application
 * context.
 * <p>
 * This class must be used as a {@code static @Bean} method definition or just
 * {@code @Import}ed on some {@code @Configuration} class.
 * 
 * @author Artem Bilan
 *
 */
public class GoogleStorageProtocolResolverBeanFactoryPostProcessor
		implements BeanFactoryPostProcessor, ResourceLoaderAware {

	private static final Log logger = LogFactory.getLog(GoogleStorageProtocolResolverBeanFactoryPostProcessor.class);

	private DefaultResourceLoader resourceLoader;

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		if (DefaultResourceLoader.class.isAssignableFrom(resourceLoader.getClass())) {
			this.resourceLoader = (DefaultResourceLoader) resourceLoader;
		}
		else {
			logger.warn("The provided delegate resource loader is not an implementation " +
					"of DefaultResourceLoader. Custom Protocol using gs:// prefix will not be " +
					"enabled.");
		}
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		try {
			Storage storage = beanFactory.getBean(Storage.class);
			GoogleStorageProtocolResolver googleStorageProtocolResolver = new GoogleStorageProtocolResolver(storage);
			this.resourceLoader.addProtocolResolver(googleStorageProtocolResolver);
		}
		catch (NoSuchBeanDefinitionException e) {
			logger.warn("No 'com.google.cloud.storage.Storage' bean in the application context." +
					" Custom Protocol using gs:// prefix will not be enabled.");
		}
	}

}
