/*
 *  Copyright 2017-2018 original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.pubsub;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.gcp.core.Credentials;
import org.springframework.cloud.gcp.core.GcpScope;

@ConfigurationProperties("spring.cloud.gcp.pubsub")
public class GcpPubSubProperties {
	private int subscriberExecutorThreads = 4;

	private int publisherExecutorThreads = 4;

	private String projectId;

	@NestedConfigurationProperty
	private final Credentials credentials = new Credentials(GcpScope.PUBSUB.getUrl());

	public int getSubscriberExecutorThreads() {
		return this.subscriberExecutorThreads;
	}

	public void setSubscriberExecutorThreads(int subscriberExecutorThreads) {
		this.subscriberExecutorThreads = subscriberExecutorThreads;
	}

	public int getPublisherExecutorThreads() {
		return this.publisherExecutorThreads;
	}

	public void setPublisherExecutorThreads(int publisherExecutorThreads) {
		this.publisherExecutorThreads = publisherExecutorThreads;
	}

	public String getProjectId() {
		return this.projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public Credentials getCredentials() {
		return this.credentials;
	}

}
