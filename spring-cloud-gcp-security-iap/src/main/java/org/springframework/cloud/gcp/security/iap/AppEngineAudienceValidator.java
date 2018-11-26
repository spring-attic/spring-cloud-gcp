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
public class AppEngineAudienceValidator extends AudienceValidator {

	public static final String AUDIENCE_FORMAT = "/projects/%s/apps/%s";

	public AppEngineAudienceValidator(GcpProjectIdProvider projectIdProvider, ResourceManager resourceManager) {
		Assert.notNull(projectIdProvider, "GcpProjectIdProvider cannot be null.");
		Assert.notNull(projectIdProvider.getProjectId(), "Project Id expected not to be null.");
		Assert.notNull(resourceManager, "ResourceManager cannot be null.");

		Project project = resourceManager.get(projectIdProvider.getProjectId());
		Assert.notNull(project, "Project expected not to be null.");
		Assert.notNull(project.getProjectNumber(), "Project Number expected not to be null.");

		setAudience(String.format(AUDIENCE_FORMAT, project.getProjectNumber(), projectIdProvider.getProjectId()));
	}
}
