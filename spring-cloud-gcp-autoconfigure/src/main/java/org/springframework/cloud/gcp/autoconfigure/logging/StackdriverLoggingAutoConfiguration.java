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
package org.springframework.cloud.gcp.autoconfigure.logging;

import com.google.cloud.logging.TraceLoggingEnhancer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.logging.CompositeTraceIdExtractor;
import org.springframework.cloud.gcp.logging.LoggingWebMvcConfigurer;
import org.springframework.cloud.gcp.logging.TraceIdExtractor;
import org.springframework.cloud.gcp.logging.TraceIdLoggingWebMvcInterceptor;
import org.springframework.cloud.gcp.logging.XCloudTraceIdExtractor;
import org.springframework.cloud.gcp.logging.ZipkinTraceIdExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 */
@Configuration
@ConditionalOnClass({ TraceIdLoggingWebMvcInterceptor.class, TraceLoggingEnhancer.class })
@EnableConfigurationProperties({ StackdriverLoggingProperties.class })
@ConditionalOnProperty(value = "spring.cloud.gcp.logging.enabled", matchIfMissing = true)
@Import(LoggingWebMvcConfigurer.class)
public class StackdriverLoggingAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TraceIdLoggingWebMvcInterceptor loggingWebMvcInterceptor(
			TraceIdExtractor extractor) {
		return new TraceIdLoggingWebMvcInterceptor(extractor);
	}

	@Bean
	@ConditionalOnMissingBean
	public TraceIdExtractor traceIdExtractor(
			StackdriverLoggingProperties loggingProperties) {
		TraceIdExtractorType checkedType = loggingProperties.getTraceIdExtractor();
		if (checkedType == null) {
			checkedType = TraceIdExtractorType.XCLOUD_ZIPKIN;
		}
		TraceIdExtractor extractor;
		switch (checkedType) {
		case XCLOUD:
			extractor = new XCloudTraceIdExtractor();
			break;
		case ZIPKIN:
			extractor = new ZipkinTraceIdExtractor();
			break;
		case ZIPKIN_XCLOUD:
			extractor = new CompositeTraceIdExtractor(
					new ZipkinTraceIdExtractor(), new XCloudTraceIdExtractor());
			break;
		case XCLOUD_ZIPKIN:
			extractor = new CompositeTraceIdExtractor(
					new XCloudTraceIdExtractor(), new ZipkinTraceIdExtractor());
			break;
		default:
			throw new IllegalArgumentException(checkedType + " is not a valid trace ID extractor type.");
		}
		return extractor;
	}
}
