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

package org.springframework.cloud.gcp.security.iap.claims;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.nimbusds.jose.proc.SimpleSecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.JWTClaimsSetVerifier;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * JWT fields defined at
 *
 * @see <a href="https://tools.ietf.org/html/rfc7519#section-4.1">RFC7519 for JWT definition</a>
 * @see <a href="https://cloud.google.com/iap/docs/signed-headers-howto#verify_the_jwt_payload">Google Cloud JWT
 * structure</a>
 */
public class RequiredFieldsJwtClaimsSetVerifier implements JWTClaimsSetVerifier<SimpleSecurityContext> {
	private static final Log LOGGER = LogFactory.getLog(RequiredFieldsJwtClaimsSetVerifier.class);

	private final static Set<String> REQUIRED_CLAIMS = ImmutableSet.of("sub", "aud", "exp", "iat", "email");

	@Override
	public void verify(JWTClaimsSet claims, SimpleSecurityContext context) throws BadJWTException {

		boolean requiredFieldsPresent = true;

		for (String claimName : REQUIRED_CLAIMS) {
			if (claims.getClaim(claimName) == null) {
				throw new BadJWTException("Required claim missing: " + claimName);
			}
		}
	}
}
