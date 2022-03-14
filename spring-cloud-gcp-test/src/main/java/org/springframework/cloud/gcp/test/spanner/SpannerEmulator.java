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

package org.springframework.cloud.gcp.test.spanner;

import org.junit.Assume;

import org.springframework.cloud.gcp.test.Emulator;

public class SpannerEmulator implements Emulator {

	public SpannerEmulator() {
	}

	public SpannerEmulator(boolean checkEnvironmentFlag) {
		if (checkEnvironmentFlag) {
			String emulatorHost = System.getenv("SPANNER_EMULATOR_HOST");
			Assume.assumeFalse(
					"Set SPANNER_EMULATOR_HOST environment variable prior to running emulator tests; copy output of this command and run it:\ngcloud beta emulators spanner env-init",
					emulatorHost == null || emulatorHost.isEmpty());
		}
	}

	public String getName() {
		return "spanner";
	}

	@Override
	public String[] getKillCommandFragments(String hostPort) {
		return new String[] {
				"cloud_spanner_emulator/emulator_main",
				"cloud_spanner_emulator/gateway_main" };
	}

	@Override
	public String[][] getPostStartCommands() {
		return new String[][] {
				{"gcloud", "config", "configurations", "activate", "emulator" },
				{"gcloud", "spanner", "instances", "create", "integration-instance", "--config=emulator",
				"--description=\"Test Instance\"", "--nodes=1"}
		};
	}

	@Override
	public String[][] getPostKillCommands() {
		return new String[][] { {
				"gcloud", "config", "configurations", "activate", "default" } };
	}

}
