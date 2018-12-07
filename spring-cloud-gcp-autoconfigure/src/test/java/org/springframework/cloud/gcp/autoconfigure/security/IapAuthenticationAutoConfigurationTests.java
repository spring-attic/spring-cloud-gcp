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

package org.springframework.cloud.gcp.autoconfigure.security;

import java.time.Instant;

import javax.servlet.http.HttpServletRequest;

import com.google.cloud.resourcemanager.Project;
import com.google.cloud.resourcemanager.ResourceManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.core.GcpEnvironment;
import org.springframework.cloud.gcp.core.GcpEnvironmentProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.MetadataProvider;
import org.springframework.cloud.gcp.security.iap.AppEngineAudienceProvider;
import org.springframework.cloud.gcp.security.iap.AudienceProvider;
import org.springframework.cloud.gcp.security.iap.AudienceValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
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
 *
 * @since 1.1
 */
@RunWith(MockitoJUnitRunner.class)
public class IapAuthenticationAutoConfigurationTests {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	static final String FAKE_USER_TOKEN = "lol cats forever";

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(IapAuthenticationAutoConfiguration.class, TestConfiguration.class));

	@Mock
	HttpServletRequest mockIapRequest;

	@Mock
	HttpServletRequest mockNonIapRequest;

	@Mock
	static Jwt mockJwt;

	@Mock
	static GcpProjectIdProvider mockProjectIdProvider;

	@Mock
	static GcpEnvironmentProvider mockEnvironmentProvider;

	@Mock
	static ResourceManager mockResourceManager;

	@Mock
	static Project mockProject;

	@Mock
	static MetadataProvider mockMetadataProvider;

	@Before
	public void httpRequestSetup() {
		when(this.mockIapRequest.getHeader("x-goog-iap-jwt-assertion")).thenReturn("very fake jwt");
	}

	@Test
	public void testIapAutoconfiguredBeansExistInContext() {
		this.contextRunner
				.withPropertyValues("spring.cloud.gcp.security.iap.audience=unused")
				.run(this::verifyJwtBeans);
	}

	@Test
	public void testAutoconfiguredBeansMissingWhenGatingPropertyFalse() {

		this.expectedException.expect(NoSuchBeanDefinitionException.class);
		this.expectedException.expectMessage("No qualifying bean of type " +
				"'org.springframework.security.oauth2.jwt.JwtDecoder' available");

		this.contextRunner
				.withPropertyValues("spring.cloud.gcp.security.iap.enabled=false")
				.run(context ->	context.getBean(JwtDecoder.class));
	}

	@Test
	public void testIapBeansReturnedWhenBothIapAndSpringSecurityConfigPresent() {
		new ApplicationContextRunner()
				.withPropertyValues("spring.cloud.gcp.security.iap.audience=unused")
				.withConfiguration(
						AutoConfigurations.of(
								IapAuthenticationAutoConfiguration.class,
								OAuth2ResourceServerAutoConfiguration.class,
								TestConfiguration.class))
				.run(this::verifyJwtBeans);
	}

	@Test
	public void testUserBeansReturnedUserConfigPresent() {
		this.contextRunner
				.withUserConfiguration(UserConfiguration.class)
				.withPropertyValues("spring.cloud.gcp.security.iap.audience=unused")
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
				.withPropertyValues("spring.cloud.gcp.security.iap.header=some-other-header")
				.withPropertyValues("spring.cloud.gcp.security.iap.audience=unused")
				.run(context -> {
					when(this.mockNonIapRequest.getHeader("some-other-header")).thenReturn("other header jwt");

					BearerTokenResolver resolver = context.getBean(BearerTokenResolver.class);
					assertThat(resolver).isNotNull();
					assertThat(resolver.resolve(this.mockIapRequest)).isEqualTo(null);
					assertThat(resolver.resolve(this.mockNonIapRequest)).isEqualTo("other header jwt");
				});
	}

	@Test
	public void testContextFailsWhenAudienceValidatorNotAvailable() throws Exception {

		this.contextRunner
				.run(context -> {
					assertThat(context).getFailure()
							.hasCauseInstanceOf(NoSuchBeanDefinitionException.class)
							.hasMessageContaining("No qualifying bean of type 'org.springframework.cloud.gcp.security.iap.AudienceProvider'");
				});
	}

	@Test
	public void testFixedStringAudienceValidatorAddedWhenAvailable() throws Exception {
		when(mockJwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(10));
		when(mockJwt.getNotBefore()).thenReturn(Instant.now().minusSeconds(10));
		when(mockJwt.getClaimAsString("iss")).thenReturn("https://cloud.google.com/iap");

		this.contextRunner
				.withUserConfiguration(FixedAudienceValidatorConfiguration.class)
				.run(context -> {
					DelegatingOAuth2TokenValidator validator
							= context.getBean("iapJwtDelegatingValidator", DelegatingOAuth2TokenValidator.class);
					OAuth2TokenValidatorResult result = validator.validate(mockJwt);
					assertTrue(result.hasErrors());
					assertThat(result.getErrors().size()).isEqualTo(1);
					assertThat(
							result.getErrors().stream().findAny().get().getDescription())
								.startsWith("This aud claim is not equal");
				});
	}

	@Test
	public void testAppEngineAudienceValidatorAddedWhenAvailable() {
		when(this.mockEnvironmentProvider.getCurrentEnvironment()).thenReturn(GcpEnvironment.APP_ENGINE_FLEXIBLE);

		this.contextRunner
				.withUserConfiguration(FixedAudienceValidatorConfiguration.class)
				.run(context -> {
					AudienceProvider audienceProvider = context.getBean(AudienceProvider.class);
					assertThat(audienceProvider).isNotNull();
					assertThat(audienceProvider).isInstanceOf(AppEngineAudienceProvider.class);
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

	@Configuration
	@AutoConfigureBefore(IapAuthenticationAutoConfiguration.class)
	static class TestConfiguration {

		@Bean
		static GcpProjectIdProvider mockProjectIdProvider() {
			return mockProjectIdProvider;
		}

		@Bean
		static GcpEnvironmentProvider mockEnvironmentProvider() {
			return mockEnvironmentProvider;
		}
	}

	@Configuration
	@AutoConfigureBefore(IapAuthenticationAutoConfiguration.class)
	static class FixedAudienceValidatorConfiguration {

		@Bean
		AudienceValidator audienceValidator() {
			return new AudienceValidator(() -> "right audience");
		}
	}

}
