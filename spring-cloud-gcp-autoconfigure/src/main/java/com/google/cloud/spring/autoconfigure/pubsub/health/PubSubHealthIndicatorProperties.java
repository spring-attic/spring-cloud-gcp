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

package com.google.cloud.spring.autoconfigure.pubsub.health;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for Pub/Sub Health Indicator.
 *
 * @author Patrik Hörlin
 */
@ConfigurationProperties("spring.cloud.gcp.pubsub.health")
public class PubSubHealthIndicatorProperties {

	/**
	 * Subscription to health check against by pulling a message.
	 */
	private String subscription;

	/**
	 * Milliseconds to wait for response from Pub/Sub before timing out.
	 */
	private Long timeoutMillis = 2000L;

	/**
	 * Whether to acknowledge messages pulled from {@link #subscription}.
	 */
	private boolean acknowledgeMessages = false;

	public String getSubscription() {
		return subscription;
	}

	public void setSubscription(String subscription) {
		this.subscription = subscription;
	}

	public Long getTimeoutMillis() {
		return timeoutMillis;
	}

	public void setTimeoutMillis(Long timeoutMillis) {
		this.timeoutMillis = timeoutMillis;
	}

	public boolean isAcknowledgeMessages() {
		return acknowledgeMessages;
	}

	public void setAcknowledgeMessages(boolean acknowledgeMessages) {
		this.acknowledgeMessages = acknowledgeMessages;
	}
}
