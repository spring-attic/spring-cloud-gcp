/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.autoconfigure.logging;

import org.springframework.cloud.gcp.logging.extractors.TraceIdExtractor;

/**
 * {@link org.springframework.web.servlet.HandlerInterceptor} that extracts the request
 * trace ID from the "x-cloud-trace-context" HTTP header and stores it in a thread-local
 * using {@link TraceIdLoggingEnhancer#setCurrentTraceId}.
 *
 * <p>
 * The {@link TraceIdLoggingEnhancer} can then be used in a logging appender to add the
 * trace ID metadata to log messages.
 *
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 *
 * @deprecated use
 * {@link org.springframework.cloud.gcp.logging.TraceIdLoggingWebMvcInterceptor}
 */
@Deprecated
public class TraceIdLoggingWebMvcInterceptor extends
		org.springframework.cloud.gcp.logging.TraceIdLoggingWebMvcInterceptor {

	public TraceIdLoggingWebMvcInterceptor(TraceIdExtractor extractor) {
		super(extractor);
	}

}
