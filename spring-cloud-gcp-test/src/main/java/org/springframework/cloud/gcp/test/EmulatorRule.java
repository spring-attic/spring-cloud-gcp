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

import org.junit.rules.ExternalResource;

/**
 * @author Elena Felder
 * @author Dmitry Solomakha
 * @author Mike Eltsufin
 * @since 1.2.3
 */
public class EmulatorRule extends ExternalResource {

	private EmulatorDriver emulatorDriver;

	public EmulatorRule(Emulator emulator) {
		this.emulatorDriver = new EmulatorDriver(emulator);
	}

	@Override
	protected void before() throws Throwable {
		emulatorDriver.startEmulator();
	}

	@Override
	protected void after() {
		emulatorDriver.shutdownEmulator();
	}

	/**
	 * Return the already-started emulator's host/port combination when called from within a
	 * JUnit method.
	 * @return emulator host/port string or null if emulator setup failed.
	 */
	public String getEmulatorHostPort() {
		return emulatorDriver.getEmulatorHostPort();
	}

}
