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

/**
 * The main class used to start and stop an emulator.
 *
 * @author Elena Felder
 * @author Dmitry Solomakha
 * @author Mike Eltsufin
 *
 * @since 1.2.3
 */
public class EmulatorDriver {
	private static final String ENV_FILE_NAME = "env.yaml";

	private static final Log LOGGER = LogFactory.getLog(EmulatorDriver.class);

	private Emulator emulator;

	// Reference to emulator instance, for cleanup.
	private Process emulatorProcess;

	// Hostname and port combination for cleanup; host should always be localhost.
	private String emulatorHostPort;

	/**
	 * Creates and emulator driver based on the emulator definition.
	 * @param emulator An implementation of an {@link Emulator} interface.
	 */
	public EmulatorDriver(Emulator emulator) {
		this.emulator = emulator;
	}

	/**
	 * Starts the emulator.
	 */
	public void startEmulator() throws IOException {
		doStartEmulator();
		afterEmulatorStart();
		determineHostPort();
	}

	/**
	 * Stops the emulator.
	 */
	public void shutdownEmulator() {
		destroyGcloudEmulatorProcess();
		executeEmulatorKillCommands();
	}

	/**
	 * Return the already-started emulator's host/port combination when called from within a
	 * JUnit method.
	 * @return emulator host/port string or null if emulator setup failed.
	 */
	public String getEmulatorHostPort() {
		return this.emulatorHostPort;
	}

	private void destroyGcloudEmulatorProcess() {
		// destroy gcloud process
		if (this.emulatorProcess != null) {
			this.emulatorProcess.destroy();
		}
		else {
			LOGGER.warn("Emulator process is null after tests; nothing to terminate.");
		}
	}

	private void executeEmulatorKillCommands() {
		// kill
		for (String command : emulator.getKillCommandFragments(emulatorHostPort)) {
			killByCommand(command);
		}

		// post-kill
		for (String[] command : emulator.getPostKillCommands()) {
			runSystemCommand(command, false);
		}

	}

