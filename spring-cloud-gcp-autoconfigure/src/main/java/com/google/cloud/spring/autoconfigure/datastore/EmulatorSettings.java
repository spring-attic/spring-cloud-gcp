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

package com.google.cloud.spring.autoconfigure.datastore;

import java.nio.file.Path;

/**
 * Properties for configuring Cloud Datastore Emulator.
 *
 * See https://cloud.google.com/sdk/gcloud/reference/beta/emulators/datastore/start for the correspondent properties
 *
 * @author Lucas Soares
 *
 * @since 1.2
 */
public class EmulatorSettings {
	/**
	 * If enabled the Datastore client will connect to an local datastore emulator.
	 */
	private boolean enabled;

	/**
	 * Is the datastore emulator port.
	 * Correspondent CLI property: --host-port=localhost:{@link EmulatorSettings #port} Default: {@code 8081}
	 */
	private int port = 8081;

	/**
	 * Consistency to use creating the Datastore server instance.
	 * Correspondent CLI property: --consistency Default: {@code 0.9}
	 */
	private double consistency = 0.9D;

	/**
	 * Configures whether or not the emulator should persist any data to disk for the emulator session.
	 * Correspondent CLI property: --store-on-disk. Default: {@code true}
	 */
	private boolean storeOnDisk = true;

	/**
	 * The directory to be used to store/retrieve data/config for an emulator run.
	 * Correspondent CLI property: --data-dir. The default value ${USER_CONFIG_DIR}/emulators/datastore is used on null
	 */
	private Path dataDir;

	public Path getDataDir() {
		return dataDir;
	}

	public void setDataDir(Path dataDir) {
		this.dataDir = dataDir;
	}

	public boolean isStoreOnDisk() {
		return storeOnDisk;
	}

	public void setStoreOnDisk(boolean storeOnDisk) {
		this.storeOnDisk = storeOnDisk;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public double getConsistency() {
		return consistency;
	}

	public void setConsistency(double consistency) {
		this.consistency = consistency;
	}
}
