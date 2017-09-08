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
package org.springframework.cloud.gcp.trace.sleuth;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.devtools.cloudtrace.v1.TraceSpan;

import org.springframework.cloud.sleuth.Log;
import org.springframework.cloud.sleuth.Span;
import org.springframework.util.StringUtils;

/**
 * Translated from Andrian Cole's
 * <a href= "https://github.com/GoogleCloudPlatform/stackdriver-zipkin/">Stackdriver
 * Zipkin Proxy</a>.
 *
 * This extracts Stackdriver Span labels equivalent from Sleuth Span.
 *
 * Sleuth Span Logs are converted to Stackdriver Span labels by using
 * {@link Log#getEvent()} as the key and {@link Log#getTimestamp()} as the value.
 *
 * Sleuth Span tags with equivalent Stackdriver labels will be renamed to the Stackdriver
 * name. Any Sleuth Span without a Stackdriver label equivalent are renamed to
 * spring.sleuth/[key_name]
 *
 * @author Ray Tsang
 */
public class LabelExtractor {
	public static final String DEFAULT_AGENT_NAME = "spring-cloud-gcp-trace";

	public static final String DEFAULT_PREFIX = "cloud.spring.io/";

	public static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd (HH:mm:ss.SSS)";

	private static final Map<String, String> DEFAULT_RENAME_MAP;
	static {
		DEFAULT_RENAME_MAP = new HashMap<>();
		DEFAULT_RENAME_MAP.put("http.host", "/http/host");
		DEFAULT_RENAME_MAP.put("http.method", "/http/method");
		DEFAULT_RENAME_MAP.put("http.status_code", "/http/status_code");
		DEFAULT_RENAME_MAP.put("http.request.size", "/request/size");
		DEFAULT_RENAME_MAP.put("http.response.size", "/response/size");
		DEFAULT_RENAME_MAP.put("http.url", "/http/url");
	}

	private final String agentName;

	private final String prefix;

	private final Map<String, String> labelRenameMap;

	private final DateFormat timestampFormat;

	public LabelExtractor() {
		this(DEFAULT_AGENT_NAME, DEFAULT_PREFIX, DEFAULT_RENAME_MAP, new SimpleDateFormat(DEFAULT_TIMESTAMP_FORMAT));
	}

	public LabelExtractor(String agentName, String prefix, Map<String, String> labelRenameMap,
			DateFormat timestampFormat) {
		this.agentName = agentName;
		this.prefix = prefix;
		this.labelRenameMap = labelRenameMap;
		this.timestampFormat = timestampFormat;
	}

	public Map<String, String> extract(Span span, TraceSpan.SpanKind kind, String instanceId) {
		Map<String, String> labels = new HashMap<>();

		for (Map.Entry<String, String> tag : span.tags().entrySet()) {
			labels.put(label(tag.getKey()), tag.getValue());
		}

		for (Log log : span.logs()) {
			labels.put(label(log.getEvent()), formatTimestamp(log.getTimestamp()));
		}

		if (span.tags().containsKey(Span.SPAN_PEER_SERVICE_TAG_NAME)) {
			labels.put("/component", span.tags().get(Span.SPAN_PEER_SERVICE_TAG_NAME));
		}

		if (span.getParents() == null || span.getParents().isEmpty()) {
			labels.put("/agent", this.agentName);
		}

		if ((kind == TraceSpan.SpanKind.RPC_CLIENT || kind == TraceSpan.SpanKind.RPC_SERVER)
				&& StringUtils.hasText(instanceId)) {
			if (StringUtils.hasText(instanceId)) {
				labels.put(label(Span.INSTANCEID), instanceId);
			}
		}

		return labels;
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
