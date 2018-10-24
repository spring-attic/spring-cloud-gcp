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

import java.util.Collections;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class IapAuthentication extends AbstractAuthenticationToken {

	public static final String DEFAULT_ROLE = "ROLE_USER";

	private final String email;

	private final String subject;

	private final String jwtToken;

	public IapAuthentication(String email, String subject, String jwtToken) {
		super(Collections.singletonList(new SimpleGrantedAuthority(DEFAULT_ROLE)));
		this.email = email;
		this.subject = subject;
		this.jwtToken = jwtToken;
	}

	@Override
	public Object getCredentials() {
		return this.jwtToken;
	}

	@Override
	public Object getPrincipal() {
		return email;
	}

	@Override
	public boolean isAuthenticated() {
		return email != null && !email.equals("");
	}
}
