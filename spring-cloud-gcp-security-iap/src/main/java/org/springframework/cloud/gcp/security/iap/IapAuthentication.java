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

import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * Null authorities are passed to the superclass, as there are no inherent authorities bestowed by IAP
 * pre-authentication. The roles are instead determined in a provided implementation of
 * {@link org.springframework.security.authentication.AuthenticationDetailsSource}
 *
 * TODO: enhance this object with additional org.springframework.cloud.gcp.security.iap.claims available in the JWT
 * token.
 */
public final class IapAuthentication extends PreAuthenticatedAuthenticationToken {

	private final String subject;

	public IapAuthentication(String email, String subject, String jwtToken) {
		super(email, jwtToken);
		this.subject = subject;
	}

	public String getSubject() {
		return this.subject;
	}
}
