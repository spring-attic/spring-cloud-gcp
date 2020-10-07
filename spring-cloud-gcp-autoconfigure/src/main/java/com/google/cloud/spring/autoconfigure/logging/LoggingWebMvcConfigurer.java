/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spring.autoconfigure.logging;

import com.google.cloud.spring.core.GcpProjectIdProvider;

import org.springframework.context.annotation.Configuration;

/**
 * MVC Adapter that adds the {@link TraceIdLoggingWebMvcInterceptor}.
 *
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 *
 * @deprecated use {@link com.google.cloud.spring.logging.LoggingWebMvcConfigurer}
 */
@Configuration(proxyBeanMethods = false)
@Deprecated
public class LoggingWebMvcConfigurer extends com.google.cloud.spring.logging.LoggingWebMvcConfigurer {
	public LoggingWebMvcConfigurer(TraceIdLoggingWebMvcInterceptor interceptor,
			GcpProjectIdProvider projectIdProvider) {
		super(interceptor, projectIdProvider);
	}
}
