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

package org.springframework.cloud.gcp.autoconfigure.datastore;

import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * If spring.cloud.gcp.datastore.emulator.enabled is set to true the emulator will be
 * started as a local datastore server using the
 * {@link com.google.cloud.datastore.testing.LocalDatastoreHelper}.
 *
 * @author Lucas Soares
 *
 * @since 1.2
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("spring.cloud.gcp.datastore.emulator.enabled")
@AutoConfigureBefore(GcpDatastoreAutoConfiguration.class)
@EnableConfigurationProperties(GcpDatastoreProperties.class)
@ConditionalOnMissingBean(LocalDatastoreHelper.class)
public class GcpDatastoreEmulatorAutoConfiguration implements SmartLifecycle {

	private static final Log LOGGER = LogFactory.getLog(GcpDatastoreEmulatorAutoConfiguration.class);

	private LocalDatastoreHelper helper;

	private volatile boolean running;

	@Bean
	public LocalDatastoreHelper createDatastoreHelper(
			GcpDatastoreProperties datastoreProperties) {
		EmulatorSettings settings = datastoreProperties.getEmulator();

		this.helper = LocalDatastoreHelper.create(settings.getConsistency(),
				settings.getPort());

		return this.helper;
	}

	/** Stops the instance of the emulator. */
	@Override
	public void stop() {
		if (!isRunning()) {
			LOGGER.warn("The datastore emulator is not running.");

			return;
		}

		try {
			LOGGER.info("Stopping datastore emulator.");

			this.helper.stop();

			LOGGER.info("Datastore emulator stopped.");

			this.running = false;
		}
		catch (Exception e) {
			LOGGER.warn("Failed to stop datastore instance.", e);
		}
	}

	/**
	 * Checks if the instance is running. This will be <code>true</code> after a successful
	 * execution of the method {@link #start()} and <code>false</code> after a successful
	 * execution of the method {@link #stop()}. method is called.
	 */
	@Override
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Starts the instance of the emulator.
	 */
	@Override
	public void start() {
		if (isRunning()) {
			LOGGER.warn("The datastore emulator is already running.");
			return;
		}

		try {
			LOGGER.info("Starting datastore emulator.");

			this.helper.start();

			LOGGER.info("Datastore emulator started.");

			this.running = true;
		}
		catch (Exception e) {
			LOGGER.error("Error constructing datastore instance.");
		}
	}
}
