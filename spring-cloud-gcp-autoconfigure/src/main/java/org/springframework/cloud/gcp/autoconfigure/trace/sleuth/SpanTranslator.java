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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.primitives.UnsignedLongs;
import com.google.devtools.cloudtrace.v1.TraceSpan;
import com.google.devtools.cloudtrace.v1.TraceSpan.SpanKind;
import com.google.protobuf.Timestamp;
import zipkin2.Span;

/**
 * SpanTranslator converts a Zipkin Span to a Stackdriver Trace Span.
 *
 * <p>It will rewrite span IDs so that multi-host Zipkin spans are converted to single-host
 * Stackdriver spans. Zipkin Spans with both server-side and client-side information will
 * be split into two Stackdriver Trace Spans where the client-side span is a parent of the
 * server-side span. Other parent-child relationships will be preserved.
 *
 * <p>Imported from from <a href="Stackdriver
 * Zipkin">https://github.com/GoogleCloudPlatform/stackdriver-zipkin/</a>
 */
public class SpanTranslator {

	private final LabelExtractor labelExtractor;

	private final Map<Span.Kind, SpanKind> SPAN_KIND_MAP;

	{
		Map<Span.Kind, SpanKind> map = new HashMap<>();
		map.put(Span.Kind.CLIENT, SpanKind.RPC_CLIENT);
		map.put(Span.Kind.SERVER, SpanKind.RPC_SERVER);
		this.SPAN_KIND_MAP = Collections.unmodifiableMap(map);
	}

	/** Create a SpanTranslator. */
	public SpanTranslator(LabelExtractor labelExtractor) {
		this.labelExtractor = labelExtractor;
	}

	/**
	 * Converts a Zipkin Span into a Stackdriver Trace Span.
	 *
	 * @param zipkinSpan The Zipkin Span.
	 * @return A Stackdriver Trace Span.
	 */
	public TraceSpan translate(Span zipkinSpan) {
		TraceSpan.Builder builder = TraceSpan.newBuilder();
		translate(builder, zipkinSpan);
		return builder.build();
	}

	TraceSpan.Builder translate(TraceSpan.Builder spanBuilder, Span zipkinSpan) {
		// It's possible to have an empty name when the Span is generated from ane error.
		spanBuilder.setName(zipkinSpan.name() == null ? "" : zipkinSpan.name());
		SpanKind kind = getSpanKind(zipkinSpan.kind());
		spanBuilder.setKind(kind);
		rewriteIds(zipkinSpan, spanBuilder, kind);
		if (zipkinSpan.timestampAsLong() != 0L) {
			spanBuilder.setStartTime(createTimestamp(zipkinSpan.timestamp()));
			if (zipkinSpan.durationAsLong() != 0) {
				Timestamp endTime = createTimestamp(zipkinSpan.timestampAsLong() + zipkinSpan.durationAsLong());
				spanBuilder.setEndTime(endTime);
			}
		}
		spanBuilder.putAllLabels(this.labelExtractor.extract(zipkinSpan));
		return spanBuilder;
	}

	/**
	 * Rewrite Span IDs to split multi-host Zipkin spans into multiple single-host Stackdriver
	 * spans.
	 */
	private void rewriteIds(Span zipkinSpan, TraceSpan.Builder builder, SpanKind kind) {
		long id = parseUnsignedLong(zipkinSpan.id());
		long parentId = parseUnsignedLong(zipkinSpan.parentId());
		if (kind == SpanKind.RPC_CLIENT) {
			builder.setSpanId(rewriteId(id));
		}
		else {
			builder.setSpanId(id);
		}

		// Change the parentSpanId of RPC_SERVER spans to use the rewritten spanId of the
		// RPC_CLIENT
		// spans.
		if (kind == SpanKind.RPC_SERVER) {
			if (Boolean.TRUE.equals(zipkinSpan.shared())) {
				builder.setParentSpanId(rewriteId(id));
			}
			else {
				builder.setParentSpanId(rewriteId(parentId));
			}
		}
		else {
			builder.setParentSpanId(parentId);
		}
	}

	private long parseUnsignedLong(String id) {
		if (id == null) {
			return 0;
		}
		return UnsignedLongs.parseUnsignedLong(id, 16);
	}

	private long rewriteId(long id) {
		if (id == 0) {
			return 0;
		}
		// To deterministically rewrite the ID, xor it with a random 64-bit constant.
		final long pad = 0x3f6a2ec3c810c2abL;
		return id ^ pad;
	}

	private SpanKind getSpanKind(Span.Kind zipkinKind) {
		if (zipkinKind == null) {
			return SpanKind.SPAN_KIND_UNSPECIFIED;
		}

		return this.SPAN_KIND_MAP.getOrDefault(zipkinKind, SpanKind.UNRECOGNIZED);
	}

	public Timestamp createTimestamp(long microseconds) {
		long seconds = (microseconds / 1000000);
		int remainderMicros = (int) (microseconds % 1000000);
		int remainderNanos = remainderMicros * 1000;

		return Timestamp.newBuilder().setSeconds(seconds).setNanos(remainderNanos).build();
	}
}
