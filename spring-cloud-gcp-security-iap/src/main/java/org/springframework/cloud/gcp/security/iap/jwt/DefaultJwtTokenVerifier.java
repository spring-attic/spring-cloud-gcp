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

package org.springframework.cloud.gcp.security.iap.jwt;

import java.net.URL;
import java.time.Clock;

import com.google.common.collect.ImmutableList;

import org.springframework.cloud.gcp.security.iap.claims.IssueTimeInPastClaimVerifier;
import org.springframework.cloud.gcp.security.iap.claims.IssuerClaimVerifier;
import org.springframework.cloud.gcp.security.iap.claims.RequiredFieldsClaimVerifier;

// TODO: should this even exist? Or is it enough to have autoconfiguration do the same thing, with the assumption
// that anyone who does not use autoconfig is okay wiring up their own?
public class DefaultJwtTokenVerifier extends JwtTokenVerifier {

	public DefaultJwtTokenVerifier(URL jwkRegistryUrl) {
		super(
				new JwtSignatureVerifier(jwkRegistryUrl),
				ImmutableList.of(
					new RequiredFieldsClaimVerifier(),
					new IssueTimeInPastClaimVerifier(Clock.systemUTC()),
					// TODO: uncomment; commented out for local testing
					//new ExpirationTimeInFutureClaimVerifier(Clock.systemUTC())
					new IssuerClaimVerifier()
				)
		);
	}
}
