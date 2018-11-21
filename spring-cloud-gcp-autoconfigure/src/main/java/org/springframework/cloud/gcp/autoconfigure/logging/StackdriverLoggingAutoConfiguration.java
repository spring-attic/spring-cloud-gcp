/*
 *  Copyright 2017-2018 original author or authors.
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

import com.google.cloud.logging.logback.LoggingAppender;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.cloud.gcp.autoconfigure.logging.extractors.TraceIdExtractor;
import org.springframework.cloud.gcp.autoconfigure.logging.extractors.XCloudTraceIdExtractor;
import org.springframework.cloud.gcp.autoconfigure.trace.StackdriverTraceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * This class configures a Web MVC interceptor to capture trace IDs for log correlation.
 * This configuration is turned on only if Trace support is not used and Web MVC is used.
 * Otherwise, the MDC context will be used by the Logback appenders.
 *
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 */
@Configuration
@ConditionalOnClass({HandlerInterceptorAdapter.class, LoggingAppender.class})
@ConditionalOnMissingBean(type = "org.springframework.cloud.sleuth.autoconfig.SleuthProperties")
@AutoConfigureAfter(StackdriverTraceAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
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
	public TraceIdExtractor traceIdExtractor() {
		return new XCloudTraceIdExtractor();
	}

}
