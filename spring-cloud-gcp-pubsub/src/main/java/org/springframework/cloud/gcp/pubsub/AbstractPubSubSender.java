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

package org.springframework.cloud.gcp.pubsub;

import com.google.auth.Credentials;

import org.springframework.cloud.gcp.pubsub.converters.PubSubHeaderMapper;
import org.springframework.context.Lifecycle;

/**
 * @author Vinicius Carvalho
 * @author Mark Fisher
 */
public abstract class AbstractPubSubSender implements ReactivePubSubSender, Lifecycle {

	private final String project;

	private final Credentials credentials;

	private final String baseTopicName;

	private PubSubHeaderMapper headerMapper;

	private volatile boolean running;

	public AbstractPubSubSender(String project, Credentials credentials) {
		this.project = project;
		this.credentials = credentials;
		this.baseTopicName = String.format("projects/%s/topics/", this.project);
	}

	public PubSubHeaderMapper getHeaderMapper() {
		return this.headerMapper;
	}

	public void setHeaderMapper(PubSubHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	@Override
	public final void start() {
		this.running = true;
		doStart();
	}

	@Override
	public final void stop() {
		doStop();
		this.running = false;
	}

	@Override
	public final boolean isRunning() {
		return this.running;
	}

	protected abstract void doStart();

	protected abstract void doStop();
}
