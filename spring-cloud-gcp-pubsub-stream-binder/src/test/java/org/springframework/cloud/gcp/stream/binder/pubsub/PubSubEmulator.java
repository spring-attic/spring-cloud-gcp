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

package org.springframework.cloud.gcp.stream.binder.pubsub;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.ExternalResource;

/**
 * JUnit rule that starts a Pub/Sub emulator.
 *
 * @author João André Martins
 */
public class PubSubEmulator extends ExternalResource {

	private static final Log LOGGER = LogFactory.getLog(PubSubEmulator.class);

	private Path cloudSdkPath;

	private Process emulator;

	private int port;

	public PubSubEmulator() {
		this.cloudSdkPath = Paths.get(System.getProperty("user.home"))
				.resolve("google-cloud-sdk")
				.resolve("bin")
				.resolve("gcloud");
		// A port number between 10000 and 20000.
		this.port = new Double((Math.random() + 1) * 10000).intValue();
	}

	@Override
	protected void before() throws Throwable {
		ProcessBuilder processBuilder = new ProcessBuilder(
				this.cloudSdkPath.toString(),
				"beta",
				"emulators",
				"pubsub",
				"start",
				"--host-port=localhost:" + this.port);
		new Thread(() -> {
			try {
				LOGGER.info("Starting the emulator with command: " + processBuilder.command());
				this.emulator = processBuilder.start();
			}
			catch (IOException ioe) {
				LOGGER.info("Could not start Pub/Sub emulator.", ioe);
			}
		}).start();
	}

	@Override
	protected void after() {
		this.emulator.destroy();
	}

	public int getPort() {
		return this.port;
	}
}
