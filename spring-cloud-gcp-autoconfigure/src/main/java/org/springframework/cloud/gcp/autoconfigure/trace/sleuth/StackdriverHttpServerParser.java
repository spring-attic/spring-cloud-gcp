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

import brave.SpanCustomizer;
import brave.http.HttpAdapter;
import brave.http.HttpServerParser;

/**
 * An {@link HttpServerParser} that fills information for Stackdriver Trace.
 *
 * <p>Based on {@link org.springframework.cloud.sleuth.instrument.web.SleuthHttpServerParser}.
 *
 * @author Ray Tsang
 */
public class StackdriverHttpServerParser extends HttpServerParser {
	private final StackdriverHttpClientParser clientParser;

	public StackdriverHttpServerParser() {
		this.clientParser = new StackdriverHttpClientParser();
	}

	@Override
	protected <Req> String spanName(HttpAdapter<Req, ?> adapter,
			Req req) {
		return this.clientParser.spanName(adapter, req);
	}

	@Override
	public <Req> void request(HttpAdapter<Req, ?> adapter, Req req,
			SpanCustomizer customizer) {
		this.clientParser.request(adapter, req, customizer);
	}

	@Override
	public <Resp> void response(HttpAdapter<?, Resp> adapter, Resp res, Throwable error,
			SpanCustomizer customizer) {
		this.clientParser.response(adapter, res, error, customizer);
	}
}
