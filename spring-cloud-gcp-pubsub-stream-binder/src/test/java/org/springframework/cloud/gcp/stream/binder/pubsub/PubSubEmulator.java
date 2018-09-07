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

import org.junit.rules.ExternalResource;

public class PubSubEmulator extends ExternalResource {
	private PubSubTestBinder binder;

	// Needed for cleanup.
	private Process emulatorProcess;

	private String emulatorHost;

	private String emulatorPort;

	private final Path emulatorConfigPath = Paths.get(System.getProperty("user.home")).resolve(
			Paths.get(".config", "gcloud", "emulators", "pubsub", "env.yaml"));

	@Override
	protected void before() throws Throwable {

		this.emulatorProcess = new ProcessBuilder("gcloud", "beta", "emulators", "pubsub", "start").start();
		assertConfigPresent();

		Process envInitProcess = new ProcessBuilder("gcloud", "beta", "emulators", "pubsub", "env-init").start();
		String envVariable = new BufferedReader(new InputStreamReader(envInitProcess.getInputStream())).readLine();
		envInitProcess.waitFor();

		String emulatorHostPort = envVariable.substring(envVariable.indexOf('=') + 1);
		System.out.println("initialization completed; property = " + System.getenv("PUBSUB_EMULATOR_HOST")
				+ "; property from output = " + envVariable + "; hostname = " + emulatorHostPort);

		int portSeparatorIndex = emulatorHostPort.indexOf(":");
		if (portSeparatorIndex < 0 || !envVariable.contains("localhost")) {
			throw new RuntimeException("Malformed host, can't create emulator: " + envVariable);
		}
		this.emulatorHost = emulatorHostPort.substring(0, portSeparatorIndex);
		this.emulatorPort = emulatorHostPort.substring(portSeparatorIndex + 1);

		this.binder = new PubSubTestBinder(emulatorHostPort);
	}

	/**
	 * Shuts down the two emulator processes.
	 *
	 * gcloud command is shut down through the direct process handle.
	 * java process is identified and shut down through shell commands.
	 */
	@Override
	protected void after() {
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
			System.out.println("Failed to cleanup: ");
			e.printStackTrace();
		}
		this.emulatorProcess.destroy();

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
			throw new RuntimeException("Emulator could not be configured -- env.yaml not found");
		}
	}

	/**
	 * Attempts to kill a process on best effort basis.
	 *
	 * @param pid Presumably a valid PID. No checking done to validate.
	 */
	private void killProcess(String pid) {
		try {
			new ProcessBuilder("kill", pid).start();
		}
		catch (IOException e) {
			System.err.println(String.format("Failed to clean up PID %s", pid));
		}
	}
}