	private void killByCommand(String command) {
		AtomicBoolean foundProcess = new AtomicBoolean(false);

		try {
			Process psProcess = new ProcessBuilder("ps", "-vx").start();

			try (BufferedReader br = new BufferedReader(new InputStreamReader(psProcess.getInputStream()))) {
				br.lines()
						.filter((psLine) -> psLine.contains(command))
						.map((psLine) -> new StringTokenizer(psLine).nextToken())
						.forEach((p) -> {
							LOGGER.info("Found " + command + " process to kill: " + p);
							killProcess(p);
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

	private void doStartEmulator() throws IOException {
		Path emulatorConfigDir = Paths.get(System.getProperty("user.home")).resolve(
				Paths.get(".config", "gcloud", "emulators", emulator.getName()));
		Path emulatorConfigPath = emulatorConfigDir.resolve(ENV_FILE_NAME);

		boolean configPresent = emulatorConfigPath.toFile().exists();
		WatchService watchService = null;

		if (configPresent) {
			watchService = FileSystems.getDefault().newWatchService();
			emulatorConfigDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
		}

		try {
			this.emulatorProcess = new ProcessBuilder("gcloud", "beta", "emulators", emulator.getName(), "start")
					.start();
		}
		catch (IOException ex) {
			throw new EmulatorRuntimeException("Gcloud not found; leaving host/port uninitialized.", ex);
		}

		if (configPresent) {
			waitForConfigChange(watchService, ENV_FILE_NAME);
			watchService.close();
		}
		else {
			waitForConfigCreation(emulatorConfigPath);
		}
	}

	protected void afterEmulatorStart() {
		for (String[] command : this.emulator.getPostStartCommands()) {
			ProcessOutcome processOutcome = runSystemCommand(command, true);

			if (processOutcome.getStatus() != 0) {
				shutdownEmulator();
				throw new EmulatorRuntimeException("After emulator start command failed: "
						+ String.join("\n", processOutcome.getErrors()));
			}
		}
	}

	/**
	 * Extract host/port from output of env-init command: "export
	 * PUBSUB_EMULATOR_HOST=localhost:8085".
	 */
	private void determineHostPort() {
		ProcessOutcome processOutcome = runSystemCommand(
				new String[] { "gcloud", "beta", "emulators", this.emulator.getName(), "env-init" }, true);
		if (processOutcome.getOutput().isEmpty()) {
			throw new EmulatorRuntimeException("env-init command did not produce output");
		}
		String emulatorInitString = processOutcome.getOutput().get(0);
		this.emulatorHostPort = emulatorInitString.substring(emulatorInitString.indexOf('=') + 1);
	}

	private static ProcessOutcome runSystemCommand(String[] command, boolean failOnError) {
		ProcessOutcome processOutcome = runSystemCommand(command);
		if (failOnError && processOutcome.getStatus() != 0) {
			throw new EmulatorRuntimeException("Command execution failed: " + String.join(" ", command)
					+ "; output: " + processOutcome.getOutput()
					+ "; error: " + processOutcome.getErrors());

		}
		return processOutcome;
	}

	/**
	 * Attempt to kill a process on best effort basis. Failure is logged and ignored, as it is
	 * not critical to the tests' functionality.
	 * @param pid presumably a valid PID. No checking done to validate.
	 */
	private static void killProcess(String pid) {
		try {
			new ProcessBuilder("kill", "-9", pid).start();
		}
		catch (IOException ex) {
			LOGGER.error("Failed to clean up PID " + pid, ex);
		}
	}

	/**
	 * Wait until the emulator configuration file is present. Fail if the file does not
	 * appear after 10 seconds.
	 */
	private static void waitForConfigCreation(Path emulatorConfigPath) {
		Awaitility.await("Emulator could not be configured due to missing env.yaml. Is the emulator installed?")
				.atMost(10, TimeUnit.SECONDS)
				.pollInterval(100, TimeUnit.MILLISECONDS)
				.until(() -> emulatorConfigPath.toFile().exists());
	}


	/**
	 * Wait until the emulator configuration file is updated. Fail if the file does not
	 * update after 10 second.
	 * @param watchService the watch-service to poll
	 */
	private static void waitForConfigChange(WatchService watchService, String envFileName) {
		Awaitility.await("Configuration file update could not be detected")
				.atMost(20, TimeUnit.SECONDS)
				.pollInterval(100, TimeUnit.MILLISECONDS)
				.until(() -> {
					WatchKey key = watchService.poll(100, TimeUnit.MILLISECONDS);

					if (key != null) {
						Optional<Path> configFilePath = key.pollEvents().stream()
								.map((event) -> (Path) event.context())
								.filter((path) -> envFileName.equals(path.toString()))
								.findAny();
						return configFilePath.isPresent();
					}
					return false;
				});
	}

	private static ProcessOutcome runSystemCommand(String[] command) {

		Process envInitProcess = null;
		try {
			envInitProcess = new ProcessBuilder(command).start();
		}
		catch (IOException e) {
			throw new EmulatorRuntimeException(e);
		}

		try (BufferedReader brInput = new BufferedReader(new InputStreamReader(envInitProcess.getInputStream()));
				BufferedReader brError = new BufferedReader(new InputStreamReader(envInitProcess.getErrorStream()))) {
			return new ProcessOutcome(
					brInput.lines().collect(Collectors.toList()),
					brError.lines().collect(Collectors.toList()),
					envInitProcess.waitFor());
		}
		catch (IOException e) {
			throw new EmulatorRuntimeException(e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EmulatorRuntimeException(e);
		}
	}

	private static class ProcessOutcome {

		private List<String> output;

		private List<String> errors;

		private int status;

		ProcessOutcome(List<String> output, List<String> errors, int status) {
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
	}
}
