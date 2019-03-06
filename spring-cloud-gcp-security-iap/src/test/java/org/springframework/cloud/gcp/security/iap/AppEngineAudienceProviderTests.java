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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests for the AppEngine Audience Provider.
 *
 * @author Elena Felder
 */
@RunWith(MockitoJUnitRunner.class)
public class AppEngineAudienceProviderTests {

	@Mock
	GcpProjectIdProvider mockProjectIdProvider;

	@Mock
	ResourceManager mockResourceManager;

	@Mock
	Project mockProject;

	@Before
	public void setUp() {
		when(this.mockProjectIdProvider.getProjectId()).thenReturn("steal-spaceship");
	}

	@Test
	public void testNullProjectIdProviderDisallowed() {

		assertThatThrownBy(() -> {
			new AppEngineAudienceProvider(null);
		})
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("GcpProjectIdProvider cannot be null.");
	}

	@Test
	public void testNullResourceManagerDisallowed() {

		assertThatThrownBy(() -> {
			new AppEngineAudienceProvider(this.mockProjectIdProvider).setResourceManager(null);
		})
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("ResourceManager cannot be null.");
	}

	@Test
	public void testNullProjectDisallowed() {
		assertThatThrownBy(() -> {
			AppEngineAudienceProvider provider = new AppEngineAudienceProvider(this.mockProjectIdProvider);
			provider.setResourceManager(this.mockResourceManager);
			provider.getAudience();
		})
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageStartingWith("Project expected not to be null. Is Cloud Resource Manager API enabled");

	}

	@Test
	public void testNullProjectNumberDisallowed() {
		when(mockProjectIdProvider.getProjectId()).thenReturn("steal-spaceship");
		when(this.mockResourceManager.get("steal-spaceship")).thenReturn(this.mockProject);
		when(this.mockProject.getProjectNumber()).thenReturn(null);

		assertThatThrownBy(() -> {
			AppEngineAudienceProvider provider = new AppEngineAudienceProvider(this.mockProjectIdProvider);
			provider.setResourceManager(this.mockResourceManager);
			provider.getAudience();
		})
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Project Number expected not to be null.");
	}

	@Test
	public void testNullProjectIdDisallowed() {
		when(mockProjectIdProvider.getProjectId()).thenReturn(null);
		when(this.mockResourceManager.get(null)).thenReturn(this.mockProject);
		when(this.mockProject.getProjectNumber()).thenReturn(42L);

		assertThatThrownBy(() -> {
			AppEngineAudienceProvider provider = new AppEngineAudienceProvider(this.mockProjectIdProvider);
			provider.setResourceManager(this.mockResourceManager);
			provider.getAudience();
		})
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Project Id expected not to be null.");
	}


	@Test
	public void testAudienceFormatCorrect() {
		when(this.mockResourceManager.get("steal-spaceship")).thenReturn(this.mockProject);
		when(this.mockProject.getProjectNumber()).thenReturn(42L);

		AppEngineAudienceProvider provider = new AppEngineAudienceProvider(this.mockProjectIdProvider);
		provider.setResourceManager(this.mockResourceManager);

		assertThat(provider.getAudience()).isEqualTo("/projects/42/apps/steal-spaceship");
	}
}
