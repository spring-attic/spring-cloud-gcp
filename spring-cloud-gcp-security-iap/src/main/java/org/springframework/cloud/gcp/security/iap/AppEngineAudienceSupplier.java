/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.security.iap;

import java.util.function.Supplier;

import com.google.cloud.resourcemanager.Project;
import com.google.cloud.resourcemanager.ResourceManager;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.security.oauth2.jwt.Jwt} token validator for GCP App Engine (both Flexible and Standard)
 * audience strings.
 *
 * @author Elena Felder
 *
 * @since 1.1
 */
public class AppEngineAudienceSupplier implements Supplier<String> {

	/**
	 * App Engine audience format: {@code /projects/PROJECT_NUMBER/apps/PROJECT_ID}.
	 */
	public static final String AUDIENCE_FORMAT = "/projects/%s/apps/%s";

	private GcpProjectIdProvider projectIdProvider;

	private ResourceManager resourceManager;

	public AppEngineAudienceSupplier(GcpProjectIdProvider projectIdProvider, ResourceManager resourceManager) {
		Assert.notNull(projectIdProvider, "GcpProjectIdProvider cannot be null.");
		Assert.notNull(projectIdProvider.getProjectId(), "Project Id expected not to be null.");
		Assert.notNull(resourceManager, "ResourceManager cannot be null.");

		this.projectIdProvider = projectIdProvider;
		this.resourceManager = resourceManager;
	}

	@Override
	public String get() {

		Project project = this.resourceManager.get(this.projectIdProvider.getProjectId());
		Assert.notNull(project, "Project expected not to be null.");
		Assert.notNull(project.getProjectNumber(), "Project Number expected not to be null.");

		return String.format(AUDIENCE_FORMAT, project.getProjectNumber(), this.projectIdProvider.getProjectId());
	}
}
