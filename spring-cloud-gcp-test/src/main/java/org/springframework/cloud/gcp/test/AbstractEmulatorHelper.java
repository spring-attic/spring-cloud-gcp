/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.cloud.gcp.test;

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
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.awaitility.Awaitility;

abstract class AbstractEmulatorHelper {

	private final Path EMULATOR_CONFIG_DIR = Paths.get(System.getProperty("user.home")).resolve(
			Paths.get(".config", "gcloud", "emulators", getEmulatorName()));

	private static final String ENV_FILE_NAME = "env.yaml";

	private final Path EMULATOR_CONFIG_PATH = EMULATOR_CONFIG_DIR.resolve(ENV_FILE_NAME);

	private static final Log LOGGER = LogFactory.getLog(AbstractEmulatorHelper.class);

	// Reference to emulator instance, for cleanup.
	private Process emulatorProcess;

	// Hostname for cleanup, should always be localhost.
	private String emulatorHostPort;

	abstract String getGatingPropertyName();

	/**
	 * Launch an instance of pubsub emulator or skip all tests. If it.pubsub-emulator
	 * environmental property is off, all tests will be skipped through the failed assumption.
	 * If the property is on, any setup failure will trigger test failure. Failures during
	 * teardown are merely logged.
	 * @throws IOException if config file creation or directory watcher on existing file
	 *     fails.
	 * @throws InterruptedException if process is stopped while waiting to retry.
	 */
	public void startEmulator() throws IOException, InterruptedException {

		beforeEmulatorStart();
		doStartEmulator();
		afterEmulatorStart();
		determineHostPort();
	}

	/**
	 * Shut down the two emulator processes. gcloud command is shut down through the direct
	 * process handle. java process is identified and shut down through shell commands. There
	 * should normally be only one process with that host/port combination, but if there are
	 * more, they will be cleaned up as well. Any failure is logged and ignored since it's not
	 * critical to the tests' operation.
	 */
	public void shutdownEmulator() {
		findAndDestroyEmulator();
		afterEmulatorDestroyed();
	}

	protected void afterEmulatorDestroyed() {
		// does nothing by default.
	}

	private void findAndDestroyEmulator() {
		// destroy gcloud process
		if (this.emulatorProcess != null) {
			this.emulatorProcess.destroy();
		}
		else {
			LOGGER.warn("Emulator process null after tests; nothing to terminate.");
		}
	}

	protected void killByCommand(String command) {
		AtomicBoolean foundProcess = new AtomicBoolean(false);

		try {
			Process psProcess = new ProcessBuilder("ps", "-vx").start();

			try (BufferedReader br = new BufferedReader(new InputStreamReader(psProcess.getInputStream()))) {
				br.lines()
						.filter((psLine) -> psLine.contains(command))
						.peek(line -> System.out.println("found line after filter: " + line))
						.map((psLine) -> new StringTokenizer(psLine).nextToken())
						.forEach((p) -> {
							LOGGER.info("Found " + command + " process to kill: " + p);
							this.killProcess(p);
							foundProcess.set(true);
						});
			}

			if (!foundProcess.get()) {
				LOGGER.warn("Did not find the emulator process to kill based on: " + command);
			}
		}
		catch (IOException ex) {
			LOGGER.warn("Failed to cleanup: ", ex);
		}
	}

	/**
	 * Return the already-started emulator's host/port combination when called from within a
	 * JUnit method.
	 * @return emulator host/port string or null if emulator setup failed.
	 */
	public String getEmulatorHostPort() {
		return this.emulatorHostPort;
	}

	private void doStartEmulator() throws IOException, InterruptedException {
		boolean configPresent = Files.exists(EMULATOR_CONFIG_PATH);
		WatchService watchService = null;

		if (configPresent) {
			watchService = FileSystems.getDefault().newWatchService();
			EMULATOR_CONFIG_DIR.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
		}

		try {
			this.emulatorProcess = new ProcessBuilder("gcloud", "beta", "emulators", getEmulatorName(), "start")
					.start();
		}
		catch (IOException ex) {
			throw new RuntimeException("Gcloud not found; leaving host/port uninitialized.", ex);
		}

		if (configPresent) {
			updateConfig(watchService);
			watchService.close();
		}
		else {
			waitForConfigCreation();
		}

	}

	protected void beforeEmulatorStart() {
		// does nothing by default
	}

