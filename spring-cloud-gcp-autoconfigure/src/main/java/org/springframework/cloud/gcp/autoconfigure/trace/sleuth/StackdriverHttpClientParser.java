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

package org.springframework.cloud.gcp.autoconfigure.trace.sleuth;

import java.net.URI;

import brave.SpanCustomizer;
import brave.http.HttpAdapter;
import brave.http.HttpClientParser;

/**
 * An {@link HttpClientParser} that fills information for Stackdriver Trace.
 *
 * <p>Based on {@link org.springframework.cloud.sleuth.instrument.web.SleuthHttpClientParser}.
 *
 * @author Ray Tsang
 */
public class StackdriverHttpClientParser extends HttpClientParser {
	@Override
	protected <Req> String spanName(HttpAdapter<Req, ?> adapter,
			Req req) {
		URI uri = URI.create(adapter.url(req));
		return uri.toASCIIString();
	}

	@Override
	public <Req> void request(HttpAdapter<Req, ?> adapter, Req req,
			SpanCustomizer customizer) {
		super.request(adapter, req, customizer);
		String url = adapter.url(req);
		URI uri = URI.create(url);

		customizer.tag("http.url", url);
		if (uri.getHost() != null) {
			customizer.tag("http.host", uri.getHost());
		}
		customizer.tag("http.path", uri.getPath());
		customizer.tag("http.method", adapter.method(req));
	}

	@Override
	public <Resp> void response(HttpAdapter<?, Resp> adapter, Resp res, Throwable error,
			SpanCustomizer customizer) {
		int statusCode = 0;
		if (res != null) {
			String route = adapter.route(res);
			if (route != null) {
				customizer.tag("http.route", route);
			}

			// Always send status code to Stackdriver
			statusCode = adapter.statusCodeAsInt(res);
			customizer.tag("http.status_code", String.valueOf(statusCode));
		}
		error(statusCode, error, customizer);
	}
}
