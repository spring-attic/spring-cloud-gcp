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

package org.springframework.cloud.gcp.logging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * MVC Adapter that adds the {@link TraceIdLoggingWebMvcInterceptor}
 *
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 */
@Configuration
public class LoggingWebMvcConfigurer extends WebMvcConfigurerAdapter {

	private final TraceIdLoggingWebMvcInterceptor interceptor;

	public LoggingWebMvcConfigurer(
			@Autowired(required = false) TraceIdLoggingWebMvcInterceptor interceptor,
			LoggingWebMvcConfigurerSettings settings) {
		if (interceptor != null) {
			this.interceptor = interceptor;
		}
		else {
			this.interceptor = new TraceIdLoggingWebMvcInterceptor(
					getCompositeExtractor(settings.getExtractorCombination()));
		}
	}

	/**
	 * Gets a {@link CompositeHeaderTraceIdExtractor} based on
	 * {@link TraceIdExtractorCombination}. Defaults to XCLOUD_ZIPKIN.
	 * @param combination the enum indication the combination.
	 * @return the composite trace ID extractor.
	 */
	public static CompositeHeaderTraceIdExtractor getCompositeExtractor(
			TraceIdExtractorCombination combination) {
		CompositeHeaderTraceIdExtractor extractor = new CompositeHeaderTraceIdExtractor(
				new XCloudTraceIdExtractor(), new ZipkinTraceIdExtractor());
		if (combination == null) {
			return extractor;
		}
		switch (combination) {
		case XCLOUD:
			extractor = new CompositeHeaderTraceIdExtractor(new XCloudTraceIdExtractor());
			break;
		case ZIPKIN:
			extractor = new CompositeHeaderTraceIdExtractor(new ZipkinTraceIdExtractor());
			break;
		case ZIPKIN_XCLOUD:
			extractor = new CompositeHeaderTraceIdExtractor(new ZipkinTraceIdExtractor(),
					new XCloudTraceIdExtractor());
			break;
		}
		return extractor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(this.interceptor);
	}
}
