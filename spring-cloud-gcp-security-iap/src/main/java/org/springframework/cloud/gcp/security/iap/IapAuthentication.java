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

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthoritiesContainer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

public final class IapAuthentication extends PreAuthenticatedAuthenticationToken
	implements GrantedAuthoritiesContainer {
	private static final Log LOGGER = LogFactory.getLog(IapAuthentication.class);

	public static final String DEFAULT_ROLE = "ROLE_USER";

	@Override
	public Collection<? extends GrantedAuthority> getGrantedAuthorities() {
		LOGGER.info("IapAuthentication getGrantedAuthorities() called; returning authorities container.");

		return super.getAuthorities();
	}

	private final String subject;

	public IapAuthentication(String email, String subject, String jwtToken) {
		super(email, jwtToken, Collections.singletonList(new SimpleGrantedAuthority(DEFAULT_ROLE)));
		this.subject = subject;
	}

	@Override
	public Object getDetails() {
		LOGGER.info("IapAuthentication getDetails() called; returning authorities container.");
		// TODO: figure out how to get details from parent container
		return new GrantedAuthoritiesContainer() {
			@Override
			public Collection<? extends GrantedAuthority> getGrantedAuthorities() {
				return IapAuthentication.this.getAuthorities();
			}
		};
	}
}
