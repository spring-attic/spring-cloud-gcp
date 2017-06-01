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

package org.springframework.cloud.gcp.core.autoconfig;

import com.google.auth.oauth2.GoogleCredentials;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.core.GCPProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

/**
 *
 * Base starter for Google Cloud Projects. Provide defaults for {@link GoogleCredentials}.
 * Binds properties from {@link GCPProperties}
 *
 * @author Vinicius Carvalho
 */
@Configuration
@ConditionalOnClass(GoogleCredentials.class)
@EnableConfigurationProperties(GCPProperties.class)
public class GCPContextAutoConfiguration {


	@Autowired
	private GCPProperties gcpProperties;

	@Bean
	public GoogleCredentials googleCredentials(ResourceLoader resourceLoader) throws Exception {
		if (!StringUtils.isEmpty(this.gcpProperties.getJsonKeyFile())) {
			return GoogleCredentials.fromStream(
					resourceLoader.getResource(this.gcpProperties.getJsonKeyFile())
							.getInputStream());
		}
		else {
			return GoogleCredentials.getApplicationDefault();
		}
	}

}
