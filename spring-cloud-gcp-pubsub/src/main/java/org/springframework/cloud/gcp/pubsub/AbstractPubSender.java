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
 *
 */

package org.springframework.cloud.gcp.pubsub;

import com.google.auth.Credentials;

import org.springframework.cloud.gcp.pubsub.converters.PubSubHeaderMapper;
import org.springframework.context.Lifecycle;

/**
 * @author Vinicius Carvalho
 */
public abstract class AbstractPubSender implements ReactivePubSubSender, Lifecycle{

	protected final String project;

	protected final Credentials credentials;

	protected volatile boolean running = false;

	protected final String BASE_NAME;

	protected final String BASE_TOPIC_NAME;

	protected PubSubHeaderMapper headerMapper;

	private final String PUBSUB_ENDPOINT = "pubsub.googleapis.com";

	private final int PUBUSUB_PORT = 443;


	public AbstractPubSender(String project, Credentials credentials) {
		this.project = project;
		this.credentials = credentials;
		this.BASE_NAME = String.format("projects/%s/",this.project);
		this.BASE_TOPIC_NAME = BASE_NAME + "topics/";
	}

	@Override
	public void start() {
		this.running = true;
		doStart();
	}

	@Override
	public void stop() {

		doStop();
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	abstract void doStart();

	abstract void doStop();

	public PubSubHeaderMapper getHeaderMapper() {
		return headerMapper;
	}

	public void setHeaderMapper(PubSubHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}
}
