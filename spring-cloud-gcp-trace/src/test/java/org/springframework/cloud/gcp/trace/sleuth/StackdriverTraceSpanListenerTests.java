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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import com.google.cloud.trace.v1.consumer.TraceConsumer;
import com.google.devtools.cloudtrace.v1.Trace;
import com.google.devtools.cloudtrace.v1.TraceSpan;
import com.google.devtools.cloudtrace.v1.Traces;
import com.google.protobuf.Timestamp;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.sleuth.Log;
import org.springframework.cloud.sleuth.Sampler;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = {
		StackdriverTraceSpanListenerTests.TestConfiguration.class })
@RunWith(SpringRunner.class)
public class StackdriverTraceSpanListenerTests {
	private static final long beginTime = 1238912378081L;

	private static final long endTime = 1238912378123L;

	@Autowired
	TestConfiguration test;

	@Autowired
	StackdriverTraceSpanListener spanListener;

	@Autowired
	MockEnvironment mockEnvironment;

	SimpleDateFormat dateFormatter = new SimpleDateFormat(LabelExtractor.DEFAULT_TIMESTAMP_FORMAT);

	@PostConstruct
	public void init() {
		this.test.traceSpans.clear();
	}

	@Test
	public void testSingleServerSpan() {
		Span parent = Span.builder()
				.traceId(123L)
				.name("http:parent")
				.begin(beginTime - 1)
				.end(endTime + 1)
				.log(new Log(beginTime, Span.SERVER_RECV))
				.log(new Log(endTime, Span.SERVER_SEND))
				.remote(false).build();

		this.spanListener.report(parent);

		Assert.assertEquals(1, this.test.traceSpans.size());
		TraceSpan traceSpan = this.test.traceSpans.get(0);
		Assert.assertEquals("http:parent", traceSpan.getName());
		// Server Spans should use Span begin and end time, not SR or SS
		Assert.assertEquals(this.spanListener.createTimestamp(beginTime - 1), traceSpan.getStartTime());
		Assert.assertEquals(this.spanListener.createTimestamp(endTime + 1), traceSpan.getEndTime());
		Assert.assertEquals(this.dateFormatter.format(new Date(beginTime)),
				traceSpan.getLabelsOrThrow("cloud.spring.io/sr"));
		Assert.assertEquals(this.dateFormatter.format(new Date(endTime)),
				traceSpan.getLabelsOrThrow("cloud.spring.io/ss"));
		Assert.assertEquals(TraceSpan.SpanKind.RPC_SERVER, traceSpan.getKind());
	}

	@Test
	public void testSingleClientSpan() {
		Span parent = Span.builder()
				.traceId(123L)
				.name("http:call")
				.begin(beginTime - 1)
				.end(endTime + 1)
				.log(new Log(beginTime, Span.CLIENT_SEND))
				.log(new Log(endTime, Span.CLIENT_RECV))
				.remote(false).build();

		this.spanListener.report(parent);

		Assert.assertEquals(1, this.test.traceSpans.size());
		TraceSpan traceSpan = this.test.traceSpans.get(0);
		Assert.assertEquals("http:call", traceSpan.getName());
		// Client span chould use CS and CR time, not Span begin or end time.
		Assert.assertEquals(this.spanListener.createTimestamp(beginTime), traceSpan.getStartTime());
		Assert.assertEquals(this.spanListener.createTimestamp(endTime), traceSpan.getEndTime());
		Assert.assertEquals(this.dateFormatter.format(new Date(beginTime)),
				traceSpan.getLabelsOrThrow("cloud.spring.io/cs"));
		Assert.assertEquals(this.dateFormatter.format(new Date(endTime)),
				traceSpan.getLabelsOrThrow("cloud.spring.io/cr"));
		Assert.assertEquals(TraceSpan.SpanKind.RPC_CLIENT, traceSpan.getKind());
	}

	@Test
	public void testSingleServerRemoteSpan() {
		Span parent = Span.builder()
				.traceId(123L)
				.name("http:parent")
				.begin(beginTime - 1)
				.end(endTime + 1)
				.log(new Log(beginTime, Span.SERVER_RECV))
				.log(new Log(endTime, Span.SERVER_SEND))
				.remote(true)
				.build();

		this.spanListener.report(parent);

		Assert.assertEquals(1, this.test.traceSpans.size());
		TraceSpan traceSpan = this.test.traceSpans.get(0);
		Assert.assertEquals(0, traceSpan.getParentSpanId());
		Assert.assertEquals("http:parent", traceSpan.getName());
		// Server side remote span should not report start/end time
		Assert.assertEquals(Timestamp.getDefaultInstance(), traceSpan.getStartTime());
		Assert.assertEquals(Timestamp.getDefaultInstance(), traceSpan.getEndTime());
		Assert.assertEquals(this.dateFormatter.format(new Date(beginTime)),
				traceSpan.getLabelsOrThrow("cloud.spring.io/sr"));
		Assert.assertEquals(this.dateFormatter.format(new Date(endTime)),
				traceSpan.getLabelsOrThrow("cloud.spring.io/ss"));
		Assert.assertEquals(TraceSpan.SpanKind.RPC_SERVER, traceSpan.getKind());
	}

