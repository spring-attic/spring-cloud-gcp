/*
 * Copyright 2017-2019 the original author or authors.
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

package com.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Integration test for the config client with local config server.
 *
 * <p>Because the test brings in config server/config client source files into a single project,
 * config server bootstrap need to be suppressed in the config client app instance.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
public class LocalSampleAppIntegrationTest {

	static final String CONFIG_DIR = "/tmp/config";

	static final String CONFIG_FILE = CONFIG_DIR + "/application.properties";

	static final String INITIAL_MESSAGE = "initial message";

	static final String UPDATED_MESSAGE = "updated message";

	final RestTemplate restTemplate = new RestTemplate();

	BufferedReader configServerOutput;

	BufferedReader configClientOutput;

	Process configServerProcess;

	Process configClientProcess;

	@BeforeClass
	public static void prepare() throws Exception {
		assumeThat(
			"PUB/SUB integration tests are disabled. Use '-Dit.pubsub=true' to enable.",
			System.getProperty("it.pubsub"), is("true"));

		Files.createDirectories(Paths.get(CONFIG_DIR));
	}

	@Test
	public void testSample() throws Exception {

		writeMessageToFile(INITIAL_MESSAGE);

		startConfigServer();

		waitForLogMessage(this.configServerOutput, "Monitoring for local config changes: [" + CONFIG_DIR + "]");
		waitForLogMessage(this.configServerOutput, "Started PubSubConfigServerApplication");
		assertConfigServerValue(INITIAL_MESSAGE);

		startConfigClient();

		waitForLogMessage(this.configClientOutput, "Located property source");
		waitForLogMessage(this.configClientOutput, "Started PubSubConfigApplication");
		assertConfigClientValue(INITIAL_MESSAGE);

		writeMessageToFile(UPDATED_MESSAGE);
		waitForLogMessage(this.configServerOutput, "Refresh for: *");
		assertConfigServerValue(UPDATED_MESSAGE);

		waitForLogMessage(this.configClientOutput, "Keys refreshed [example.message]");
		assertConfigClientValue(UPDATED_MESSAGE);
	}

	@AfterClass
	public static void tearDown() throws Exception {

		Path configFile = Paths.get(CONFIG_FILE);
		if (Files.exists(configFile)) {
			Files.delete(configFile);
		}

		Path configDir = Paths.get(CONFIG_DIR);
		if (Files.exists(configDir)) {
			Files.delete(configDir);
		}

	}

	@After
	public void closeResources() throws IOException {

		if (this.configServerOutput != null) {
			this.configServerOutput.close();
		}

		if (this.configClientOutput != null) {
			this.configClientOutput.close();
		}

		if (this.configServerProcess != null) {
			this.configServerProcess.destroy();
		}

		if (this.configClientProcess != null) {
			this.configClientProcess.destroy();
		}

	}

	private void startConfigServer() throws IOException {
		ProcessBuilder serverBuilder = new ProcessBuilder("mvn", "spring-boot:run",
				"-f", "../spring-cloud-gcp-pubsub-bus-config-sample-server-local");
		this.configServerProcess = serverBuilder.start();
		this.configServerOutput = new BufferedReader(new InputStreamReader(this.configServerProcess.getInputStream()));

	}

	private void startConfigClient() throws IOException {

		ProcessBuilder serverBuilder = new ProcessBuilder("mvn", "spring-boot:run",
				"-f", "../spring-cloud-gcp-pubsub-bus-config-sample-client");
		this.configClientProcess = serverBuilder.start();
		this.configClientOutput = new BufferedReader(new InputStreamReader(this.configClientProcess.getInputStream()));

	}

	private static void writeMessageToFile(String value) {
		File properties = new File(CONFIG_FILE);

		try (FileOutputStream fos = new FileOutputStream(properties)) {
			fos.write(("example.message = " + value).getBytes());
		}
		catch (IOException e) {
			fail("Could not write message to file", e);
		}
	}

	private void assertConfigServerValue(String message) {
		// Server is aware of value from filesystem.
		String serverPropertiesJson = this.restTemplate.getForObject("http://localhost:8888/application/default", String.class);
		assertThat(serverPropertiesJson).contains(message);
	}

	private void assertConfigClientValue(String message) {
		// Refresh scoped variable updated and returned.
		String value = this.restTemplate.getForObject("http://localhost:8080/message", String.class);
		assertThat(value).isEqualTo(message);
	}

	private void waitForLogMessage(BufferedReader reader, String message) {
		Awaitility.await(message)
			.atMost(60, TimeUnit.SECONDS)
			.until(() -> {
				// drain all lines up to the one requested, or until no more lines in reader.
				while (reader.ready()) {
					String line = reader.readLine();

					if (line == null) {
						return false;
					}

					if (line.contains(message)) {
						return true;
					}
				}
				return false;
			});
	}

}
