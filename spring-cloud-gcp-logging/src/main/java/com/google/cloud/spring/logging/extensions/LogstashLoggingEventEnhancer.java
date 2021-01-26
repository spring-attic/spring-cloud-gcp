/*
 * Copyright 2017-2019 the original author or authors.
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

package com.google.cloud.spring.logging.extensions;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.logback.LoggingEventEnhancer;
import com.google.cloud.spring.logging.JsonLoggingEventEnhancer;
import net.logstash.logback.marker.ObjectAppendingMarker;
import org.slf4j.Marker;

/**
 * Logging enhancer which adds Logstash markers to Logging API calls and JSON log entries.
 *
 * <p>
 * Supported Markers:
 * - ObjectAppendingMarker: key-value pairs are added to the log entries.
 *
 * <p>
 * This can be used by specifying this class in a {@code <loggingEventEnhancer>} element for your
 * {@link com.google.cloud.logging.logback.LoggingAppender} or
 * {@link com.google.cloud.spring.logging.StackdriverJsonLayout} definitions in logback.xml.
 */
public class LogstashLoggingEventEnhancer implements LoggingEventEnhancer, JsonLoggingEventEnhancer {

	@Override
	public void enhanceLogEntry(LogEntry.Builder builder, ILoggingEvent event) {
		addLogstashMarkerIfNecessary(
				event.getMarker(),
				marker -> builder.addLabel(marker.getFieldName(), marker.getFieldValue().toString()));
	}

	@Override
	public void enhanceJsonLogEntry(Map<String, Object> jsonMap, ILoggingEvent event) {
		addLogstashMarkerIfNecessary(
				event.getMarker(),
				marker -> jsonMap.put(marker.getFieldName(), marker.getFieldValue()));
	}

	private void addLogstashMarkerIfNecessary(
			Marker marker, Consumer<ObjectAppendingMarker> markerAdderFunction) {
		if (marker == null) {
			return;
		}

		if (marker instanceof ObjectAppendingMarker) {
			ObjectAppendingMarker objectAppendingMarker = (ObjectAppendingMarker) marker;
			markerAdderFunction.accept(objectAppendingMarker);
		}

		if (marker.hasReferences()) {
			for (Iterator<?> i = marker.iterator(); i.hasNext(); ) {
				Marker next = (Marker) i.next();
				addLogstashMarkerIfNecessary(next, markerAdderFunction);
			}
		}
	}
}
