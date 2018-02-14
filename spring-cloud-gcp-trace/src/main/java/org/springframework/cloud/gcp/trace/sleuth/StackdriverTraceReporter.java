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

import com.google.cloud.trace.v1.consumer.TraceConsumer;
import com.google.devtools.cloudtrace.v1.Trace;
import com.google.devtools.cloudtrace.v1.TraceSpan;
import com.google.devtools.cloudtrace.v1.Traces;
import zipkin2.Span;
import zipkin2.reporter.Reporter;

public class StackdriverTraceReporter implements Reporter<Span> {
	private final TraceConsumer traceConsumer;

	private final SpanTranslator spanTranslator;

	public StackdriverTraceReporter(TraceConsumer traceConsumer,
			SpanTranslator spanTranslator) {
		this.traceConsumer = traceConsumer;
		this.spanTranslator = spanTranslator;
	}

	@Override
	public void report(Span span) {
		TraceSpan traceSpan = this.spanTranslator.translate(span);
		this.traceConsumer.receive(Traces.newBuilder()
				.addTraces(Trace.newBuilder().addSpans(traceSpan).build()).build());
	}
}
