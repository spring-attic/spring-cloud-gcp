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

import org.junit.Assume;

public class SpannerEmulatorHelper extends AbstractEmulatorHelper {
	private boolean checkEnvironmentFlag = true;

	public SpannerEmulatorHelper() {
		// keep default value of checkEnvironmentFlag=true.
	}

	public SpannerEmulatorHelper(boolean checkEnvironmentFlag) {
			this.checkEnvironmentFlag = checkEnvironmentFlag;
	}

	String getGatingPropertyName() {
		return "it.spanner-emulator";
	}

	String getEmulatorName() {
		return "spanner";
	}

	@Override
	protected void beforeEmulatorStart() {
		if (this.checkEnvironmentFlag) {
			String emulatorHost = System.getenv("SPANNER_EMULATOR_HOST");
			Assume.assumeFalse(
					"Set SPANNER_EMULATOR_HOST environment variable prior to running emulator tests; copy output of this command and run it:\ngcloud beta emulators spanner env-init",
					emulatorHost == null || emulatorHost.isEmpty());
		}
	}

	@Override
	protected void afterEmulatorStart() {
		runSystemCommand(new String[] {
				"gcloud", "config", "configurations", "activate", "emulator" });

		ProcessOutcome processOutcome = runSystemCommand(new String[] {
				"gcloud", "spanner", "instances", "create", "integration-instance", "--config=emulator",
				"--description=\"Test Instance\"", "--nodes=1" },
				false);

		if (processOutcome.getStatus() != 0) {
			// don't set breakpoint here
			cleanupSpannerEmulator();
			throw new RuntimeException("Creating instance failed: "
					+ String.join("\n", processOutcome.getErrors()));
		}
	}

	@Override
	protected void afterEmulatorDestroyed() {
		cleanupSpannerEmulator();
		runSystemCommand(new String[] {
				"gcloud", "config", "configurations", "activate", "default" });
	}

	private void cleanupSpannerEmulator() {
		this.killByCommand("cloud_spanner_emulator/emulator_main");
		this.killByCommand("cloud_spanner_emulator/gateway_main");
	}

}
