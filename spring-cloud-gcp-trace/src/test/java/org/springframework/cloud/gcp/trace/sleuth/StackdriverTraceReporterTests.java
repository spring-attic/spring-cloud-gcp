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
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import com.google.cloud.trace.v1.consumer.FlushableTraceConsumer;
import com.google.devtools.cloudtrace.v1.Trace;
import com.google.devtools.cloudtrace.v1.TraceSpan;
import com.google.devtools.cloudtrace.v1.Traces;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import zipkin2.Span;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = {
		StackdriverTraceReporterTests.TestConfiguration.class })
@RunWith(SpringRunner.class)
public class StackdriverTraceReporterTests {
	private static final long beginTime = 1238912378081L;

	private static final long endTime = 1238912378123L;

	@Autowired
	TestConfiguration test;

	@Autowired
	StackdriverTraceReporter reporter;

	@Autowired
	SpanTranslator spanTranslator;

	@Autowired
	MockEnvironment mockEnvironment;

	SimpleDateFormat dateFormatter = new SimpleDateFormat(LabelExtractor.DEFAULT_TIMESTAMP_FORMAT);

	@PostConstruct
	public void init() {
		this.test.traceSpans.clear();
	}

	@Test
	public void testSingleServerSpan() {
		Span parent = Span.newBuilder()
				.traceId("123")
				.id("9999")
				.name("http:parent")
				.timestamp(beginTime)
				.duration(endTime - beginTime)
				.kind(Span.Kind.SERVER)
				.build();

		this.reporter.report(parent);

		Assert.assertEquals(1, this.test.traceSpans.size());
		TraceSpan traceSpan = this.test.traceSpans.get(0);
		Assert.assertEquals("http:parent", traceSpan.getName());
		// Server Spans should use Span begin and end time, not SR or SS
		Assert.assertEquals(this.spanTranslator.createTimestamp(beginTime), traceSpan.getStartTime());
		Assert.assertEquals(this.spanTranslator.createTimestamp(endTime), traceSpan.getEndTime());
		Assert.assertEquals(TraceSpan.SpanKind.RPC_SERVER, traceSpan.getKind());
	}

	@Test
	public void testSingleClientSpan() {
		Span parent = Span.newBuilder()
				.traceId("123")
				.id("9999")
				.name("http:call")
				.timestamp(beginTime)
				.duration(endTime - beginTime)
				.kind(Span.Kind.CLIENT)
				.build();

		this.reporter.report(parent);

		Assert.assertEquals(1, this.test.traceSpans.size());
		TraceSpan traceSpan = this.test.traceSpans.get(0);
		Assert.assertEquals("http:call", traceSpan.getName());
		// Client span chould use CS and CR time, not Span begin or end time.
		Assert.assertEquals(this.spanTranslator.createTimestamp(beginTime), traceSpan.getStartTime());
		Assert.assertEquals(this.spanTranslator.createTimestamp(endTime), traceSpan.getEndTime());
		Assert.assertEquals(TraceSpan.SpanKind.RPC_CLIENT, traceSpan.getKind());
	}

	@Test
	public void testClientServerSpans() {
		Span clientSpan = Span.newBuilder()
				.traceId("123")
				.id("9999")
				.name("http:call")
				.timestamp(beginTime)
				.duration(endTime - beginTime)
				.kind(Span.Kind.CLIENT)
				.build();

		Span serverSpan = Span.newBuilder()
				.traceId("123")
				.parentId(clientSpan.id())
				.id(clientSpan.id())
				.name("http:service")
				.timestamp(beginTime)
				.duration(endTime - beginTime)
				.kind(Span.Kind.SERVER)
				.build();

		this.reporter.report(serverSpan);
		this.reporter.report(clientSpan);

		Assert.assertEquals(2, this.test.traceSpans.size());

		TraceSpan serverTraceSpan = this.test.traceSpans.get(0);
		Assert.assertEquals("http:service", serverTraceSpan.getName());
		Assert.assertEquals(TraceSpan.SpanKind.RPC_SERVER, serverTraceSpan.getKind());

		TraceSpan clientTraceSpan = this.test.traceSpans.get(1);
		Assert.assertEquals("http:call", clientTraceSpan.getName());
		// Client span chould use CS and CR time, not Span begin or end time.
		Assert.assertEquals(this.spanTranslator.createTimestamp(beginTime), clientTraceSpan.getStartTime());
		Assert.assertEquals(this.spanTranslator.createTimestamp(endTime), clientTraceSpan.getEndTime());
		Assert.assertEquals(TraceSpan.SpanKind.RPC_CLIENT, clientTraceSpan.getKind());

		Assert.assertEquals(0, clientTraceSpan.getParentSpanId());
		Assert.assertNotEquals(Long.valueOf(serverSpan.parentId()).longValue(), serverTraceSpan.getParentSpanId());

		// Even though the client and server spans shared the same Span ID. In Stackdriver Trace,
		// the IDs should be different. This is accomplished through
		// StackdriverTraceSpanListener.rewriteIds().
		Assert.assertNotEquals(clientTraceSpan.getSpanId(), serverTraceSpan.getSpanId());

		// Although the Span IDs were re-written, the server Span's parent ID should still refer
		// to client Span ID.
		Assert.assertEquals(serverTraceSpan.getParentSpanId(), clientTraceSpan.getSpanId());
	}

	@Configuration
	@EnableAutoConfiguration
	static class TestConfiguration {
		private List<TraceSpan> traceSpans = new ArrayList<>();

		@Bean
		@Primary
		MockEnvironment mockEnvironment() {
			return new MockEnvironment();
		}

		@Bean
		public FlushableTraceConsumer traceConsumer() {
			return new FlushableTraceConsumer() {
				@Override
				public void flush() {
					// do nothing
				}

				@Override
				public void receive(Traces traces) {
					for (Trace trace : traces.getTracesList()) {
						TestConfiguration.this.traceSpans.addAll(trace.getSpansList());
					}
				}
			};
		}

		@Bean
		public SpanTranslator spanTranslator() {
			return new SpanTranslator(new LabelExtractor(new TraceKeys()));
		}

		@Bean
		StackdriverTraceReporter reporter(FlushableTraceConsumer traceConsumer, SpanTranslator spanTranslator) {
			return new StackdriverTraceReporter("dummy-project", traceConsumer, spanTranslator);

		}
	}
}
