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

import java.io.Flushable;
import java.io.IOException;

import com.google.cloud.trace.v1.consumer.FlushableTraceConsumer;
import com.google.devtools.cloudtrace.v1.Trace;
import com.google.devtools.cloudtrace.v1.TraceSpan;
import com.google.devtools.cloudtrace.v1.Traces;
import zipkin2.Span;
import zipkin2.reporter.Reporter;

public class StackdriverTraceReporter implements Reporter<Span>, Flushable {
	private final String projectId;

	private final FlushableTraceConsumer traceConsumer;

	private final SpanTranslator spanTranslator;

	public StackdriverTraceReporter(String projectId, FlushableTraceConsumer traceConsumer,
			SpanTranslator spanTranslator) {
		this.projectId = projectId;
		this.traceConsumer = traceConsumer;
		this.spanTranslator = spanTranslator;
	}

	private String padTraceId(String traceId) {
		if (traceId.length() == 16) {
			return "0000000000000000" + traceId;
		}
		else {
			return traceId;
		}
	}

	@Override
	public void report(Span span) {
		TraceSpan traceSpan = this.spanTranslator.translate(span);
		Traces traces = Traces.newBuilder()
				.addTraces(Trace.newBuilder()
						.setTraceId(padTraceId(span.traceId()))
						.setProjectId(this.projectId)
						.addSpans(traceSpan).build())
				.build();
		this.traceConsumer.receive(traces);
	}

	public void flush() throws IOException {
		this.traceConsumer.flush();
	}
}
