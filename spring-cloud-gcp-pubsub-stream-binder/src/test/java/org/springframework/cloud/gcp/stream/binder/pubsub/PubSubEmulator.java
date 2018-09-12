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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.ExternalResource;

/**
 * Rule for instantiating and tearing down a Pub/Sub emulator instance.
 */
public class PubSubEmulator extends ExternalResource {
	private static final Path emulatorConfigPath = Paths.get(System.getProperty("user.home")).resolve(
			Paths.get(".config", "gcloud", "emulators", "pubsub", "env.yaml"));

	private static final Log LOGGER = LogFactory.getLog(PubSubEmulator.class);


	// Binder to Pub/Sub emulator for use in individual tests.
	private PubSubTestBinder binder;

	// Reference to emulator instance, for cleanup.
	private Process emulatorProcess;

	// Hostname for cleanup, should always be localhost.
	private String emulatorHost;

	// Port for cleanup.
	private String emulatorPort;

	/**
	 * Launches an instance of pubsub simulator, creates a binder.
	 *
	 * @throws Throwable for any failed setup.
	 */
	@Override
	protected void before() throws Throwable {

		this.emulatorProcess = new ProcessBuilder("gcloud", "beta", "emulators", "pubsub", "start").start();
		assertConfigPresent();

		Process envInitProcess = new ProcessBuilder("gcloud", "beta", "emulators", "pubsub", "env-init").start();

		// env-init output is a shell command of the form "export PUBSUB_EMULATOR_HOST=localhost:8085".
		String emulatorInitString
				= new BufferedReader(new InputStreamReader(envInitProcess.getInputStream())).readLine();
		envInitProcess.waitFor();
		String emulatorHostPort = emulatorInitString.substring(emulatorInitString.indexOf('=') + 1);
		extractTeardownParams(emulatorHostPort);

		this.binder = new PubSubTestBinder(emulatorHostPort);
	}

	/**
	 * Shuts down the two emulator processes.
	 *
	 * gcloud command is shut down through the direct process handle.
	 * java process is identified and shut down through shell commands.
	 *
	 * There should normally be only one process with that host/port combination, but if there are more, they will be
	 * cleaned up as well.
	 */
	@Override
	protected void after() {
		this.emulatorProcess.destroy();
		try {
			String hostPortParams = String.format("--host=%s --port=%s", this.emulatorHost, this.emulatorPort);
			Process psProcess = new ProcessBuilder("ps", "-v").start();
			new BufferedReader(new InputStreamReader(psProcess.getInputStream()))
					.lines()
					.filter(psLine -> psLine.contains(hostPortParams))
					.map(psLine -> psLine.substring(0, psLine.indexOf(' ')))
					.forEach(pid -> {
						this.killProcess(pid);
					});
		}
		catch (IOException e) {
			LOGGER.error("Failed to cleanup: ", e);
		}
	}

	/**
	 * Returns an already-configured emulator binder when called from within a JUnit method.
	 */
	public PubSubTestBinder getBinder() {
		return this.binder;
	}

	/**
	 * Waits until a PubSub emulator configuration file is present.
	 *
	 * Fails if the file does not appear after 1 second.
	 *
	 * @throws InterruptedException which should interrupt the peaceful slumber and bubble up to fail the test.
	 */
	private void assertConfigPresent() throws InterruptedException {
		int attempts = 10;
		while (!Files.exists(this.emulatorConfigPath) && --attempts >= 0) {
			Thread.sleep(100);
		}
		if (attempts < 0) {
			throw new RuntimeException(
					"Emulator could not be configured due to missing env.yaml. Are PubSub and beta tools installed?");
		}
	}

	/**
	 * Remembers host/port combination for future process cleanup.
	 *
	 * Validates that localhost is the only valid value; this will stop the test if emulator decides to start with an
	 * IPv6 ::1 address.
	 *
	 * @param emulatorHostPort typically "localhost:8085"
	 */
	private void extractTeardownParams(String emulatorHostPort) {
		int portSeparatorIndex = emulatorHostPort.indexOf(":");
		if (portSeparatorIndex < 0 || !emulatorHostPort.contains("localhost")) {
			throw new RuntimeException("Malformed host, can't create emulator: " + emulatorHostPort);
		}
		this.emulatorHost = emulatorHostPort.substring(0, portSeparatorIndex);
		this.emulatorPort = emulatorHostPort.substring(portSeparatorIndex + 1);
	}

	/**
	 * Attempts to kill a process on best effort basis.
	 *
	 * Failure is logged and ignored.
	 *
	 * @param pid Presumably a valid PID. No checking done to validate.
	 */
	private void killProcess(String pid) {
		try {
			new ProcessBuilder("kill", pid).start();
		}
		catch (IOException e) {
			LOGGER.error("Failed to clean up PID " + pid);
		}
	}
}
