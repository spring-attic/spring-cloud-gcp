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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.google.devtools.cloudtrace.v1.TraceSpan;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.cloud.sleuth.Log;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceKeys;

/**
 * @author Ray Tsang
 */
public class LabelExtractorTests {
	SimpleDateFormat dateFormatter = new SimpleDateFormat(LabelExtractor.DEFAULT_TIMESTAMP_FORMAT);

	@Test
	public void testLabelReplacement() {
		LabelExtractor extractor = new LabelExtractor(new TraceKeys());
		Span span = Span.builder()
				.tag("http.host", "localhost")
				.build();
		Map<String, String> labels = extractor.extract(span, TraceSpan.SpanKind.RPC_CLIENT, null);

		Assert.assertNotNull("labels shouldn't be null", labels);
		Assert.assertEquals("localhost", labels.get("/http/host"));
		Assert.assertFalse(labels.containsKey("http.host"));
	}

	@Test
	public void testRpcClientBasics() {
		LabelExtractor extractor = new LabelExtractor(new TraceKeys());
		String instanceId = "localhost";
		long begin = 1238912378081L;
		long end = 1238912378123L;
		Span span = Span.builder()
				.begin(begin)
				.end(end)
				.log(new Log(begin, Span.CLIENT_SEND))
				.log(new Log(end, Span.CLIENT_RECV))
				.tag("http.host", "localhost")
				.tag("custom-tag", "hello")
				.tag(Span.SPAN_PEER_SERVICE_TAG_NAME, "hello-service")
				.build();

		Map<String, String> labels = extractor.extract(span, TraceSpan.SpanKind.RPC_CLIENT, "hostname");

		Assert.assertNotNull("span shouldn't be null", span);
		Assert.assertEquals(this.dateFormatter.format(new Date(begin)), labels.get("cloud.spring.io/cs"));
		Assert.assertEquals(this.dateFormatter.format(new Date(end)), labels.get("cloud.spring.io/cr"));
		Assert.assertEquals("localhost", labels.get("/http/host"));
		Assert.assertEquals("spring-cloud-gcp-trace", labels.get("/agent"));
		Assert.assertEquals("hello-service", labels.get("/component"));
		Assert.assertEquals("hostname", labels.get("cloud.spring.io/spring.instance_id"));
		Assert.assertEquals("hello", labels.get("cloud.spring.io/custom-tag"));
	}
}
