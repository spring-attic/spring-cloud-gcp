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
package org.springframework.cloud.gcp.trace;

import java.io.Closeable;
import java.io.IOException;

import com.google.cloud.trace.v1.TraceServiceClient;
import com.google.cloud.trace.v1.consumer.TraceConsumer;
import com.google.devtools.cloudtrace.v1.Traces;

/**
 * This {@link TraceConsumer} immediately sends trace data to Stackdriver Trace.
 *
 * @author Ray Tsang
 */
public class TraceServiceClientTraceConsumer implements TraceConsumer, Closeable {
	private final String projectId;

	private final TraceServiceClient client;

	public TraceServiceClientTraceConsumer(String projectId, TraceServiceClient client) {
		this.projectId = projectId;
		this.client = client;
	}

	@Override
	public void receive(Traces traces) {
		this.client.patchTraces(this.projectId, traces);
	}

	@Override
	public void close() throws IOException {
		try {
			this.client.close();
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
