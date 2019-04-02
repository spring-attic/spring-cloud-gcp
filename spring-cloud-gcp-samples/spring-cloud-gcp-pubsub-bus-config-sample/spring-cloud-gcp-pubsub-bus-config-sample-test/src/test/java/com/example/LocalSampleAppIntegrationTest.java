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
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.bouncycastle.util.io.TeeOutputStream;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Integration test for the config client with local config server.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
public class LocalSampleAppIntegrationTest {

	private RestTemplate restTemplate = new RestTemplate();

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

		Files.createDirectories(Paths.get("/tmp/config_pubsub_integration_test"));
		File properties = new File("/tmp/config_pubsub_integration_test/application.properties");

		try (FileOutputStream fos = new FileOutputStream(properties)) {
			fos.write("example.message = INTEGRATION TEST\n".getBytes());
		}
	}

	@Test
	public void testSample() {


		SpringApplicationBuilder configServer = new SpringApplicationBuilder(PubSubConfigServerApplication.class)
			.properties("server.port=8888",
				"spring.profiles.active=native",
				"spring.cloud.config.server.native.searchLocations=file:/tmp/config_pubsub_integration_test/");
		configServer.run();

		// TODO: wait until config refreshes
		Awaitility.await().atMost(60, TimeUnit.SECONDS)
			.until(() -> {
				return baos.toString().contains("Refreshed configuration");
			});

		SpringApplicationBuilder configClient = new SpringApplicationBuilder(PubSubConfigApplication.class)
			.properties("server.port=8081",
				"spring.cloud.config.server.bootstrap=false",
				"spring.profiles.active=native");
		configClient.run();

		String value = this.restTemplate.getForObject("http://localhost:8081/message", String.class);
		assertThat(value).isEqualTo("INTEGRATION TEST");
	}

}
