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

package org.springframework.cloud.gcp.security.iap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.gcp.security.iap.jwt.JwtTokenVerifier;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

public class IapAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {
	private static final Log LOGGER = LogFactory.getLog(IapAuthenticationFilter.class);

	public static final String HEADER_NAME = "x-goog-iap-jwt-assertion";

	private JwtTokenVerifier verifyIapRequestHeader;

	public IapAuthenticationFilter(JwtTokenVerifier verifyIapRequestHeader,
									AuthenticationManager authenticationManager,
									AuthenticationDetailsSource detailsSource) {
		this.verifyIapRequestHeader = verifyIapRequestHeader;
		setAuthenticationManager(authenticationManager);
		setAuthenticationDetailsSource(detailsSource);
	}

	@Override
	protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
		String assertion = request.getHeader(HEADER_NAME);
		Authentication authentication = null;

		if (assertion != null) {
			authentication = this.verifyIapRequestHeader.verifyAndExtractPrincipal(assertion);
		}

		// TODO: move actual authentication into user details source; return string principal from here
		LOGGER.info("IapAuthenticationFilter returning an authentication: " + authentication);

		return authentication;
	}

	@Override
	protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
		return request.getHeader("x-goog-iap-jwt-assertion");
	}
}
