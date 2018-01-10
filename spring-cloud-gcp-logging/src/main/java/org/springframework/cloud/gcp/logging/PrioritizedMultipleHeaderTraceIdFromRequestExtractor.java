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

import org.springframework.util.Assert;

/**
 * Checks HTTP request for multiple headers that might contain a trace ID, and provides
 * the first one.
 *
 * @author Chengyuan Zhao
 */
public class PrioritizedMultipleHeaderTraceIdFromRequestExtractor
		implements TraceIdFromRequestExtractor {

	private List<TraceIdFromRequestExtractor> subExtractors;

	public PrioritizedMultipleHeaderTraceIdFromRequestExtractor(
			List<TraceIdFromRequestExtractor> subExtractors) {
		Assert.notNull(subExtractors, "A valid list of extractors is required.");

		this.subExtractors = subExtractors;
	}

	public List<TraceIdFromRequestExtractor> getSubExtractors() {
		return this.subExtractors;
	}

	public void setSubExtractors(List<TraceIdFromRequestExtractor> subExtractors) {
		this.subExtractors = subExtractors;
	}

	@Override
	public String extractTraceIdFromRequest(HttpServletRequest req) {
		String traceId;
		for (TraceIdFromRequestExtractor extractor : getSubExtractors()) {
			traceId = extractor.extractTraceIdFromRequest(req);
			if (traceId != null) {
				return traceId;
			}
		}
		return null;
	}
}
