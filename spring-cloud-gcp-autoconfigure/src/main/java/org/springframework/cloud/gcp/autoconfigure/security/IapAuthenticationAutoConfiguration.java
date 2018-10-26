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

import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gcp.security.iap.IapAuthenticationDetailsSource;
import org.springframework.cloud.gcp.security.iap.IapAuthenticationFilter;
import org.springframework.cloud.gcp.security.iap.JwtTokenVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesUserDetailsService;

@Configuration
@ConditionalOnProperty("spring.cloud.gcp.security.iap.enabled")
public class IapAuthenticationAutoConfiguration {
	private static final Log LOGGER = LogFactory.getLog(IapAuthenticationAutoConfiguration.class);

	// TODO: differentiate based on either AppEngine or ComputeEngine
	@Bean
	public JwtTokenVerifier jwtTokenVerifier() {
		LOGGER.info("IapAuthenticationAutoConfiguration verifier bean creation");
		return new JwtTokenVerifier();
	}

	@Bean
	public IapAuthenticationFilter iapAuthenticationFilter() {
		IapAuthenticationFilter filter = new IapAuthenticationFilter(jwtTokenVerifier());
		LOGGER.info("******************* INSTANTIATING IAP AUTH FILTER: " + filter);

		PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
		provider.setPreAuthenticatedUserDetailsService(new PreAuthenticatedGrantedAuthoritiesUserDetailsService());
		filter.setAuthenticationManager(new ProviderManager(Collections.singletonList(provider)));
		filter.setAuthenticationDetailsSource(new IapAuthenticationDetailsSource());
		return filter;
	}
}
