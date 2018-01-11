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

/**
 * Holds settings for @{@link LoggingWebMvcConfigurer}
 * @author Chengyuan Zhao
 */
public class LoggingWebMvcConfigurerSettings {
	private boolean xCloudTrace = true;
	private boolean zipkinTrace;
	private boolean prioritizeXCloudTrace = true;

	public boolean isxCloudTrace() {
		return this.xCloudTrace;
	}

	public void setxCloudTrace(boolean xCloudTrace) {
		this.xCloudTrace = xCloudTrace;
	}

	public boolean isZipkinTrace() {
		return this.zipkinTrace;
	}

	public void setZipkinTrace(boolean zipkinTrace) {
		this.zipkinTrace = zipkinTrace;
	}

	public boolean isPrioritizeXCloudTrace() {
		return this.prioritizeXCloudTrace;
	}

	public void setPrioritizeXCloudTrace(boolean prioritizeXCloudTrace) {
		this.prioritizeXCloudTrace = prioritizeXCloudTrace;
	}
}
