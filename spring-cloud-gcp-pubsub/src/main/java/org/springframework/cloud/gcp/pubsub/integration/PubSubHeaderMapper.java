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

package org.springframework.cloud.gcp.pubsub.integration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

public class PubSubHeaderMapper implements HeaderMapper<Map<String, String>> {

	/**
	 * Headers to exclude in the {@link #fromHeaders(MessageHeaders, Map)}.
	 */
	private String[] doNotMapHeaderNames = {
			MessageHeaders.ID, MessageHeaders.TIMESTAMP
	};

	/**
	 * Enables/disables header filtering in {@link #fromHeaders(MessageHeaders, Map)}.
	 */
	private boolean filterHeaders;

	/**
	 * Set the names of the headers to filtered out in {@link #fromHeaders(MessageHeaders, Map)}.
	 * @param doNotMapHeaderNames
	 */
	public void setDoNotMapHeaderNames(String... doNotMapHeaderNames) {
		Assert.notNull(doNotMapHeaderNames, "Header names can't be null.");
		Assert.noNullElements(doNotMapHeaderNames, "No header name can be null.");
		this.doNotMapHeaderNames = Arrays.copyOf(doNotMapHeaderNames, doNotMapHeaderNames.length);
	}

	/**
	 * Generate headers in {@link com.google.pubsub.v1.PubsubMessage} format from
	 * {@link MessageHeaders}. All headers are converted into strings.
	 *
	 * <p>May filter out designated headers, depending on whether {@code filterHeaders} is
	 * {@code true}.
	 * @param messageHeaders headers to map from
	 * @param pubsubMessageHeaders headers in their final format
	 */
	@Override
	public void fromHeaders(MessageHeaders messageHeaders,
			final Map<String, String> pubsubMessageHeaders) {
		messageHeaders.forEach((key, value) -> pubsubMessageHeaders.put(key, value.toString()));

		if (this.filterHeaders) {
			Arrays.stream(this.doNotMapHeaderNames).forEach(pubsubMessageHeaders::remove);
		}
	}

	/**
	 * Generate headers in {@link org.springframework.messaging.Message} format from
	 * {@code Map<String, String>}.
	 * @param pubsubMessageHeaders headers in {@link com.google.pubsub.v1.PubsubMessage} format
	 * @return a map with headers in the {@link org.springframework.messaging.Message} format
	 */
	@Override
	public Map<String, Object> toHeaders(Map<String, String> pubsubMessageHeaders) {
		Map<String, Object> typifiedHeaders = new HashMap<>();
		pubsubMessageHeaders.forEach((key, value) -> typifiedHeaders.put(key, value));
		return new MessageHeaders(typifiedHeaders);
	}

	public void setFilterHeaders(boolean filterHeaders) {
		this.filterHeaders = filterHeaders;
	}
}