	protected void afterEmulatorStart() {
		// does nothing by default
	}

	/**
	 * Extract host/port from output of env-init command: "export
	 * PUBSUB_EMULATOR_HOST=localhost:8085".
	 * @throws IOException for IO errors
	 * @throws InterruptedException for interruption errors
	 */
	private void determineHostPort() {
		ProcessOutcome processOutcome = runSystemCommand(
				new String[] { "gcloud", "beta", "emulators", getEmulatorName(), "env-init" });
		if (processOutcome.getOutput().size() < 1) {
			throw new RuntimeException("env-init command did not produce output");
		}
		String emulatorInitString = processOutcome.getOutput().get(0);
		this.emulatorHostPort = emulatorInitString.substring(emulatorInitString.indexOf('=') + 1);
	}

	protected ProcessOutcome runSystemCommand(String[] command) {
		return runSystemCommand(command, true);
	}

	protected ProcessOutcome runSystemCommand(String[] command, boolean failOnError) {

		Process envInitProcess = null;
		try {
			envInitProcess = new ProcessBuilder(command).start();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

		try (BufferedReader brInput = new BufferedReader(new InputStreamReader(envInitProcess.getInputStream()));
				BufferedReader brError = new BufferedReader(new InputStreamReader(envInitProcess.getErrorStream()))) {
			ProcessOutcome processOutcome = new ProcessOutcome(command,
					brInput.lines().collect(Collectors.toList()),
					brError.lines().collect(Collectors.toList()),
					envInitProcess.waitFor());

			if (failOnError && processOutcome.status != 0) {
				throw new RuntimeException("Command execution failed: " + String.join(" ", command)
						+ "; output: " + processOutcome.getOutput()
						+ "; error: " + processOutcome.getErrors());

			}
			return processOutcome;
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	abstract String getEmulatorName();

	/**
	 * Wait until a PubSub emulator configuration file is present. Fail if the file does not
	 * appear after 10 seconds.
	 * @throws InterruptedException which should interrupt the peaceful slumber and bubble up
	 *     to fail the test.
	 */
	private void waitForConfigCreation() throws InterruptedException {
		Awaitility.await("Emulator could not be configured due to missing env.yaml. Is the emulator installed?")
				.atMost(10, TimeUnit.SECONDS)
				.pollInterval(100, TimeUnit.MILLISECONDS)
				.until(() -> Files.exists(EMULATOR_CONFIG_PATH));
	}

	/**
	 * Wait until a PubSub emulator configuration file is updated. Fail if the file does not
	 * update after 1 second.
	 * @param watchService the watch-service to poll
	 * @throws InterruptedException which should interrupt the peaceful slumber and bubble up
	 *     to fail the test.
	 */
	private void updateConfig(WatchService watchService) throws InterruptedException {
		Awaitility.await("Configuration file update could not be detected")
				.atMost(10, TimeUnit.SECONDS)
				.pollInterval(100, TimeUnit.MILLISECONDS)
				.until(() -> {
					WatchKey key = watchService.poll(100, TimeUnit.MILLISECONDS);

					if (key != null) {
						Optional<Path> configFilePath = key.pollEvents().stream()
								.map((event) -> (Path) event.context())
								.filter((path) -> ENV_FILE_NAME.equals(path.toString()))
								.findAny();
						return configFilePath.isPresent();
					}
					return false;
				});
	}

	/**
	 * Attempt to kill a process on best effort basis. Failure is logged and ignored, as it is
	 * not critical to the tests' functionality.
	 * @param pid presumably a valid PID. No checking done to validate.
	 */
	protected void killProcess(String pid) {
		try {
			new ProcessBuilder("kill", "-9", pid).start();
		}
		catch (IOException ex) {
			LOGGER.warn("Failed to clean up PID " + pid);
		}
	}

	static class ProcessOutcome {
		private String[] command;

		private List<String> output;

		private List<String> errors;

		private int status;

		ProcessOutcome(String[] command, List<String> output, List<String> errors, int status) {
			this.command = command;
			this.output = output;
			this.errors = errors;
			this.status = status;
		}

		public List<String> getOutput() {
			return output;
		}

		public List<String> getErrors() {
			return errors;
		}

		public int getStatus() {
			return status;
		}

		public String getCommandString() {
			return String.join(" ", command);
		}
	}

}
