/*
 * Copyright 2017-2018 the original author or authors.
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

package com.google.cloud.spring.autoconfigure.trace.sleuth;

import java.net.URI;

import brave.SpanCustomizer;
import brave.http.HttpRequest;
import brave.http.HttpRequestParser;
import brave.http.HttpTags;
import brave.propagation.TraceContext;


/**
 * An {@link HttpRequestParser} that fills information for Stackdriver Trace.
 *
 * <p>Based on {@code org.springframework.cloud.sleuth.instrument.web.SleuthHttpClientParser}.
 *
 * @author Ray Tsang
 * @author Travis Tomsu
 */
public class StackdriverHttpRequestParser implements HttpRequestParser {

	@Override
	public void parse(HttpRequest request, TraceContext context, SpanCustomizer customizer) {
		HttpRequestParser.DEFAULT.parse(request, context, customizer);
		HttpTags.URL.tag(request, context, customizer);
		HttpTags.ROUTE.tag(request, context, customizer);

		String url = request.url();
		URI uri = URI.create(url);
		if (uri.getHost() != null) {
			customizer.tag("http.host", uri.getHost());
		}
	}
}
