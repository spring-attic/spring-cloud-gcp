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

package org.springframework.cloud.gcp.autoconfigure.security;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoderJwkSupport;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;


/**
 * @author Elena Felder
 * @since 1.1
 */
@RunWith(MockitoJUnitRunner.class)
public class IapAuthenticationAutoConfigurationTests {

	static final String FAKE_USER_TOKEN = "lol cats forever";

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(IapAuthenticationAutoConfiguration.class));

	@Mock
	HttpServletRequest mockIapRequest;

	@Mock
	HttpServletRequest mockNonIapRequest;

	@Mock
	static Jwt mockJwt;

	@Before
	public void httpRequestSetup() {
		when(this.mockIapRequest.getHeader("x-goog-iap-jwt-assertion")).thenReturn("very fake jwt");
	}

	@Test
	public void testIapAutoconfiguredBeansExistInContext() {
		this.contextRunner.withPropertyValues("spring.cloud.gcp.security.iap.enabled=true")
				.run(this::verifyJwtBeans);
	}

	@Test (expected = NoSuchBeanDefinitionException.class)
	public void testAutoconfiguredBeansMissingWhenGatingPropertyFalse() {
		this.contextRunner
				.withPropertyValues("spring.cloud.gcp.security.iap.enabled=false")
				.run(context ->	context.getBean(JwtDecoder.class));
	}

	@Test (expected = NoSuchBeanDefinitionException.class)
	public void testAutoconfiguredBeansMissingWhenGatingPropertyMissing() {
		this.contextRunner
				.run(context -> context.getBean(JwtDecoder.class));
	}

	@Test
	public void testIapBeansReturnedWhenBothIapAndSpringSecurityConfigPresent() {
		new ApplicationContextRunner()
				.withPropertyValues("spring.cloud.gcp.security.iap.enabled=true")
				.withConfiguration(
						AutoConfigurations.of(IapAuthenticationAutoConfiguration.class,
						OAuth2ResourceServerAutoConfiguration.class))
				.run(this::verifyJwtBeans);
	}

	@Test
	public void testUserBeansReturnedUserConfigPresent() {
		this.contextRunner
				.withPropertyValues("spring.cloud.gcp.security.iap.enabled=true")
				.withUserConfiguration(UserConfiguration.class)
				.run(context -> {
					JwtDecoder jwtDecoder =  context.getBean(JwtDecoder.class);
					assertThat(jwtDecoder).isNotNull();
					assertFalse(jwtDecoder instanceof NimbusJwtDecoderJwkSupport);
					assertThat(jwtDecoder.decode("Ceci n'est pas un Jwt")).isSameAs(mockJwt);

					BearerTokenResolver resolver = context.getBean(BearerTokenResolver.class);
					assertThat(resolver).isNotNull();
					assertThat(resolver.resolve(this.mockIapRequest)).isEqualTo(FAKE_USER_TOKEN);
					assertThat(resolver.resolve(this.mockNonIapRequest)).isEqualTo(FAKE_USER_TOKEN);
				});
	}

	@Test
	public void testCustomPropertyOverridesDefault() {
		this.contextRunner
				.withPropertyValues(
						"spring.cloud.gcp.security.iap.enabled=true",
						"spring.cloud.gcp.security.iap.header=some-other-header")
				.run(context -> {
					when(this.mockNonIapRequest.getHeader("some-other-header")).thenReturn("other header jwt");

					BearerTokenResolver resolver = context.getBean(BearerTokenResolver.class);
					assertThat(resolver).isNotNull();
					assertThat(resolver.resolve(this.mockIapRequest)).isEqualTo(null);
					assertThat(resolver.resolve(this.mockNonIapRequest)).isEqualTo("other header jwt");
				});
	}

	private void verifyJwtBeans(AssertableApplicationContext context) {
		JwtDecoder jwtDecoder =  context.getBean(JwtDecoder.class);
		assertThat(jwtDecoder).isNotNull();
		assertTrue(jwtDecoder instanceof NimbusJwtDecoderJwkSupport);

		BearerTokenResolver resolver = context.getBean(BearerTokenResolver.class);
		assertThat(resolver).isNotNull();
		assertThat(resolver.resolve(this.mockIapRequest)).isEqualTo("very fake jwt");

		assertThat(resolver.resolve(this.mockNonIapRequest)).isNull();
	}

	@Configuration
	static class UserConfiguration {
		@Bean
		public JwtDecoder jwtDecoder() {
			return s -> mockJwt;
		}

		@Bean
		public BearerTokenResolver bearerTokenResolver() {
			return httpServletRequest -> FAKE_USER_TOKEN;
		}
	}
}
