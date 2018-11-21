/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.security.iap;

import com.google.cloud.resourcemanager.Project;
import com.google.cloud.resourcemanager.ResourceManager;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.MetadataProvider;
import org.springframework.util.Assert;

public class ComputeEngineAudienceValidator extends AudienceValidator {

	public ComputeEngineAudienceValidator(GcpProjectIdProvider projectIdProvider, ResourceManager resourceManager,
			MetadataProvider metadataProvider) {
		Assert.notNull(projectIdProvider, "GcpProjectIdProvider cannot be null.");
		Assert.notNull(resourceManager, "ResourceManager cannot be null.");
		Assert.notNull(metadataProvider, "MetadataProvider cannot be null.");

		Project project = resourceManager.get(projectIdProvider.getProjectId());
		String serviceId = metadataProvider.getAttribute("id");

		setAudience(String.format(
				"/projects/%s/global/backendServices/%s", project.getProjectNumber(), serviceId));
	}

}
