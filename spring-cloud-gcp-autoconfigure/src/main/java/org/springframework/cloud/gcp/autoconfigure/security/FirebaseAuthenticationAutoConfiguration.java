/*
 * Copyright 2020-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.autoconfigure.security;


import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.security.firebase.FirebaseJwtTokenDecoder;
import org.springframework.cloud.gcp.security.firebase.FirebaseTokenValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Vinicius Carvalho
 * @since 1.2.2
 */
@Configuration
@ConditionalOnClass(FirebaseTokenValidator.class)
@ConditionalOnProperty(value = "spring.cloud.gcp.security.firebase.enabled", matchIfMissing = true)
@AutoConfigureBefore(OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureAfter(GcpContextAutoConfiguration.class)
@EnableConfigurationProperties(FirebaseAuthenticationProperties.class)
public class FirebaseAuthenticationAutoConfiguration {

	private static final String ISSUER_TEMPLATE = "https://securetoken.google.com/%s";

	private final String projectId;

	public FirebaseAuthenticationAutoConfiguration(GcpProjectIdProvider gcpProjectIdProvider, FirebaseAuthenticationProperties properties) {
		this.projectId = properties.getProjectId() != null ? properties.getProjectId() : gcpProjectIdProvider.getProjectId();
	}

	@Bean
	@ConditionalOnMissingBean(name = "firebaseJwtDelegatingValidator")
	public DelegatingOAuth2TokenValidator<Jwt> firebaseJwtDelegatingValidator(JwtIssuerValidator jwtIssuerValidator, GcpProjectIdProvider gcpProjectIdProvider) {
		List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
		validators.add(new JwtTimestampValidator());
		validators.add(jwtIssuerValidator);
		validators.add(new FirebaseTokenValidator(projectId));
		return new DelegatingOAuth2TokenValidator<>(validators);
	}

	@Bean
	@ConditionalOnMissingBean(name = "firebaseAuthenticationJwtDecoder")
	public JwtDecoder firebaseAuthenticationJwtDecoder(
			DelegatingOAuth2TokenValidator<Jwt> firebaseJwtDelegatingValidator,
			FirebaseAuthenticationProperties properties) {
		return new FirebaseJwtTokenDecoder(restOperations(), properties.getPublicKeysEndpoint(),
				firebaseJwtDelegatingValidator);
	}

	@Bean
	public JwtIssuerValidator jwtIssuerValidator(GcpProjectIdProvider gcpProjectIdProvider) {
		return new JwtIssuerValidator(String.format(ISSUER_TEMPLATE, projectId));
	}

	private RestOperations restOperations() {
		SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
		clientHttpRequestFactory.setConnectTimeout(5_000);
		clientHttpRequestFactory.setReadTimeout(2_000);
		return new RestTemplate(clientHttpRequestFactory);
	}
}
