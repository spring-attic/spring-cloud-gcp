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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.MetadataProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Elena Felder
 */
@RunWith(MockitoJUnitRunner.class)
public class ComputeEngineAudienceSupplierTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	GcpProjectIdProvider mockProjectIdProvider;

	@Mock
	ResourceManager mockResourceManager;

	@Mock
	MetadataProvider mockMetadataProvider;

	@Mock
	Project mockProject;

	@Before
	public void setUp() {
		when(this.mockProjectIdProvider.getProjectId()).thenReturn("steal-spaceship");
		when(this.mockResourceManager.get("steal-spaceship")).thenReturn(this.mockProject);
		when(this.mockProject.getProjectNumber()).thenReturn(42L);
		when(this.mockMetadataProvider.getAttribute("id")).thenReturn("123");
	}

	@Test
	public void testNullProjectIdProviderDisallowed() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("GcpProjectIdProvider cannot be null.");

		new ComputeEngineAudienceSupplier(null, this.mockResourceManager, this.mockMetadataProvider);
	}

	@Test
	public void testNullResourceManagerDisallowed() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ResourceManager cannot be null.");

		new ComputeEngineAudienceSupplier(this.mockProjectIdProvider, null, this.mockMetadataProvider);
	}

	@Test
	public void testNullMetadataProviderDisallowed() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("MetadataProvider cannot be null.");

		new ComputeEngineAudienceSupplier(this.mockProjectIdProvider, this.mockResourceManager, null);
	}

	@Test
	public void correctPatternSucceeds() {

		ComputeEngineAudienceSupplier supplier
				= new ComputeEngineAudienceSupplier(
						this.mockProjectIdProvider, this.mockResourceManager, this.mockMetadataProvider);
		assertThat(supplier.get()).isEqualTo("/projects/42/global/backendServices/123");
	}

}
