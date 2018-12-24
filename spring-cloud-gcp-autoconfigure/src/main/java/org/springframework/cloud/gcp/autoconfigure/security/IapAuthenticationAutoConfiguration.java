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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.core.environment.ConditionalOnGcpEnvironment;
import org.springframework.cloud.gcp.core.GcpEnvironment;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.security.iap.AppEngineAudienceProvider;
import org.springframework.cloud.gcp.security.iap.AudienceProvider;
import org.springframework.cloud.gcp.security.iap.AudienceValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoderJwkSupport;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 * Autoconfiguration for extracting pre-authenticated user identity from
 * <a href="https://cloud.google.com/iap/">Google Cloud IAP</a> header.
 *
 * <p>Provides:
 * <ul>
 *   <li>a custom {@link BearerTokenResolver} extracting identity from {@code x-goog-iap-jwt-assertion} header
 *   <li>an ES256 web registry-based JWT token decoder bean with the following standard validations:
 *     <ul>
 *         <li>Issue time
 *         <li>Expiration time
 *         <li>Issuer
 *         <li>Audience (this validation is only enabled if running on AppEngine, or if a custom
 *         audience is provided through {@code spring.cloud.gcp.security.iap.audience} property)
 *     </ul>
 * </ul>
 * <p>If a custom {@link WebSecurityConfigurerAdapter} is present, it must add {@code .oauth2ResourceServer().jwt()}
 * customization to {@link org.springframework.security.config.annotation.web.builders.HttpSecurity} object. If no
 * custom {@link WebSecurityConfigurerAdapter} is found,
 * Spring Boot's default {@code OAuth2ResourceServerWebSecurityConfiguration} will add this customization.
 *
 * @author Elena Felder
 *
 * @since 1.1
 */
@Configuration
@ConditionalOnProperty(value = "spring.cloud.gcp.security.iap.enabled", matchIfMissing = true)
@ConditionalOnClass({AudienceValidator.class})
@AutoConfigureBefore(OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureAfter(GcpContextAutoConfiguration.class)
@EnableConfigurationProperties(IapAuthenticationProperties.class)
public class IapAuthenticationAutoConfiguration {

	private static final Log LOGGER = LogFactory.getLog(IapAuthenticationAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	public BearerTokenResolver iatTokenResolver(IapAuthenticationProperties properties) {
		return (r) -> r.getHeader(properties.getHeader());
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("spring.cloud.gcp.security.iap.audience")
	public AudienceProvider propertyBasedAudienceProvider(IapAuthenticationProperties properties) {
		return properties::getAudience;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnGcpEnvironment({GcpEnvironment.APP_ENGINE_FLEXIBLE, GcpEnvironment.APP_ENGINE_STANDARD})
	public AudienceProvider appEngineBasedAudienceProvider(GcpProjectIdProvider projectIdProvider) {
		return new AppEngineAudienceProvider(projectIdProvider);
	}

	@Bean
	@ConditionalOnMissingBean
	public AudienceValidator audienceValidator(AudienceProvider audienceProvider) {
		return new AudienceValidator(audienceProvider);
	}

	@Bean
	@ConditionalOnMissingBean(name = "iapJwtDelegatingValidator")
	public DelegatingOAuth2TokenValidator<Jwt> iapJwtDelegatingValidator(IapAuthenticationProperties properties,
			AudienceValidator audienceValidator) {

		List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
		validators.add(new JwtTimestampValidator());
		validators.add(new JwtIssuerValidator(properties.getIssuer()));
		validators.add(audienceValidator);

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Audience configured for IAP JWT validation: " + audienceValidator.getAudience());
		}

		return new DelegatingOAuth2TokenValidator<>(validators);
	}

	@Bean
	@ConditionalOnMissingBean
	public JwtDecoder iapJwtDecoder(IapAuthenticationProperties properties,
			@Qualifier("iapJwtDelegatingValidator") DelegatingOAuth2TokenValidator<Jwt> validator) {

		NimbusJwtDecoderJwkSupport jwkSupport
				= new NimbusJwtDecoderJwkSupport(properties.getRegistry(), properties.getAlgorithm());
		jwkSupport.setJwtValidator(validator);

		return jwkSupport;
	}
}
