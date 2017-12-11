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
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * A {@link ProtocolResolver} implementation for the {@code gs://} protocol.
 *
 * @author Vinicius Carvalho
 * @author Artem Bilan
 */
public class GoogleStorageProtocolResolver
		implements ProtocolResolver, BeanFactoryPostProcessor, ResourceLoaderAware {

	public static final String PROTOCOL = "gs://";

	private static final Log logger = LogFactory
			.getLog(GoogleStorageProtocolResolver.class);

	private ConfigurableListableBeanFactory beanFactory;

	private GoogleStorageProtocolResolverSettings settings = new GoogleStorageProtocolResolverSettings();

	private volatile Storage storage;

	GoogleStorageProtocolResolver() {
	}

	@Override
	public void postProcessBeanFactory(
			ConfigurableListableBeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		if (DefaultResourceLoader.class.isAssignableFrom(resourceLoader.getClass())) {
			((DefaultResourceLoader) resourceLoader).addProtocolResolver(this);
		}
		else {
			logger.warn("The provided delegate resource loader is not an implementation "
					+ "of DefaultResourceLoader. Custom Protocol using gs:// prefix will not be enabled.");
		}
	}

	@Override
	public Resource resolve(String location, ResourceLoader resourceLoader) {
		if (!location.startsWith(PROTOCOL)) {
			return null;
		}

		String pathWithoutProtocol = location.substring(PROTOCOL.length());

		String[] pathParts = pathWithoutProtocol.split("/");

		if (pathParts.length == 0 || pathWithoutProtocol.isEmpty()) {
			return null;
		}

		String bucketName = pathParts[0];

		if (this.storage == null) {
			this.storage = this.beanFactory.getBean(Storage.class);
			try {
				this.settings = this.beanFactory
						.getBean(GoogleStorageProtocolResolverSettings.class);
			}
			catch (NoSuchBeanDefinitionException e) {
				logger.info("There is no bean definition for the resolver settings, "
						+ "so defaults are used.");
			}
		}

		Resource resource;

		if (pathParts.length < 2) {
			resource = new GoogleStorageResourceBucket(this.storage, bucketName,
					this.settings.isAutoCreateFiles());
		}
		else {
			resource = new GoogleStorageResourceObject(this.storage, location,
					this.settings.isAutoCreateFiles());
		}
		return resource;
	}

}
