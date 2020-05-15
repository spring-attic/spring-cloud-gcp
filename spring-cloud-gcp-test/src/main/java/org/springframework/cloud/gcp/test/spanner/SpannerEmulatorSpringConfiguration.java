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

import java.io.IOException;

import com.google.cloud.NoCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;

import org.springframework.cloud.gcp.test.EmulatorDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

/**
 * SpannerEmulatorSpringConfiguration should be used instead of JUnit class rule because spring context tries to close the connection when it is being destroyed.
 * But the rule would already shut down the emulator by that time. That causes tests to hang.
 * @author Mike Eltsufin
 * @author Elena Felder
 * @author Dmitry Solomakha
 * @since 1.2.3
 */
@Configuration
@Order(-1000)
public class SpannerEmulatorSpringConfiguration {

	private EmulatorDriver emulatorDriver = new EmulatorDriver(new SpannerEmulator(false));

	@Bean
	public SpannerOptions spannerOptions() throws IOException {
		// starting the emulator will change the default project ID to that of the emulator config.
		emulatorDriver.startEmulator();
		return SpannerOptions.newBuilder()
				.setProjectId(ServiceOptions.getDefaultProjectId())
				.setCredentials(NoCredentials.getInstance())
				.setEmulatorHost("localhost:9010")
				.build();
	}

	// the real destroy method would attempt to connect to the already-shutdown emulator instance,
	// causing tests to hang
	@Bean(destroyMethod = "")
	public Spanner spanner(SpannerOptions spannerOptions) {
		return spannerOptions.getService();
	}

	@EventListener
	public void afterCloseEvent(ContextClosedEvent event) {
		emulatorDriver.shutdownEmulator();
	}
}