	@Test
	public void testSingleClientRemoteSpan() {
		Span parent = Span.builder()
				.traceId(123L)
				.name("http:call")
				.begin(beginTime - 1)
				.end(endTime + 1)
				.log(new Log(beginTime, Span.CLIENT_SEND))
				.log(new Log(endTime, Span.CLIENT_RECV))
				.remote(true)
				.build();

		this.spanListener.report(parent);

		Assert.assertEquals(1, this.test.traceSpans.size());
		TraceSpan traceSpan = this.test.traceSpans.get(0);
		Assert.assertEquals("http:call", traceSpan.getName());
		// Client span chould use CS and CR time, not Span begin or end time.
		Assert.assertEquals(Timestamp.getDefaultInstance(), traceSpan.getStartTime());
		Assert.assertEquals(Timestamp.getDefaultInstance(), traceSpan.getEndTime());
		Assert.assertEquals(this.dateFormatter.format(new Date(beginTime)),
				traceSpan.getLabelsOrThrow("cloud.spring.io/cs"));
		Assert.assertEquals(this.dateFormatter.format(new Date(endTime)),
				traceSpan.getLabelsOrThrow("cloud.spring.io/cr"));
		Assert.assertEquals(TraceSpan.SpanKind.RPC_CLIENT, traceSpan.getKind());
	}

	@Test
	public void testClientServerSpans() {
		Span clientSpan = Span.builder()
				.traceId(123L)
				.spanId(9999L)
				.name("http:call")
				.begin(beginTime - 1)
				.end(endTime + 1)
				.log(new Log(beginTime, Span.CLIENT_SEND))
				.log(new Log(endTime, Span.CLIENT_RECV))
				.remote(false)
				.build();

		Span serverSpan = Span.builder()
				.traceId(123L)
				.parent(clientSpan.getSpanId())
				.spanId(clientSpan.getSpanId())
				.name("http:service")
				.begin(beginTime + 1)
				.end(endTime - 1)
				.log(new Log(beginTime, Span.SERVER_RECV))
				.log(new Log(endTime, Span.SERVER_SEND))
				.remote(true)
				.build();

		this.spanListener.report(serverSpan);
		this.spanListener.report(clientSpan);

		Assert.assertEquals(2, this.test.traceSpans.size());

		TraceSpan serverTraceSpan = this.test.traceSpans.get(0);
		Assert.assertEquals("http:service", serverTraceSpan.getName());
		// Server side remote span should not report start/end time
		Assert.assertEquals(Timestamp.getDefaultInstance(), serverTraceSpan.getStartTime());
		Assert.assertEquals(Timestamp.getDefaultInstance(), serverTraceSpan.getEndTime());
		Assert.assertEquals(this.dateFormatter.format(new Date(beginTime)),
				serverTraceSpan.getLabelsOrThrow("cloud.spring.io/sr"));
		Assert.assertEquals(this.dateFormatter.format(new Date(endTime)),
				serverTraceSpan.getLabelsOrThrow("cloud.spring.io/ss"));
		Assert.assertEquals(TraceSpan.SpanKind.RPC_SERVER, serverTraceSpan.getKind());

		TraceSpan clientTraceSpan = this.test.traceSpans.get(1);
		Assert.assertEquals("http:call", clientTraceSpan.getName());
		// Client span chould use CS and CR time, not Span begin or end time.
		Assert.assertEquals(this.spanListener.createTimestamp(beginTime), clientTraceSpan.getStartTime());
		Assert.assertEquals(this.spanListener.createTimestamp(endTime), clientTraceSpan.getEndTime());
		Assert.assertEquals(this.dateFormatter.format(new Date(beginTime)),
				clientTraceSpan.getLabelsOrThrow("cloud.spring.io/cs"));
		Assert.assertEquals(this.dateFormatter.format(new Date(endTime)),
				clientTraceSpan.getLabelsOrThrow("cloud.spring.io/cr"));
		Assert.assertEquals(TraceSpan.SpanKind.RPC_CLIENT, clientTraceSpan.getKind());

		Assert.assertEquals(0, clientTraceSpan.getParentSpanId());
		Assert.assertNotEquals(serverSpan.getParents().get(0).longValue(), serverTraceSpan.getParentSpanId());

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
		public Sampler sampler() {
			return new AlwaysSampler();
		}

		@Bean
		@Primary
		MockEnvironment mockEnvironment() {
			return new MockEnvironment();
		}

		@Bean
		public TraceConsumer traceConsumer() {
			return new TraceConsumer() {
				@Override
				public void receive(Traces traces) {
					for (Trace trace : traces.getTracesList()) {
						TestConfiguration.this.traceSpans.addAll(trace.getSpansList());
					}
				}
			};
		}

		@Bean
		StackdriverTraceSpanListener spanListener(TraceConsumer traceConsumer) {
			return new StackdriverTraceSpanListener("instance-id", "project-id", new LabelExtractor(new TraceKeys()),
					Collections.emptyList(), traceConsumer);
		}
	}
}
