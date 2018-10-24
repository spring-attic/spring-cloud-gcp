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

import org.springframework.cloud.gcp.security.iap.claims.IapJwtClaimsSetVerifier;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkTokenStore;

/** Verify IAP authorization JWT token in incoming request.
 *
 * JwkTokenStore verifies the signature through JwkVerifyingJwtAccessTokenConverter.
 */
public class JwtTokenVerifier {

	private static final String PUBLIC_KEY_VERIFICATION_URL =
			"https://www.gstatic.com/iap/verify/public_key-jwk";

	private JwkTokenStore tokenStore = new JwkTokenStore(PUBLIC_KEY_VERIFICATION_URL, new IapJwtClaimsSetVerifier());

	public IapAuthentication verifyAndExtractPrincipal(String jwtToken, String expectedAudience) {

		// reads and validates
		OAuth2AccessToken token = tokenStore.readAccessToken(jwtToken);

		IapAuthentication iapAuth
				= new IapAuthentication((String) token.getAdditionalInformation().get("email"), null, jwtToken);
		return iapAuth;
	}
}
