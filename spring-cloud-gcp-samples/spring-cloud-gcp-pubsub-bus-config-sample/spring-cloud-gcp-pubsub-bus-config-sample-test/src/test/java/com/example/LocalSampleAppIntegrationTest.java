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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.bouncycastle.util.io.TeeOutputStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.boot.builder.SpringApplicationBuilder;
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

	static final String CONFIG_DIR = "/tmp/config_pubsub_integration_test";

	static final String CONFIG_FILE = CONFIG_DIR + "/application.properties";

	static final String INITIAL_MESSAGE = "initial message";

	static final String UPDATED_MESSAGE = "initial message";

	private final RestTemplate restTemplate = new RestTemplate();

	private static PrintStream systemOut;

	private static ByteArrayOutputStream baos;

	@BeforeClass
	public static void prepare() throws Exception {
		assumeThat(
			"PUB/SUB integration tests are disabled. Use '-Dit.pubsub=true' to enable.",
			System.getProperty("it.pubsub"), is("true"));

		systemOut = System.out;
		baos = new ByteArrayOutputStream();
		TeeOutputStream out = new TeeOutputStream(systemOut, baos);
		System.setOut(new PrintStream(out));

		Files.createDirectories(Paths.get(CONFIG_DIR));
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

		System.setOut(systemOut);
	}

	@Test
	public void testSample() throws Exception {

		writeMessageToFile(INITIAL_MESSAGE);

		startConfigServer();
		waitForLogMessage("Monitoring for local config changes: [" + CONFIG_DIR + "]");
		assertConfigServerValue(INITIAL_MESSAGE);

		startConfigClient();
		waitForLogMessage("Located property source");
		assertConfigClientValue(INITIAL_MESSAGE);

		writeMessageToFile(UPDATED_MESSAGE);
		waitForLogMessage("Refresh for: *");
		assertConfigServerValue(UPDATED_MESSAGE);

		String value = this.restTemplate.getForObject("http://localhost:8081/message", String.class);

		waitForLogMessage("Keys refreshed [example.message]");
		assertConfigClientValue(UPDATED_MESSAGE);
	}

	private void startConfigServer() {
		SpringApplicationBuilder
			configServer = new SpringApplicationBuilder(PubSubConfigServerApplication.class)
			.properties("server.port=8888",
				"spring.profiles.active=native",
				"spring.cloud.config.server.native.searchLocations=file:" + CONFIG_DIR);
		configServer.run();
	}

	private void startConfigClient() {
		SpringApplicationBuilder
			configClient = new SpringApplicationBuilder(PubSubConfigApplication.class)
			.properties("server.port=8081",
				// suppress config server behavior.
				"spring.cloud.config.server.bootstrap=false",
				// re-enable config client behavior.
				"spring.cloud.config.enabled=true",
				"spring.cloud.config.watch.enabled=true",
				// Suppress Git validation configured through spring.provides in config server module.
				"spring.profiles.active=native"
			);
		configClient.run();
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
		String value = this.restTemplate.getForObject("http://localhost:8081/message", String.class);
		assertThat(value).isEqualTo(message);
	}

	private void waitForLogMessage(String message) {
		Awaitility.await(message)
			.atMost(60, TimeUnit.SECONDS)
			.until(() -> {
				return baos.toString().contains(message);
			});
	}

}
