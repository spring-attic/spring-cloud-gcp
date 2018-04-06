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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import zipkin2.Annotation;
import zipkin2.Span;

import org.springframework.cloud.sleuth.TraceKeys;

/**
 * Translated from Adrian Cole's
 * <a href= "https://github.com/GoogleCloudPlatform/stackdriver-zipkin/">Stackdriver
 * Zipkin Proxy</a>.
 *
 * <p>This extracts Stackdriver Span labels equivalent from Zipkin Span.
 *
 * <p>Zipkin Span annotations with equivalent Stackdriver labels will be renamed to the
 * Stackdriver name. Any Sleuth Span without a Stackdriver label equivalent are renamed to
 * spring.sleuth/[key_name]
 *
 * @author Ray Tsang
 */
public class LabelExtractor {

	public static final String DEFAULT_AGENT_NAME = "spring-cloud-gcp-trace";

	public static final String DEFAULT_PREFIX = "cloud.spring.io/";

	public static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd (HH:mm:ss.SSS z)";

	private final String agentName;

	private final String prefix;

	private final Map<String, String> labelRenameMap;

	private final DateFormat timestampFormat;

	public LabelExtractor() {
		this(newDefaultLabelRenameMap());
	}

	public LabelExtractor(TraceKeys traceKeys) {
		this(newDefaultLabelRenameMap(traceKeys));
	}

	public LabelExtractor(Map<String, String> labelRenameMap) {
		this(DEFAULT_AGENT_NAME, DEFAULT_PREFIX, labelRenameMap, new SimpleDateFormat(DEFAULT_TIMESTAMP_FORMAT));
	}

	public LabelExtractor(String agentName, String prefix, Map<String, String> labelRenameMap,
			DateFormat timestampFormat) {
		this.agentName = agentName;
		this.prefix = prefix;
		this.labelRenameMap = labelRenameMap;
		this.timestampFormat = timestampFormat;
	}

	/**
	 * Default Zipkin to Stackdriver tag/label mapping if not using legacy mode.
	 *
	 * @return new instance of a Map with the label mapping
	 */
	public static Map<String, String> newDefaultLabelRenameMap() {
		Map<String, String> labelRenameMap = new HashMap<>();
		labelRenameMap = new HashMap<>();

		labelRenameMap.put("http.host", "/http/host");
		labelRenameMap.put("http.method", "/http/method");
		labelRenameMap.put("http.status_code", "/http/status_code");
		labelRenameMap.put("http.request.size", "/http/request/size");
		labelRenameMap.put("http.response.size", "/http/response/size");
		labelRenameMap.put("http.url", "/http/url");
		labelRenameMap.put("http.path", "/http/path");
		labelRenameMap.put("http.route", "/http/route");

		return labelRenameMap;
	}

	/**
	 * Default Sleuth to Stackdriver tag/label mapping if using legacy mode.
	 * @param traceKeys
	 * @return new instance of a Map with the label mapping
	 */
	public static Map<String, String> newDefaultLabelRenameMap(TraceKeys traceKeys) {
		Map<String, String> labelRenameMap = new HashMap<>();
		TraceKeys.Http httpKeys = traceKeys.getHttp();
		labelRenameMap = new HashMap<>();
		labelRenameMap.put(httpKeys.getHost(), "/http/host");
		labelRenameMap.put(httpKeys.getMethod(), "/http/method");
		labelRenameMap.put(httpKeys.getStatusCode(), "/http/status_code");
		labelRenameMap.put(httpKeys.getRequestSize(), "/http/request/size");
		labelRenameMap.put(httpKeys.getResponseSize(), "/http/response/size");
		labelRenameMap.put(httpKeys.getUrl(), "/http/url");
		labelRenameMap.put(httpKeys.getPath(), "/http/path");
		return labelRenameMap;
	}

	/**
	 * Extracts the Stackdriver span labels that are equivalent to the Zipkin Span
	 * annotations.
	 *
	 * @param zipkinSpan The Zipkin Span
	 * @return A map of the Stackdriver span labels equivalent to the Zipkin annotations.
	 */
	public Map<String, String> extract(Span zipkinSpan) {
		Map<String, String> result = new LinkedHashMap<>();
		for (Map.Entry<String, String> tag : zipkinSpan.tags().entrySet()) {
			result.put(label(tag.getKey()), tag.getValue());
		}

		// Only use server receive spans to extract endpoint data as spans
		// will be rewritten into multiple single-host Stackdriver spans. A client send
		// trace might not show the final destination.
		if (zipkinSpan.localEndpoint() != null && zipkinSpan.kind() == Span.Kind.SERVER) {
			if (zipkinSpan.localEndpoint().ipv4() != null) {
				result.put(label("endpoint.ipv4"), zipkinSpan.localEndpoint().ipv4());
			}
			if (zipkinSpan.localEndpoint().ipv6() != null) {
				result.put(label("endpoint.ipv6"), zipkinSpan.localEndpoint().ipv6());
			}
		}

		for (Annotation annotation : zipkinSpan.annotations()) {
			result.put(label(annotation.value()), formatTimestamp(annotation.timestamp()));
		}

		if (zipkinSpan.localEndpoint() != null && !zipkinSpan.localEndpoint().serviceName().isEmpty()) {
			result.put("/component", zipkinSpan.localEndpoint().serviceName());
		}

		if (zipkinSpan.parentId() == null) {
			String agentName = System.getProperty("stackdriver.trace.zipkin.agent", "spring-cloud-gcp-trace");
			result.put("/agent", agentName);
		}

		return result;
	}

	protected String label(String key) {
		if (this.labelRenameMap.containsKey(key)) {
			return this.labelRenameMap.get(key);
		}
		else {
			return this.prefix + key;
		}
	}

	protected String formatTimestamp(long milliseconds) {
		return this.timestampFormat.format(new Date(milliseconds));
	}
}
