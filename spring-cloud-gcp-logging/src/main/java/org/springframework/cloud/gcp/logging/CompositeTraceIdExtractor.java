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

package org.springframework.cloud.gcp.logging;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.ImmutableList;

import org.springframework.util.Assert;

/**
 * Delegates checking HTTP request to a list of provided {@link TraceIdExtractor}, and provides
 * the first one that is found.
 *
 * @author Chengyuan Zhao
 */
public class CompositeTraceIdExtractor implements TraceIdExtractor {

	private final List<TraceIdExtractor> extractors;

	public CompositeTraceIdExtractor(TraceIdExtractor... extractors) {
		Assert.notEmpty(extractors, "A list of valid trace ID extractors is required.");
		this.extractors = ImmutableList.copyOf(extractors);
	}

	public List<TraceIdExtractor> getExtractors() {
		return this.extractors;
	}

	@Override
	public String extractTraceIdFromRequest(HttpServletRequest req) {
		String traceId;
		for (TraceIdExtractor extractor : getExtractors()) {
			traceId = extractor.extractTraceIdFromRequest(req);
			if (traceId != null) {
				return traceId;
			}
		}
		return null;
	}
}
