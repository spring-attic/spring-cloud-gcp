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

package org.springframework.cloud.gcp.logging;

import com.google.auth.Credentials;
import com.google.cloud.logging.LoggingOptions;

import org.springframework.cloud.gcp.core.UserAgentHeaderProvider;

/**
 * A Google Cloud Java Logback {@link com.google.cloud.logging.logback.LoggingAppender}
 * wrapper that configures it for Spring Cloud GCP.
 *
 * @author Mike Eltsufin
 * @since 1.2
 */
public class LoggingAppender extends com.google.cloud.logging.logback.LoggingAppender {
	private LoggingOptions loggingOptions;

	/**
	 * Wraps {@link com.google.cloud.logging.logback.LoggingAppender#getLoggingOptions()} to
	 * add {@link UserAgentHeaderProvider} configuration, so that usage can be properly
	 * attributed to Spring Cloud GCP.
	 */
	@Override
	protected LoggingOptions getLoggingOptions() {

		if (loggingOptions == null) {
			LoggingOptions.Builder loggingOptionsBuilder = LoggingOptions.newBuilder();

			// only credentials are set in the options of the parent class
			Credentials credentials = super.getLoggingOptions().getCredentials();
			if (credentials != null) {
				loggingOptionsBuilder.setCredentials(credentials);
			}

			// set User-Agent
			loggingOptionsBuilder.setHeaderProvider(new UserAgentHeaderProvider(this.getClass()));

			this.loggingOptions = loggingOptionsBuilder.build();
		}

		return this.loggingOptions;
	}
}
