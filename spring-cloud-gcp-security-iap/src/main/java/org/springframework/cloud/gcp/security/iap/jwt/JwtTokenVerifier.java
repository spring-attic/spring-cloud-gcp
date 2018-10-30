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
import java.text.ParseException;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.gcp.security.iap.IapAuthentication;
import org.springframework.cloud.gcp.security.iap.claims.ClaimVerifier;
import org.springframework.cloud.gcp.security.iap.claims.CompositeClaimVerifier;
import org.springframework.cloud.gcp.security.iap.claims.IssueTimeInPastClaimVerifier;
import org.springframework.cloud.gcp.security.iap.claims.IssuerClaimVerifier;
import org.springframework.cloud.gcp.security.iap.claims.RequiredFieldsClaimVerifier;

/**
 * Verify IAP authorization JWT token in incoming request.
 *
 * JwkTokenStore verifies the signature through JwkVerifyingJwtAccessTokenConverter.
 */
public class JwtTokenVerifier {

	private static final Log LOGGER = LogFactory.getLog(JwtTokenVerifier.class);

	private final ClaimVerifier claimVerifier;

	private final JwtSignatureVerifier jwtSignatureVerifier;

	public JwtTokenVerifier(JwtSignatureVerifier jwtSignatureVerifier, ClaimVerifier claimVerifier) {
		this.jwtSignatureVerifier = jwtSignatureVerifier;
		this.claimVerifier = claimVerifier;
	}

	public JwtTokenVerifier(URL jwkRegistryUrl) {
		this(new JwtSignatureVerifier(jwkRegistryUrl),
				new CompositeClaimVerifier(
						new RequiredFieldsClaimVerifier(),
						new IssueTimeInPastClaimVerifier(),
						// TODO: uncomment; commented out for local testing
						// new ExpirationTimeInFutureClaimVerifier()
						new IssuerClaimVerifier()));
	}

	public IapAuthentication verifyAndExtractPrincipal(String jwtToken) {
		if (jwtToken == null) {
			LOGGER.warn("Jwt token verification requested, yet no token passed in; not authenticating");
			return null;
		}

		IapAuthentication authentication = null;
		SignedJWT signedJwt = extractSignedToken(jwtToken);

		if (jwtSignatureVerifier.validateJwt(signedJwt)) {
			JWTClaimsSet claims = extractClaims(signedJwt);
			String email = extractClaimValue(claims, "email");

			LOGGER.info("******************* about to verify: " + this.claimVerifier);

			if (claims != null && email != null && this.claimVerifier.verify(claims)) {
				authentication = new IapAuthentication(email, claims.getSubject(), jwtToken);
			}
			LOGGER.info("******************* after verifying: " + this.claimVerifier);

		}
		else {
			LOGGER.warn("Jwt public key verification failed; not authenticating");
		}

		return authentication;
	}

	private String extractClaimValue(JWTClaimsSet claims, String propertyName) {
		try {
			return claims.getStringClaim(propertyName);
		}
		catch (ParseException e) {
			LOGGER.warn("String value could not be parsed from claims.", e);
			return null;
		}
	}

	private JWTClaimsSet extractClaims(SignedJWT signedJwt) {

		JWTClaimsSet claims = null;
		try {
			claims = signedJwt.getJWTClaimsSet();
		}
		catch (ParseException e) {
			LOGGER.warn("JWT Claims could not be parsed", e);
		}
		return claims;
	}

	private SignedJWT extractSignedToken(String jwtToken) {
		SignedJWT signedJwt = null;

		try {
			signedJwt = SignedJWT.parse(jwtToken);
		}
		catch (ParseException e) {
			LOGGER.error("JWT Token could not be parsed.", e);
		}

		return signedJwt;
	}
}
