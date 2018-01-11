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

package org.springframework.cloud.gcp.autoconfigure.core;

import java.util.List;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpScope;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author João André Martins
 */
@Configuration
public class GcpContextAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testGetProjectIdProvider_withGcpProperties() {
		loadEnvironment("spring.cloud.gcp.projectId=test-project");
		GcpProjectIdProvider provider = this.context.getBean(GcpProjectIdProvider.class);

		assertEquals("test-project", provider.getProjectId());
	}

	@Test
	public void testGetProjectIdProvider_withoutGcpProperties() {
		loadEnvironment();
		assertTrue(this.context.getBean(GcpProjectIdProvider.class)
				instanceof DefaultGcpProjectIdProvider);
	}

	@Test
	public void testResolveScopesDefaultScopes() {
		loadEnvironment();
		GcpContextAutoConfiguration configuration = this.context.getBean(GcpContextAutoConfiguration.class);
		List<String> scopes = configuration.resolveScopes();
		assertTrue(scopes.size() > 1);
		assertTrue(scopes.contains(GcpScope.PUBSUB.getUrl()));
	}

	@Test
	public void testResolveScopesOverrideScopes() {
		loadEnvironment("spring.cloud.gcp.credentials.scopes=myscope");
		GcpContextAutoConfiguration configuration = this.context.getBean(GcpContextAutoConfiguration.class);
		List<String> scopes = configuration.resolveScopes();
		assertEquals(scopes.size(), 1);
		assertTrue(scopes.contains("myscope"));
	}

	@Test
	public void testResolveScopesStarterScopesPlaceholder() {
		loadEnvironment("spring.cloud.gcp.credentials.scopes=DEFAULT_SCOPES,myscope");
		GcpContextAutoConfiguration configuration = this.context.getBean(GcpContextAutoConfiguration.class);
		List<String> scopes = configuration.resolveScopes();
		assertTrue(scopes.size() == GcpScope.values().length + 1);
		assertTrue(scopes.contains(GcpScope.PUBSUB.getUrl()));
		assertTrue(scopes.contains("myscope"));
	}


	private void loadEnvironment(String... environment) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(GcpContextAutoConfiguration.class);
		context.register(this.getClass());
		EnvironmentTestUtils.addEnvironment(context, environment);
		context.refresh();
		this.context = context;
	}

	@Bean
	public CredentialsProvider googleCredentials() {
		return () -> mock(Credentials.class);
	}
}
