/*
 *  Copyright 2018 original author or authors.
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
package org.springframework.cloud.gcp.logging.autoconfig;

import com.google.cloud.logging.TraceLoggingEnhancer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gcp.logging.LoggingWebMvcConfigurer;
import org.springframework.cloud.gcp.logging.TraceIdLoggingWebMvcInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Mike Eltsufin
 */
@Configuration
@ConditionalOnClass({ TraceIdLoggingWebMvcInterceptor.class, TraceLoggingEnhancer.class })
@ConditionalOnProperty(value = "spring.cloud.gcp.logging.enabled", matchIfMissing = true)
@Import(LoggingWebMvcConfigurer.class)
public class StackdriverLoggingAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TraceIdLoggingWebMvcInterceptor getLoggingWebMvcInterceptor() {
		return new TraceIdLoggingWebMvcInterceptor();
	}
}
