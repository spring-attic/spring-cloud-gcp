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

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoderJwkSupport;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 * Autoconfiguration for extracting pre-authenticated user identity from Google Cloud IAP header.
 *
 * <p>The following conditions must be present for this configuration to take effect:
 * <ul>
 *   <li>{@code spring.cloud.gcp.security.iap.enabled} property is true. Note that a missing property is treated as
 *   {@code false}. Because there is no custom code beyond what Spring Security OAuth provides, no dependency can be
 *   used as a consistent signal for using IAP authentication, making an explicit property necessary.
 *   <li>Spring Security OAuth Resource Server 5.0 or higher is present on the classpath.
 * </ul>
 * <p>If these conditions are met, a custom {@link BearerTokenResolver} and an ES256 web registry-based JWT token
 * decoder beans are provided.
 * <p>If a custom {@link WebSecurityConfigurerAdapter} is present, it must add {@code .oauth2ResourceServer().jwt()}
 * customization to {@link org.springframework.security.config.annotation.web.builders.HttpSecurity} object. If no
 * custom {@link WebSecurityConfigurerAdapter} is found,
 * Spring Boot's default {@code OAuth2ResourceServerWebSecurityConfiguration} will add this customization.
 *
 * @author Elena Felder
 * @since 1.1
 */
@Configuration
@ConditionalOnProperty("spring.cloud.gcp.security.iap.enabled")
@ConditionalOnClass({NimbusJwtDecoderJwkSupport.class})
@AutoConfigureBefore(OAuth2ResourceServerAutoConfiguration.class)
@EnableConfigurationProperties(IapAuthenticationProperties.class)
public class IapAuthenticationAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public JwtDecoder iapJwtDecoder(IapAuthenticationProperties properties) {
		return new NimbusJwtDecoderJwkSupport(properties.getRegistry(), properties.getAlgorithm());
	}

	@Bean
	@ConditionalOnMissingBean
	public BearerTokenResolver iatTokenResolver(IapAuthenticationProperties properties) {
		return r -> r.getHeader(properties.getHeader());
	}
}
