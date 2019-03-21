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

package org.springframework.cloud.gcp.pubsub.integration;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the Pub/Sub message header.
 *
 * @author João André Martins
 * @author Eric Goetschalckx
 * @author Chengyuan Zhao
 */
public class PubSubHeaderMapperTests {

	/**
	 * used to check for exception messages and types.
	 */
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testFilterHeaders() {
		PubSubHeaderMapper mapper = new PubSubHeaderMapper();
		Map<String, Object> originalHeaders = new HashMap<>();
		originalHeaders.put("my header", "pantagruel's nativity");
		originalHeaders.put(NativeMessageHeaderAccessor.NATIVE_HEADERS, "deerhunter");
		MessageHeaders internalHeaders = new MessageHeaders(originalHeaders);

		Map<String, String> filteredHeaders = new HashMap<>();
		mapper.fromHeaders(internalHeaders, filteredHeaders);
		assertThat(filteredHeaders.size()).isEqualTo(1);
		assertThat(filteredHeaders.get("my header")).isEqualTo("pantagruel's nativity");
	}

	@Test
	public void testDontFilterHeaders() {
		PubSubHeaderMapper mapper = new PubSubHeaderMapper();
		mapper.setOutboundHeaderPatterns("*");
		Map<String, Object> originalHeaders = new HashMap<>();
		originalHeaders.put("my header", "pantagruel's nativity");
		MessageHeaders internalHeaders = new MessageHeaders(originalHeaders);

		Map<String, String> filteredHeaders = new HashMap<>();
		mapper.fromHeaders(internalHeaders, filteredHeaders);
		assertThat(filteredHeaders.size()).isEqualTo(3);
	}

	@Test
	public void testToHeaders() {
		PubSubHeaderMapper mapper = new PubSubHeaderMapper();
		Map<String, String> originalHeaders = new HashMap<>();
		originalHeaders.put(MessageHeaders.ID, "pantagruel's nativity");
		originalHeaders.put(MessageHeaders.TIMESTAMP, "the moon is down");
		originalHeaders.put("my header", "don't touch it");

		Map<String, Object> internalHeaders = mapper.toHeaders(originalHeaders);
		assertThat(internalHeaders.size()).isEqualTo(3);
	}

	@Test
	public void testSetInboundHeaderPatterns() {
		PubSubHeaderMapper mapper = new PubSubHeaderMapper();

		mapper.setInboundHeaderPatterns("x-*");

		Map<String, String> originalHeaders = new HashMap<>();
		String headerValue = "the moon is down";
		originalHeaders.put("x-" + MessageHeaders.TIMESTAMP, headerValue);
		originalHeaders.put("my header", "don't touch it");

		Map<String, Object> internalHeaders = mapper.toHeaders(originalHeaders);

		assertThat(internalHeaders.size()).isEqualTo(1);
		assertThat(internalHeaders.get("x-" + MessageHeaders.TIMESTAMP)).isEqualTo(headerValue);
		assertThat(internalHeaders.containsKey("my header")).isEqualTo(false);
	}

	@Test
	public void testSetInboundHeaderPatternsNullPatterns() {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("Header patterns can't be null.");

		PubSubHeaderMapper mapper = new PubSubHeaderMapper();
		mapper.setInboundHeaderPatterns(null);
	}

	@Test
	public void testSetInboundHeaderPatternsNullPatternElements() {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("No header pattern can be null.");

		PubSubHeaderMapper mapper = new PubSubHeaderMapper();
		mapper.setInboundHeaderPatterns(new String[1]);
	}
}
