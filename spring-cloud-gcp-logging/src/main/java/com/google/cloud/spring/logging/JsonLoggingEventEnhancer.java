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

package com.google.cloud.spring.logging;

import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * This interface allows users to write additional JSON record the JSON log layout.
 *
 * <p>Implementing classes can be used as {@code <loggingEventEnhancer>} elements in the logback definition
 * for {@link StackdriverJsonLayout}.
 */
public interface JsonLoggingEventEnhancer {

	/**
	 * Add additional JSON data to the JSON log entry, based on the {@link ILoggingEvent}.
	 * Users should put additional records to {@code jsonMap} to extend the JSON object that will be logged.
	 *
	 * @param jsonMap represents the JSON object that is produced by the Stackdriver JSON logging layout
	 * @param event the Logback logging event
	 */
	void enhanceJsonLogEntry(Map<String, Object> jsonMap, ILoggingEvent event);
}
