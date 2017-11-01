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

import java.util.List;

import com.google.cloud.trace.v1.consumer.TraceConsumer;
import com.google.devtools.cloudtrace.v1.Trace;
import com.google.devtools.cloudtrace.v1.TraceSpan;
import com.google.devtools.cloudtrace.v1.Traces;
import com.google.protobuf.Timestamp;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.sleuth.Log;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanAdjuster;
import org.springframework.cloud.sleuth.SpanReporter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Adapted from <a href=
 * "https://github.com/spring-cloud/spring-cloud-sleuth/tree/master/spring-cloud-sleuth-zipkin">Spring
 * Cloud Sleuth Zipkin's Span Listener</a>
 *
 * Listener of Sleuth events. Reports to Stackdriver Trace via {@link TraceConsumer}
 *
 * @author Ray Tsang
 */
public class StackdriverTraceSpanListener implements SpanReporter {
	private static final org.apache.commons.logging.Log LOGGER = LogFactory.getLog(StackdriverTraceSpanListener.class);

	private final String instanceId;

	private final String projectId;

	private final LabelExtractor labelExtractor;

	private final List<SpanAdjuster> spanAdjusters;

	private final TraceConsumer traceConsumer;

	public StackdriverTraceSpanListener(String instanceId, String projectId, LabelExtractor labelExtractor,
			List<SpanAdjuster> spanAdjusters, TraceConsumer traceConsumer) {
		this.instanceId = instanceId;
		this.projectId = projectId;
		this.labelExtractor = labelExtractor;
		this.spanAdjusters = spanAdjusters;
		this.traceConsumer = traceConsumer;
	}

	protected Timestamp createTimestamp(long milliseconds) {
		long seconds = (milliseconds / 1000);
		int remainderMicros = (int) (milliseconds % 1000);
		int remainderNanos = remainderMicros * 1000000;

		return Timestamp.newBuilder().setSeconds(seconds).setNanos(remainderNanos).build();
	}

	private TraceSpan.SpanKind getKind(Span span) {
		for (Log log : span.logs()) {
			if (Span.CLIENT_SEND.equals(log.getEvent()) || Span.CLIENT_RECV.equals(log.getEvent())) {
				return TraceSpan.SpanKind.RPC_CLIENT;
			}
			else if (Span.SERVER_RECV.equals(log.getEvent()) || Span.SERVER_SEND.equals(log.getEvent())) {
				return TraceSpan.SpanKind.RPC_SERVER;
			}
		}
		return TraceSpan.SpanKind.SPAN_KIND_UNSPECIFIED;
	}

	private Log findLog(Span span, String event) {
		for (Log log : span.logs()) {
			if (event.equals(log.getEvent())) {
				return log;
			}
		}
		return null;
	}

	protected TraceSpan convert(Span span) {
		Span adjustedSpan = span;
		for (SpanAdjuster adjuster : this.spanAdjusters) {
			adjustedSpan = adjuster.adjust(adjustedSpan);
		}

		TraceSpan.Builder builder = TraceSpan.newBuilder();

		// Set name
		if (StringUtils.hasText(adjustedSpan.getName())) {
			builder.setName(adjustedSpan.getName());
		}

		TraceSpan.SpanKind kind = getKind(adjustedSpan);
		builder.setKind(kind);
		builder.setSpanId(adjustedSpan.getSpanId());
		rewriteIds(adjustedSpan, kind, builder);
		writeStartEndTime(adjustedSpan, builder);

		builder.putAllLabels(this.labelExtractor.extract(adjustedSpan, builder.getKind(), this.instanceId));

		return builder.build();
	}

	private void writeStartEndTime(Span span, TraceSpan.Builder builder) {
		if (!span.isRemote()) {
			Log clientSend = findLog(span, Span.CLIENT_SEND);
			Log clientReceive = findLog(span, Span.CLIENT_RECV);
			if (clientSend != null) {
				builder.setStartTime(createTimestamp(clientSend.getTimestamp()));
			}
			else {
				builder.setStartTime(createTimestamp(span.getBegin()));
			}
			if (!span.isRunning()) {
				if (clientReceive != null) {
					builder.setEndTime(createTimestamp(clientReceive.getTimestamp()));
				}
				else {
					builder.setEndTime(createTimestamp(span.getEnd()));
				}
			}
		}
	}

	private void rewriteIds(Span span, TraceSpan.SpanKind kind, TraceSpan.Builder builder) {
		// Find the parent ID. Use 0 if no parent found.
		long parentId = 0;
		if (span.getParents().size() > 0) {
			if (span.getParents().size() > 1) {
				LOGGER.error("Stackdriver Trace doesn't support spans with multiple parents. Omitting "
						+ "other parents for " + span);
			}
			parentId = span.getParents().get(0);
		}

		// Every span will need a different Span ID.
		// If it's a RPC_CLIENT span, there is likely to be a RPC_SERVER span. Both spans would've
		// had the same Span ID and that won't work. Thus, we need to change one of the Span IDs.
		// If it's a RPC_CLIENT, we'll rewrite the Span ID. Later, we'll need to rewrite the
		// corresponding RPC_SERVER span's parent ID to match this one.
		if (kind == TraceSpan.SpanKind.RPC_CLIENT) {
			builder.setSpanId(rewriteId(span.getSpanId()));
		}
		else {
			builder.setSpanId(span.getSpanId());
		}

		// Change the parentSpanId of RPC_SERVER spans to use the rewritten spanId of the
		// RPC_CLIENT spans.
		if (kind == TraceSpan.SpanKind.RPC_SERVER) {
			if (!span.isRemote()) {
				// Owns the Span.
				// This means the parent RPC_CLIENT span was a separate span with id=span.parentId.
				// When that span was converted, it would have had id=rewriteId(span.parentId)
				builder.setParentSpanId(rewriteId(parentId));
			}
			else {
				// This is a multi-host span.
				// This means the parent client-side span has the same id as this span. When that fragment
				// of the span was converted, it would have had id rewriteId(span.spanId)
				builder.setParentSpanId(rewriteId(span.getSpanId()));
			}
		}
		else {
			builder.setParentSpanId(parentId);
		}
	}

	private long rewriteId(long id) {
		if (id == 0) {
			return 0;
		}
		// To deterministically rewrite the ID, xor it with a random 64-bit constant.
		final long pad = 0x3f6a2ec3c810c2abL;
		return id ^ pad;
	}

	private String formatTraceId(Span span) {
		return span.traceIdString();
	}

	@Override
	public void report(Span span) {
		Assert.notNull(span, "span cannot be null");
		if (span.isExportable()) {
			TraceSpan traceSpan = convert(span);
			Traces traces = Traces.newBuilder()
					.addTraces(Trace.newBuilder()
							.setTraceId(formatTraceId(span))
							.setProjectId(this.projectId)
							.addSpans(traceSpan)
							.build())
					.build();
			this.traceConsumer.receive(traces);
		}
		else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("The span " + span + " will not be sent to Stackdriver Trace due to sampling");
			}
		}
	}
}
