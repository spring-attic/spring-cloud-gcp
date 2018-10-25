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

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails;

public class IapAuthenticationDetailsSource implements AuthenticationDetailsSource<HttpServletRequest,
		PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails> {
	@Override
	public PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails buildDetails(HttpServletRequest request) {
		// todo: instanceof
		// todo: is there a way to not duplicate authorities creation?
		return new PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails(
				request,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
		//((IapAuthentication)SecurityContextHolder.getContext().getAuthentication()).getGrantedAuthorities()
	}
}
