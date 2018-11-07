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

package com.example;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;

/**
 * Sample custom {@link WebSecurityConfigurerAdapter} that applies OAuth Resource Server pre-authentication, and
 * rejects unauthenticated requests to a single page, {@code /topsecret}. All other pages are unsecured.
 *
 * Because of {@code spring-cloud-gcp-starter-security-iap} dependency, the secure token will be retrieved from Google
 * Cloud IAP header {@code x-goog-iap-jwt-assertion}, and not from the standard OAuth Bearer header.
 *
 * @author Elena Felder
 * @since 1.1
 */
@Configuration
@EnableWebSecurity
public class SecurityConfigurer extends WebSecurityConfigurerAdapter {
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
				.authorizeRequests().antMatchers("/topsecret").authenticated()
				.and()
				.oauth2ResourceServer()
					.jwt()
					.and()
					.authenticationEntryPoint(new Http403ForbiddenEntryPoint());
	}
}
