/*
 *  Copyright 2017 original author or authors.
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

package org.springframework.cloud.gcp.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jisha Abubaker
 * @author João André Martins
 */
public class GoogleConfigPropertySourceLocatorTest {

	private GcpConfigPropertiesProvider gcpConfigProperties;
	private Map<String, Object> expectedProperties;
	private GoogleConfigPropertySourceLocator googleConfigPropertySourceLocator;
	private GcpProjectIdProvider projectIdProvider;
	private CredentialsProvider credentialsProvider;

	@Before
	public void setUp() {
		this.gcpConfigProperties = mock(GcpConfigPropertiesProvider.class);
		when(this.gcpConfigProperties.getName()).thenReturn("test");
		when(this.gcpConfigProperties.isEnabled()).thenReturn(true);
		org.springframework.cloud.gcp.core.Credentials configCredentials =
				mock(org.springframework.cloud.gcp.core.Credentials.class);
		when(this.gcpConfigProperties.getCredentials()).thenReturn(configCredentials);
		when(this.gcpConfigProperties.getProfile()).thenReturn("default");
		this.expectedProperties = new HashMap<>();
		this.expectedProperties.put("property-int", 10);
		this.expectedProperties.put("property-bool", true);
		this.projectIdProvider = () -> "projectid";
		this.credentialsProvider = () -> mock(Credentials.class);

	}

	@Test
	public void locateReturnsMapPropertySource() throws Exception {
		GoogleConfigEnvironment googleConfigEnvironment = mock(GoogleConfigEnvironment.class);
		when(googleConfigEnvironment.getConfig()).thenReturn(this.expectedProperties);
		this.googleConfigPropertySourceLocator = spy(new GoogleConfigPropertySourceLocator(
				this.projectIdProvider, this.credentialsProvider, this.gcpConfigProperties));
		doReturn(googleConfigEnvironment).when(this.googleConfigPropertySourceLocator).getRemoteEnvironment();
		PropertySource<?> propertySource = this.googleConfigPropertySourceLocator.locate(new StandardEnvironment());
		assertEquals(propertySource.getName(), "spring-cloud-gcp");
		assertEquals(propertySource.getProperty("property-int"), 10);
		assertEquals(propertySource.getProperty("property-bool"), true);
		assertEquals("projectid", this.googleConfigPropertySourceLocator.getProjectId());
	}

	@Test
	public void locateReturnsMapPropertySource_disabled() throws Exception {
		when(this.gcpConfigProperties.isEnabled()).thenReturn(false);
		GoogleConfigEnvironment googleConfigEnvironment = mock(GoogleConfigEnvironment.class);
		when(googleConfigEnvironment.getConfig()).thenReturn(this.expectedProperties);
		this.googleConfigPropertySourceLocator = spy(new GoogleConfigPropertySourceLocator(
				this.projectIdProvider, this.credentialsProvider, this.gcpConfigProperties));
		doReturn(googleConfigEnvironment).when(this.googleConfigPropertySourceLocator).getRemoteEnvironment();
		PropertySource<?> propertySource = this.googleConfigPropertySourceLocator.locate(new StandardEnvironment());
		assertEquals(propertySource.getName(), "spring-cloud-gcp");
		assertEquals(0, ((MapPropertySource) propertySource).getPropertyNames().length);
	}

	@Test
	public void disabledPropertySourceReturnsNull() throws Exception {
		when(this.gcpConfigProperties.isEnabled()).thenReturn(false);
		this.googleConfigPropertySourceLocator = spy(new GoogleConfigPropertySourceLocator(
				this.projectIdProvider, this.credentialsProvider, this.gcpConfigProperties));
		this.googleConfigPropertySourceLocator.locate(new StandardEnvironment());
		verify(this.googleConfigPropertySourceLocator, never()).getRemoteEnvironment();
	}

	@Test
	public void disabledPropertySourceAvoidChecks() throws IOException {
		when(this.gcpConfigProperties.isEnabled()).thenReturn(false);
		this.googleConfigPropertySourceLocator =
				spy(new GoogleConfigPropertySourceLocator(null, null, this.gcpConfigProperties));
		this.googleConfigPropertySourceLocator.locate(new StandardEnvironment());
		verify(this.googleConfigPropertySourceLocator, never()).getRemoteEnvironment();
	}

	@Test
	public void testProjectIdInConfigProperties() throws IOException {
		when(this.gcpConfigProperties.getProjectId()).thenReturn("pariah");
		this.googleConfigPropertySourceLocator = new GoogleConfigPropertySourceLocator(
				this.projectIdProvider, this.credentialsProvider, this.gcpConfigProperties
		);

		assertEquals("pariah", this.googleConfigPropertySourceLocator.getProjectId());
	}
}
