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

import java.text.SimpleDateFormat;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import zipkin2.Endpoint;
import zipkin2.Span;

import org.springframework.cloud.sleuth.TraceKeys;

/**
 * @author Ray Tsang
 */
public class LabelExtractorTests {
	SimpleDateFormat dateFormatter = new SimpleDateFormat(LabelExtractor.DEFAULT_TIMESTAMP_FORMAT);

	@Test
	public void testRpcClientBasics() {
		LabelExtractor extractor = new LabelExtractor();
		String instanceId = "localhost";
		long begin = 1238912378081L;
		long end = 1238912378123L;
		Span span = Span.newBuilder()
				.traceId("123")
				.id("9999")
				.timestamp(begin)
				.duration(end - begin)
				.putTag("http.host", "localhost")
				.putTag("custom-tag", "hello")
				.localEndpoint(Endpoint.newBuilder().serviceName("hello-service").build())
				.build();

		Map<String, String> labels = extractor.extract(span);

		Assert.assertNotNull("span shouldn't be null", span);
		Assert.assertEquals("localhost", labels.get("/http/host"));
		Assert.assertEquals("spring-cloud-gcp-trace", labels.get("/agent"));
		Assert.assertEquals("hello-service", labels.get("/component"));
		Assert.assertEquals("hello", labels.get("cloud.spring.io/custom-tag"));
	}

	@Test
	public void testRpcClientBasicsTraceKeys() {
		LabelExtractor extractor = new LabelExtractor(new TraceKeys());
		String instanceId = "localhost";
		long begin = 1238912378081L;
		long end = 1238912378123L;
		Span span = Span.newBuilder()
				.traceId("123")
				.id("9999")
				.timestamp(begin)
				.duration(end - begin)
				.putTag("http.host", "localhost")
				.putTag("custom-tag", "hello")
				.localEndpoint(Endpoint.newBuilder().serviceName("hello-service").build())
				.build();

		Map<String, String> labels = extractor.extract(span);

		Assert.assertNotNull("span shouldn't be null", span);
		Assert.assertEquals("localhost", labels.get("/http/host"));
		Assert.assertEquals("spring-cloud-gcp-trace", labels.get("/agent"));
		Assert.assertEquals("hello-service", labels.get("/component"));
		Assert.assertEquals("hello", labels.get("cloud.spring.io/custom-tag"));
	}
}
