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

import java.text.ParseException;
import java.util.List;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.gcp.security.iap.IapAuthentication;
import org.springframework.cloud.gcp.security.iap.claims.ClaimVerifier;

/**
 * Verify IAP authorization JWT token in incoming request.
 *
 * JwkTokenStore verifies the signature through JwkVerifyingJwtAccessTokenConverter.
 */
public class JwtTokenVerifier {

	private static final Log LOGGER = LogFactory.getLog(JwtTokenVerifier.class);

	private final List<ClaimVerifier> claimVerifiers;

	private final JwtSignatureVerifier jwtSignatureVerifier;

	public JwtTokenVerifier(JwtSignatureVerifier jwtSignatureVerifier, List<ClaimVerifier> claimVerifiers) {
		this.jwtSignatureVerifier = jwtSignatureVerifier;
		this.claimVerifiers = claimVerifiers;
	}

	public IapAuthentication verifyAndExtractPrincipal(String jwtToken, String expectedAudience) {
		if (jwtToken == null) {
			LOGGER.warn("Jwt token verification requested, yet no token passed in; not authenticating");
			return null;
		}

		IapAuthentication authentication = null;
		SignedJWT signedJwt = extractSignedToken(jwtToken);

		if (jwtSignatureVerifier.validateJwt(signedJwt)) {
			JWTClaimsSet claims = extractClaims(signedJwt);
			String email = extractClaimValue(claims, "email");

			if (validateClaims(claims) && email != null) {
				authentication = new IapAuthentication(email, claims.getSubject(), jwtToken);
			}
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

	private boolean validateClaims(JWTClaimsSet claims) {
		if (claims == null) {
			LOGGER.warn("Null claims cannot be validated.");
			return false;
		}

		for (ClaimVerifier verifier : this.claimVerifiers) {
			if (!verifier.verify(claims)) {
				return false;
			}
		}

		// claims must have audience, issuer
		// TODO: Vary expectec audience based on whether installed in AppEngine or ComputeEngine
		// Preconditions.checkArgument(claims.getAudience().contains(expectedAudience));

		return true;
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
