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
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.support.utils.PatternMatchUtils;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.Assert;

/**
 * Maps headers from {@link com.google.pubsub.v1.PubsubMessage}s to
 * {@link org.springframework.messaging.Message}s and vice-versa.
 *
 * <p>By default, filters out headers called "id", "timestamp", "gcp_pubsub_acknowledgement" or
 * "nativeHeaders" on the {@link org.springframework.messaging.Message} to
 * {@link com.google.pubsub.v1.PubsubMessage} header conversion.
 *
 * @author João André Martins
 */
public class PubSubHeaderMapper implements HeaderMapper<Map<String, String>> {

	/**
	 * Patterns of headers to map in {@link #fromHeaders(MessageHeaders, Map)}.
	 * First patterns take precedence.
	 */
	@SuppressWarnings("deprecation")
	private String[] outboundHeaderPatterns = {
			"!" + MessageHeaders.ID,
			"!" + MessageHeaders.TIMESTAMP,
			"!" + GcpPubSubHeaders.ACKNOWLEDGEMENT,
			"!" + GcpPubSubHeaders.ORIGINAL_MESSAGE,
			"!" + NativeMessageHeaderAccessor.NATIVE_HEADERS,
			"*"};

	/**
	 * Patterns of headers to map in {@link #toHeaders(Map)}.
	 * First patterns take precedence.
	 */
	private String[] inboundHeaderPatterns = {"*"};

	/**
	 * Set the patterns of the headers to be mapped in {@link #fromHeaders(MessageHeaders, Map)}.
	 * First patterns take precedence.
	 * @param outboundHeaderPatterns header patterns to be mapped
	 */
	public void setOutboundHeaderPatterns(String... outboundHeaderPatterns) {
		Assert.notNull(outboundHeaderPatterns, "Header patterns can't be null.");
		Assert.noNullElements(outboundHeaderPatterns, "No header pattern can be null.");
		this.outboundHeaderPatterns =
				Arrays.copyOf(outboundHeaderPatterns, outboundHeaderPatterns.length);
	}

	/**
	 * Set the patterns of the headers to be mapped in {@link #toHeaders(Map)}.
	 * First patterns take precedence.
	 * @param inboundHeaderPatterns header patterns to be mapped
	 */
	public void setInboundHeaderPatterns(String... inboundHeaderPatterns) {
		Assert.notNull(inboundHeaderPatterns, "Header patterns can't be null.");
		Assert.noNullElements(inboundHeaderPatterns, "No header pattern can be null.");
		this.inboundHeaderPatterns =
				Arrays.copyOf(inboundHeaderPatterns, inboundHeaderPatterns.length);
	}

	/**
	 * Generate headers in {@link com.google.pubsub.v1.PubsubMessage} format from
	 * {@link MessageHeaders}. All headers are converted into strings.
	 * <p>Will map only the headers that match the patterns in {@code outboundHeaderPatternsMap}.
	 * @param messageHeaders headers to map from
	 * @param pubsubMessageHeaders headers in their final format
	 */
	@Override
	public void fromHeaders(MessageHeaders messageHeaders,
			final Map<String, String> pubsubMessageHeaders) {
		messageHeaders.entrySet().stream()
				.filter(entry -> Boolean.TRUE.equals(
						PatternMatchUtils.smartMatch(entry.getKey(),
								this.outboundHeaderPatterns)))
				.forEach(entry -> pubsubMessageHeaders.put(
						entry.getKey(), entry.getValue().toString()));
	}

	/**
	 * Generate headers in {@link org.springframework.messaging.Message} format from
	 * {@code Map<String, String>}.
	 * <p>Will map only the headers that match the patterns in {@code inboundHeaderPatternsMap}.
	 * @param pubsubMessageHeaders headers in {@link com.google.pubsub.v1.PubsubMessage} format
	 * @return a map with headers in the {@link org.springframework.messaging.Message} format
	 */
	@Override
	public Map<String, Object> toHeaders(Map<String, String> pubsubMessageHeaders) {
		return pubsubMessageHeaders.entrySet().stream()
				.filter(entry -> Boolean.TRUE.equals(
						PatternMatchUtils.smartMatch(entry.getKey(),
								this.inboundHeaderPatterns)))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
}
