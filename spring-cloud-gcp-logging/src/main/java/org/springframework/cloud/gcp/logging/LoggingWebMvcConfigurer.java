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

package org.springframework.cloud.gcp.logging;

import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * MVC Adapter that adds the {@link TraceIdLoggingWebMvcInterceptor}
 *
 * @author Mike Eltsufin
 */
@Configuration
public class LoggingWebMvcConfigurer extends WebMvcConfigurerAdapter {

	private TraceIdLoggingWebMvcInterceptor interceptor;

	public LoggingWebMvcConfigurer(TraceIdLoggingWebMvcInterceptor interceptor) {
		this.interceptor = interceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		Assert.notNull(this.interceptor, "Interceptor must be provided");
		registry.addInterceptor(this.interceptor);
	}
}
