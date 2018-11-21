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
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.ExternalResource;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Rule for instantiating and tearing down a Pub/Sub emulator instance.
 *
 * Tests can access the emulator's host/port combination by calling {@link #getEmulatorHostPort()} method.
 *
 * @author Elena Felder
 * @author Mike Eltsufin
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

	// Conditional rule execution based on an environmental flag.
	private boolean enableTests;

	public PubSubEmulator() {
		if ("true".equals(System.getProperty("it.pubsub-emulator"))) {
			this.enableTests = true;
		}
		else {
			LOGGER.warn("PubSubEmulator rule disabled. Please enable with -Dit.pubsub-emulator.");
		}
	}

	/**
	 * Launch an instance of pubsub emulator or skip all tests.
	 * If it.pubsub-emulator environmental property is off, all tests will be skipped through the failed assumption.
	 * If the property is on, any setup failure will trigger test failure. Failures during teardown are merely logged.
	 * @throws IOException if config file creation or directory watcher on existing file fails.
	 * @throws InterruptedException if process is stopped while waiting to retry.
	 */
	@Override
	protected void before() throws IOException, InterruptedException {

		assumeTrue("PubSubEmulator rule disabled. Please enable with -Dit.pubsub-emulator.", this.enableTests);

		startEmulator();
		determineHostPort();
	}

	/**
	 * Shut down the two emulator processes.
	 * gcloud command is shut down through the direct process handle. java process is
	 * identified and shut down through shell commands.
	 * There should normally be only one process with that host/port combination, but if there
	 * are more, they will be cleaned up as well.
	 * Any failure is logged and ignored since it's not critical to the tests' operation.
	 */
	@Override
	protected void after() {
		findAndDestroyEmulator();
	}

	private void findAndDestroyEmulator() {
		// destroy gcloud process
		if (this.emulatorProcess != null) {
			this.emulatorProcess.destroy();
		}
		else {
			LOGGER.warn("Emulator process null after tests; nothing to terminate.");
		}

		// find destory emulator process spawned by gcloud
		if (this.emulatorHostPort == null) {
			LOGGER.warn("Host/port null after the test.");
		}
		else {
			int portSeparatorIndex = this.emulatorHostPort.lastIndexOf(":");
			if (portSeparatorIndex < 0) {
				LOGGER.warn("Malformed host: " + this.emulatorHostPort);
				return;
			}

			String emulatorHost = this.emulatorHostPort.substring(0, portSeparatorIndex);
			String emulatorPort = this.emulatorHostPort.substring(portSeparatorIndex + 1);

			AtomicBoolean foundEmulatorProcess = new AtomicBoolean(false);
			String hostPortParams = String.format("--host=%s --port=%s", emulatorHost, emulatorPort);
			try {
				Process psProcess = new ProcessBuilder("ps", "-vx").start();

				try (BufferedReader br = new BufferedReader(new InputStreamReader(psProcess.getInputStream()))) {
					br.lines()
							.filter(psLine -> psLine.contains(hostPortParams))
							.map(psLine -> new StringTokenizer(psLine).nextToken())
							.forEach(p -> {
								LOGGER.info("Found emulator process to kill: " + p);
								this.killProcess(p);
								foundEmulatorProcess.set(true);
							});
				}

				if (!foundEmulatorProcess.get()) {
					LOGGER.warn("Did not find the emualtor process to kill based on: " + hostPortParams);
				}
			}
			catch (IOException e) {
				LOGGER.warn("Failed to cleanup: ", e);
			}
		}
	}

	/**
	 * Return the already-started emulator's host/port combination when called from within a
	 * JUnit method.
	 * @return Emulator host/port string or null if emulator setup failed.
	 */
	public String getEmulatorHostPort() {
		return this.emulatorHostPort;
	}

	private void startEmulator() throws IOException, InterruptedException {
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
			fail("Gcloud not found; leaving host/port uninitialized.");
		}

		if (configPresent) {
			updateConfig(watchService);
			watchService.close();
		}
		else {
			createConfig();
		}

	}

	/**
	 * Extract host/port from output of env-init command: "export PUBSUB_EMULATOR_HOST=localhost:8085".
	 */
	private void determineHostPort() throws IOException, InterruptedException {
		Process envInitProcess = new ProcessBuilder("gcloud", "beta", "emulators", "pubsub", "env-init").start();


		try (BufferedReader br = new BufferedReader(new InputStreamReader(envInitProcess.getInputStream()))) {
			String emulatorInitString = br.readLine();
			envInitProcess.waitFor();
			this.emulatorHostPort = emulatorInitString.substring(emulatorInitString.indexOf('=') + 1);
		}
	}

	/**
	 * Wait until a PubSub emulator configuration file is present.
	 * Fail if the file does not appear after 10 seconds.
	 * @throws InterruptedException which should interrupt the peaceful slumber and bubble up
	 * to fail the test.
	 */
	private void createConfig() throws InterruptedException {
		int attempts = 10;
		while (!Files.exists(EMULATOR_CONFIG_PATH) && --attempts >= 0) {
			Thread.sleep(1000);
		}
		if (attempts < 0) {
			fail(
					"Emulator could not be configured due to missing env.yaml. Are PubSub and beta tools installed?");
		}
	}

	/**
	 * Wait until a PubSub emulator configuration file is updated.
	 * Fail if the file does not update after 1 second.
	 * @throws InterruptedException which should interrupt the peaceful slumber and bubble up
	 * to fail the test.
	 */
	private void updateConfig(WatchService watchService) throws InterruptedException {
		int attempts = 10;
		while (--attempts >= 0) {
			WatchKey key = watchService.poll(100, TimeUnit.MILLISECONDS);

			if (key != null) {
				Optional<Path> configFilePath = key.pollEvents().stream()
						.map(event -> (Path) event.context())
						.filter(path -> ENV_FILE_NAME.equals(path.toString()))
						.findAny();
				if (configFilePath.isPresent()) {
					return;
				}
			}
		}

		fail("Configuration file update could not be detected");
	}

	/**
	 * Attempt to kill a process on best effort basis.
	 * Failure is logged and ignored, as it is not critical to the tests' functionality.
	 * @param pid Presumably a valid PID. No checking done to validate.
	 */
	private void killProcess(String pid) {
		try {
			new ProcessBuilder("kill", pid).start();
		}
		catch (IOException e) {
			LOGGER.warn("Failed to clean up PID " + pid);
		}
	}
}
