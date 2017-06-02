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

package org.springframework.cloud.gcp.storage.autoconfig;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gcp.storage.GoogleStorageProtocolResolver;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

/**
 * @author Vinicius Carvalho
 */
@Configuration
@ConditionalOnClass(Storage.class)
public class StorageAutoConfiguration implements ResourceLoaderAware {

	private ResourceLoader defaultResourceLoader;

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.defaultResourceLoader = resourceLoader;
	}

	@Bean
	@ConditionalOnMissingBean(Storage.class)
	public Storage storage(GoogleCredentials credentials) throws Exception {
		return StorageOptions.newBuilder().setCredentials(credentials).build()
				.getService();
	}

	@Bean
	public GoogleStorageProtocolResolver googleStorageProtocolResolver(Storage storage) {
		return new GoogleStorageProtocolResolver(this.defaultResourceLoader, storage);
	}
}
