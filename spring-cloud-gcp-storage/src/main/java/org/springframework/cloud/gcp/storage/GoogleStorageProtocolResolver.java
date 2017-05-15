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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

import com.google.cloud.storage.Storage;

/**
 * @author Vinicius Carvalho
 */
public class GoogleStorageProtocolResolver implements ProtocolResolver, InitializingBean {

	private final ResourceLoader delegate;

	private final Storage storage;

	private final Logger logger = LoggerFactory.getLogger(GoogleStorageProtocolResolver.class);

	public GoogleStorageProtocolResolver(ResourceLoader delegate, Storage storage) {
		Assert.notNull(delegate, "Parent resource loader can not be null");
		Assert.notNull(storage, "Storage client can not be null");
		this.delegate = delegate;
		this.storage = storage;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (DefaultResourceLoader.class.isAssignableFrom(this.delegate.getClass())) {
			((DefaultResourceLoader) this.delegate).addProtocolResolver(this);
		}
		else {
			logger.warn("The provided delegate resource loader is not an implementation of DefaultResourceLoader. "
					+ "Custom Protocol using gs:// prefix will not be enabled.");
		}
	}

	@Override
	public Resource resolve(String location, ResourceLoader resourceLoader) {
		if (location.startsWith("gs://")) {
			return new GoogleStorageResource(this.storage, location);
		}
		return this.delegate.getResource(location);
	}
}
