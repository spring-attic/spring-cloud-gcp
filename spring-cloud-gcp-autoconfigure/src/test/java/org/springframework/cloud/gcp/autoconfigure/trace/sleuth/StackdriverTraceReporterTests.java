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
import java.util.ArrayList;
import java.util.List;

import com.google.cloud.trace.v1.consumer.FlushableTraceConsumer;
import com.google.devtools.cloudtrace.v1.Trace;
import com.google.devtools.cloudtrace.v1.TraceSpan;
import com.google.devtools.cloudtrace.v1.Traces;
import org.junit.Test;
import zipkin2.Span;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

public class StackdriverTraceReporterTests {
	private static final long beginTime = 1238912378081L;

	private static final long endTime = 1238912378123L;

	SimpleDateFormat dateFormatter = new SimpleDateFormat(LabelExtractor.DEFAULT_TIMESTAMP_FORMAT);

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class);

	@Test
	public void testSingleServerSpan() {
		this.contextRunner.run(context -> {
			Span parent = Span.newBuilder()
					.traceId("123")
					.id("9999")
					.name("http:parent")
					.timestamp(beginTime)
					.duration(endTime - beginTime)
					.kind(Span.Kind.SERVER)
					.build();

			StackdriverTraceReporter reporter = context.getBean(StackdriverTraceReporter.class);
			reporter.report(parent);

			TestConfiguration configuration = context.getBean(TestConfiguration.class);
			assertThat(configuration.traceSpans.size()).isEqualTo(1);
			TraceSpan traceSpan = configuration.traceSpans.get(0);
			assertThat(traceSpan.getName()).isEqualTo("http:parent");
			SpanTranslator spanTranslator = context.getBean(SpanTranslator.class);
			// Server Spans should use Span begin and end time, not SR or SS
			assertThat(traceSpan.getStartTime())
					.isEqualTo(spanTranslator.createTimestamp(beginTime));
			assertThat(traceSpan.getEndTime())
					.isEqualTo(spanTranslator.createTimestamp(endTime));
			assertThat(traceSpan.getKind()).isEqualByComparingTo(TraceSpan.SpanKind.RPC_SERVER);
		});
	}

	@Test
	public void testSingleClientSpan() {
		this.contextRunner.run(context -> {
			Span parent = Span.newBuilder()
					.traceId("123")
					.id("9999")
					.name("http:call")
					.timestamp(beginTime)
					.duration(endTime - beginTime)
					.kind(Span.Kind.CLIENT)
					.build();

			StackdriverTraceReporter reporter = context.getBean(StackdriverTraceReporter.class);
			reporter.report(parent);

			TestConfiguration configuration = context.getBean(TestConfiguration.class);
			assertThat(configuration.traceSpans.size()).isEqualTo(1);
			TraceSpan traceSpan = configuration.traceSpans.get(0);
			assertThat(traceSpan.getName()).isEqualTo("http:call");
			SpanTranslator spanTranslator = context.getBean(SpanTranslator.class);
			// Client span chould use CS and CR time, not Span begin or end time.
			assertThat(traceSpan.getStartTime())
					.isEqualTo(spanTranslator.createTimestamp(beginTime));
			assertThat(traceSpan.getEndTime()).isEqualTo(spanTranslator.createTimestamp(endTime));
			assertThat(traceSpan.getKind()).isEqualTo(TraceSpan.SpanKind.RPC_CLIENT);
		});
	}

	@Test
	public void testClientServerSpans() {
		this.contextRunner.run(context -> {
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

			StackdriverTraceReporter reporter = context.getBean(StackdriverTraceReporter.class);
			reporter.report(serverSpan);
			reporter.report(clientSpan);

			TestConfiguration configuration = context.getBean(TestConfiguration.class);
			assertThat(configuration.traceSpans.size()).isEqualTo(2);

			TraceSpan serverTraceSpan = configuration.traceSpans.get(0);
			assertThat(serverTraceSpan.getName()).isEqualTo("http:service");
			assertThat(serverTraceSpan.getKind()).isEqualTo(TraceSpan.SpanKind.RPC_SERVER);

			TraceSpan clientTraceSpan = configuration.traceSpans.get(1);
			assertThat(clientTraceSpan.getName()).isEqualTo("http:call");
			SpanTranslator spanTranslator = context.getBean(SpanTranslator.class);
			// Client span chould use CS and CR time, not Span begin or end time.
			assertThat(clientTraceSpan.getStartTime())
					.isEqualTo(spanTranslator.createTimestamp(beginTime));
			assertThat(clientTraceSpan.getEndTime())
					.isEqualTo(spanTranslator.createTimestamp(endTime));
			assertThat(clientTraceSpan.getKind())
					.isEqualTo(TraceSpan.SpanKind.RPC_CLIENT);

			assertThat(clientTraceSpan.getParentSpanId()).isEqualTo(0);
			assertThat(serverTraceSpan.getParentSpanId())
					.isNotEqualTo(Long.valueOf(serverSpan.parentId()).longValue());

			// Even though the client and server spans shared the same Span ID. In Stackdriver Trace,
			// the IDs should be different. This is accomplished through
			// StackdriverTraceSpanListener.rewriteIds().
			assertThat(serverTraceSpan.getSpanId()).isNotEqualTo(clientTraceSpan.getSpanId());

			// Although the Span IDs were re-written, the server Span's parent ID should still refer
			// to client Span ID.
			assertThat(clientTraceSpan.getSpanId()).isEqualTo(serverTraceSpan.getParentSpanId());
		});
	}

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
