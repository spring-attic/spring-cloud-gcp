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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gcp.security.iap.IapAuthenticationDetailsSource;
import org.springframework.cloud.gcp.security.iap.IapAuthenticationFilter;
import org.springframework.cloud.gcp.security.iap.jwt.JwtTokenVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesUserDetailsService;

@Configuration
@ConditionalOnProperty(value = IapSecurityConstants.IAP_GATING_PROPERTY, matchIfMissing = true)
@ConditionalOnClass({ IapAuthenticationFilter.class, WebSecurityConfigurerAdapter.class })
public class IapAuthenticationAutoConfiguration {

	// todo: externalize as properties?
	private static final String PUBLIC_KEY_VERIFICATION_LINK = "https://www.gstatic.com/iap/verify/public_key-jwk";

	// TODO: differentiate based on either AppEngine or ComputeEngine

	@Bean
	@ConditionalOnMissingBean
	public JwtTokenVerifier jwtTokenVerifier() {

		URL registryUrl = null;
		try {
			registryUrl = new URL(PUBLIC_KEY_VERIFICATION_LINK);
		}
		catch (MalformedURLException e) {
			throw new BeanInstantiationException(JwtTokenVerifier.class, "Invalid JWK URL", e);
		}

		return new JwtTokenVerifier(registryUrl);
	}

	@Bean
	@ConditionalOnMissingBean
	public PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider(
			AuthenticationUserDetailsService userDetailsService) {
		PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
		provider.setPreAuthenticatedUserDetailsService(userDetailsService);

		return provider;
	}

	@Bean
	@ConditionalOnMissingBean
	public PreAuthenticatedGrantedAuthoritiesUserDetailsService preAuthenticatedUserDetailsService() {
		return new PreAuthenticatedGrantedAuthoritiesUserDetailsService();
	}

	@Bean
	@ConditionalOnMissingBean
	public ProviderManager preAuthenticatedAuthenticationManager(PreAuthenticatedAuthenticationProvider provider) {
		return new ProviderManager(Collections.singletonList(provider));
	}

	@Bean
	@ConditionalOnMissingBean
	public IapAuthenticationDetailsSource iapAuthenticationDetailsSource() {
		return new IapAuthenticationDetailsSource();
	}

	@Bean
	@ConditionalOnMissingBean
	public IapAuthenticationFilter iapAuthenticationFilter(JwtTokenVerifier tokenVerifier,
			ProviderManager authenticationManager, AuthenticationDetailsSource detailsSource) {
		return new IapAuthenticationFilter(tokenVerifier, authenticationManager, detailsSource);
	}
}
