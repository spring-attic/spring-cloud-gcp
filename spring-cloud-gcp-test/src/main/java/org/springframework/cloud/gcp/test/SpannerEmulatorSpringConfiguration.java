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

import java.io.IOException;

import com.google.cloud.NoCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

@Configuration
@Order(-1000)
public class SpannerEmulatorSpringConfiguration {

	private SpannerEmulatorHelper emulatorHelper = new SpannerEmulatorHelper(false);

	@Bean
	public SpannerOptions spannerOptions() throws IOException, InterruptedException {
		// starting the emulator will change the default project ID to that of the emulator config.
		emulatorHelper.startEmulator();
		return SpannerOptions.newBuilder()
				.setProjectId(ServiceOptions.getDefaultProjectId())
				.setCredentials(NoCredentials.getInstance())
				.setEmulatorHost("localhost:9010")
				.build();
	}

	// the real destroy method would attempt to connect the already-shutdown emulator instance,
	// causing tests to fail.
	@Bean(destroyMethod = "")
	public Spanner spanner(SpannerOptions spannerOptions) {
		return spannerOptions.getService();
	}

	@EventListener
	public void afterCloseEvent(ContextClosedEvent event) {
		emulatorHelper.shutdownEmulator();
	}
}
