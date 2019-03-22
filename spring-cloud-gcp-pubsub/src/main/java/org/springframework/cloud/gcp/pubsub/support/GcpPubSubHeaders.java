/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.pubsub.support;

/**
 * Google Cloud Platform internal headers for Spring Messaging messages.
 *
 * @author João André Martins
 * @author Elena Felder
 * @author Chengyuan Zhao
 */
public abstract class GcpPubSubHeaders {

	private static final String PREFIX = "gcp_pubsub_";

	/**
	 * The ack header text.
	 *
	 * @deprecated as of 1.1, use {@link #ORIGINAL_MESSAGE} instead.
	 */
	@Deprecated
	public static final String ACKNOWLEDGEMENT = PREFIX + "acknowledgement";

	/**
	 * The topic header text.
	 */
	public static final String TOPIC = PREFIX + "topic";

	/**
	 * The original message header text.
	 */
	public static final String ORIGINAL_MESSAGE = PREFIX + "original_message";
}
