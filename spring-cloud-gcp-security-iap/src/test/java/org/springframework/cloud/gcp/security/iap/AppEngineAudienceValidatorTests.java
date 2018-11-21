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
import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Elena Felder
 */
@RunWith(MockitoJUnitRunner.class)
public class AppEngineAudienceValidatorTests {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	GcpProjectIdProvider mockProjectIdProvider;

	@Mock
	ResourceManager mockResourceManager;

	@Mock
	Project mockProject;

	@Mock
	Jwt mockJwt;

	@Test
	public void testNullProjectIdProviderDisallowed() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("GcpProjectIdProvider cannot be null.");

		new AppEngineAudienceValidator(null, this.mockResourceManager);
	}

	@Test
	public void testNullResourceManagerDisallowed() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ResourceManager cannot be null.");

		new AppEngineAudienceValidator(this.mockProjectIdProvider, null);
	}

	@Test
	public void testCorrectAudienceFormatSucceeds() {
		when(this.mockProjectIdProvider.getProjectId()).thenReturn("steal-spaceship");
		when(this.mockResourceManager.get("steal-spaceship")).thenReturn(this.mockProject);
		when(this.mockProject.getProjectNumber()).thenReturn(42L);
		when(this.mockJwt.getAudience()).thenReturn(ImmutableList.of("/projects/42/apps/steal-spaceship"));

		AppEngineAudienceValidator validator
				= new AppEngineAudienceValidator(this.mockProjectIdProvider, this.mockResourceManager);
		assertFalse(validator.validate(this.mockJwt).hasErrors());
	}

	@Test
	public void testIncorrectAudienceFormatFails() {
		when(this.mockProjectIdProvider.getProjectId()).thenReturn("steal-spaceship");
		when(this.mockResourceManager.get("steal-spaceship")).thenReturn(this.mockProject);
		when(this.mockProject.getProjectNumber()).thenReturn(42L);
		when(this.mockJwt.getAudience()).thenReturn(ImmutableList.of("/projects/1/apps/magrathea"));

		AppEngineAudienceValidator validator
				= new AppEngineAudienceValidator(this.mockProjectIdProvider, this.mockResourceManager);
		OAuth2TokenValidatorResult result = validator.validate(this.mockJwt);
		assertTrue(result.hasErrors());
		assertThat(result.getErrors().size()).isEqualTo(1);
		assertThat(result.getErrors().stream().findFirst().get().getDescription())
				.startsWith("This aud claim is not equal");
	}
}
