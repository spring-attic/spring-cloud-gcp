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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.core.GcpProperties;
import org.springframework.cloud.gcp.storage.GoogleStorageProtocolResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * An auto-configuration for Google {@link Storage} bean definition. Also it
 * {@link Import} a {@link GoogleStorageProtocolResolver} to register it
 * with the {@code DefaultResourceLoader}.
 *
 * @author Vinicius Carvalho
 * @author Artem Bilan
 *
 * @see GoogleStorageProtocolResolver
 */
@Configuration
@ConditionalOnClass(Storage.class)
@EnableConfigurationProperties(GcpProperties.class)
@Import(GoogleStorageProtocolResolver.class)
public class StorageAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public static Storage storage(GoogleCredentials credentials) {
		return StorageOptions.newBuilder()
				.setCredentials(credentials)
				.build()
				.getService();
	}

}
