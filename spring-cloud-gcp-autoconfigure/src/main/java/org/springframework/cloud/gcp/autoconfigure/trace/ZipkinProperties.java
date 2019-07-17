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

package org.springframework.cloud.gcp.autoconfigure.trace;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Zipkin Properties.
 *
 * @author Tim Ysewyn
 * @deprecated Replaced by {@link GcpTraceProperties} since 1.2.
 * Added for backward & forward compatibility.
 */
@Deprecated
@ConfigurationProperties("spring.zipkin")
public class ZipkinProperties {

	/**
	 * Timeout in seconds before pending spans will be sent in batches to GCP Stackdriver Trace.
	 * Deprecated in favor of spring.cloud.gcp.trace.messageTimeout
	 */
	private int messageTimeout = 1;

	public int getMessageTimeout() {
		return this.messageTimeout;
	}

	public void setMessageTimeout(int messageTimeout) {
		this.messageTimeout = messageTimeout;
	}
}
