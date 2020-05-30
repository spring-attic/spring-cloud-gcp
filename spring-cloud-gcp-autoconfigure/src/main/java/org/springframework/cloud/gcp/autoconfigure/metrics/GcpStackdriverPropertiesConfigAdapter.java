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

package org.springframework.cloud.gcp.autoconfigure.metrics;

import com.google.api.gax.core.CredentialsProvider;

import org.springframework.boot.actuate.autoconfigure.metrics.export.stackdriver.StackdriverProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.export.stackdriver.StackdriverPropertiesConfigAdapter;

/**
 * @author Eddú Meléndez
 * @since 1.2.4
 */
public class GcpStackdriverPropertiesConfigAdapter extends StackdriverPropertiesConfigAdapter {

	private String projectId;

	private CredentialsProvider credentialsProvider;

	public GcpStackdriverPropertiesConfigAdapter(StackdriverProperties properties) {
		super(properties);
	}

	public GcpStackdriverPropertiesConfigAdapter(StackdriverProperties properties, String projectId,
			CredentialsProvider credentialsProvider) {
		this(properties);
		this.projectId = projectId;
		this.credentialsProvider = credentialsProvider;
	}

	@Override
	public String projectId() {
		return this.projectId;
	}

	@Override
	public CredentialsProvider credentials() {
		return this.credentialsProvider;
	}
}
