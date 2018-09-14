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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.ExternalResource;

/**
 * Rule for instantiating and tearing down a Pub/Sub emulator instance.
 *
 * Tests can access the emulator's host/port combination by calling {@link #getEmulatorHostPort()} method.
 *
 * @author Elena Felder
 *
 * @since 1.1
 */
public class PubSubEmulator extends ExternalResource {
	private static final Path EMULATOR_CONFIG_DIR = Paths.get(System.getProperty("user.home")).resolve(
			Paths.get(".config", "gcloud", "emulators", "pubsub"));

	private static final String ENV_FILE_NAME = "env.yaml";

	private static final Path EMULATOR_CONFIG_PATH = EMULATOR_CONFIG_DIR.resolve(ENV_FILE_NAME);

	private static final Log LOGGER = LogFactory.getLog(PubSubEmulator.class);

	// Reference to emulator instance, for cleanup.
	private Process emulatorProcess;

	// Hostname for cleanup, should always be localhost.
	private String emulatorHostPort;


	/**
	 * Launch an instance of pubsub emulator.
	 *
	 * @throws Throwable for any unexpected setup failure.
	 */
	@Override
	protected void before() throws Throwable {

		boolean configPresent = Files.exists(EMULATOR_CONFIG_PATH);
		WatchService watchService = null;

		if (configPresent) {
			watchService = FileSystems.getDefault().newWatchService();
			EMULATOR_CONFIG_DIR.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
		}

		try {
			this.emulatorProcess = new ProcessBuilder("gcloud", "beta", "emulators", "pubsub", "start")
					.start();
		}
		catch (IOException e) {
			LOGGER.warn("Gcloud not found; leaving host/port uninitialized.");
			return;
		}

		boolean startStatus = (configPresent ? updateConfig(watchService) : createConfig());
		if (!startStatus) {
			LOGGER.warn("Pub/Sub emulator failed to start; leaving host/port uninitialized.");
			return;
		}

		Process envInitProcess = new ProcessBuilder("gcloud", "beta", "emulators", "pubsub", "env-init").start();

		// env-init output is a shell command of the form "export
		// PUBSUB_EMULATOR_HOST=localhost:8085".
		String emulatorInitString = new BufferedReader(new InputStreamReader(envInitProcess.getInputStream()))
				.readLine();
		envInitProcess.waitFor();
		this.emulatorHostPort = emulatorInitString.substring(emulatorInitString.indexOf('=') + 1);
	}

	/**
	 * Shut down the two emulator processes.
	 *
	 * gcloud command is shut down through the direct process handle. java process is
	 * identified and shut down through shell commands.
	 *
	 * There should normally be only one process with that host/port combination, but if there
	 * are more, they will be cleaned up as well.
	 */
	@Override
	protected void after() {
		if (this.emulatorProcess != null) {
			this.emulatorProcess.destroy();
		}

		if (this.emulatorHostPort == null) {
			return;
		}

		try {
			int portSeparatorIndex = this.emulatorHostPort.indexOf(":");
			if (portSeparatorIndex < 0 || !this.emulatorHostPort.contains("localhost")) {
				LOGGER.error("Malformed host, can't create emulator: " + this.emulatorHostPort);
				return;
			}
			String emulatorHost = this.emulatorHostPort.substring(0, portSeparatorIndex);
			String emulatorPort = this.emulatorHostPort.substring(portSeparatorIndex + 1);

			String hostPortParams = String.format("--host=%s --port=%s", emulatorHost, emulatorPort);
			Process psProcess = new ProcessBuilder("ps", "-v").start();
			new BufferedReader(new InputStreamReader(psProcess.getInputStream()))
					.lines()
					.filter(psLine -> psLine.contains(hostPortParams))
					.map(psLine -> new StringTokenizer(psLine).nextToken())
					.forEach(this::killProcess);
		}
		catch (IOException e) {
			LOGGER.error("Failed to cleanup: ", e);
		}
	}

	/**
	 * Return the already-started emulator's host/port combination when called from within a JUnit method.
	 *
	 * @return Emulator host/port string or null if emulator setup failed.
	 */
	public String getEmulatorHostPort() {
		return this.emulatorHostPort;
	}

	/**
	 * Wait until a PubSub emulator configuration file is present.
	 *
	 * Give up and log warning if the file does not appear after 10 seconds.
	 *
	 * @return whether configuration creation succeeded
	 *
	 * @throws InterruptedException which should interrupt the peaceful slumber and bubble up
	 * to fail the test.
	 */
	private boolean createConfig() throws InterruptedException {
		int attempts = 10;
		while (!Files.exists(EMULATOR_CONFIG_PATH) && --attempts >= 0) {
			Thread.sleep(1000);
		}
		if (attempts < 0) {
			LOGGER.warn(
					"Emulator could not be configured due to missing env.yaml. Are PubSub and beta tools installed?");
			return false;
		}
		return true;
	}

	/**
	 * Wait until a PubSub emulator configuration file is updated.
	 *
	 * Give up and log warning if the file does not update after 1 second.
	 *
	 * @return whether configuration update succeeded
	 * @throws InterruptedException which should interrupt the peaceful slumber and bubble up
	 * to fail the test.
	 */
	private boolean updateConfig(WatchService watchService) throws InterruptedException {
		int attempts = 10;
		while (--attempts >= 0) {
			WatchKey key = watchService.poll(100, TimeUnit.MILLISECONDS);

			if (key != null) {
				Optional<Path> configFilePath = key.pollEvents().stream()
						.map(event -> (Path) event.context())
						.filter(path -> ENV_FILE_NAME.equals(path.toString()))
						.findAny();
				if (configFilePath.isPresent()) {
					return true;
				}
			}
		}

		LOGGER.warn("Configuration file update could not be detected");
		return false;
	}

	/**
	 * Attempt to kill a process on best effort basis.
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
