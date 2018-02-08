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

/**
 * @author João André Martins
 */
@ConfigurationProperties("spring.cloud.gcp.pubsub")
public class GcpPubSubProperties {
	/** Number of threads used by every {@link com.google.cloud.pubsub.v1.Subscriber}. */
	private int subscriberExecutorThreads = 4;

	/** Number of threads used by every {@link com.google.cloud.pubsub.v1.Publisher}. */
	private int publisherExecutorThreads = 4;

	/** Overrides the GCP project ID specified in the Core module. */
	private String projectId;

	/**
	 * The host and port of the local running emulator. If provided, this will setup the
	 * client to connect against a running pub/sub emulator
	 */
	private String emulatorHost;

	/** Overrides the GCP OAuth2 credentials specified in the Core module. */
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

	public String getEmulatorHost() {
		return this.emulatorHost;
	}

	public void setEmulatorHost(String emulatorHost) {
		this.emulatorHost = emulatorHost;
	}
}
