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

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.proc.SimpleSecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.JWTClaimsSetVerifier;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.gcp.security.iap.IapAuthentication;
import org.springframework.cloud.gcp.security.iap.claims.CompositeJwtClaimsSetVerifier;
import org.springframework.cloud.gcp.security.iap.claims.IssuerJwtClaimsSetVerifier;
import org.springframework.cloud.gcp.security.iap.claims.RequiredFieldsJwtClaimsSetVerifier;

/**
 * Verify IAP authorization JWT token in incoming request.
 *
 * JwkTokenStore verifies the signature through JwkVerifyingJwtAccessTokenConverter.
 */
public class JwtTokenVerifier {

	private static final Log LOGGER = LogFactory.getLog(JwtTokenVerifier.class);

	private final JWTClaimsSetVerifier<? extends SecurityContext> jwtClaimsSetVerifier;

	private final JwtSignatureVerifier jwtSignatureVerifier;

	public JwtTokenVerifier(JwtSignatureVerifier jwtSignatureVerifier,
							JWTClaimsSetVerifier<? extends SecurityContext> jwtClaimsSetVerifier) {
		this.jwtSignatureVerifier = jwtSignatureVerifier;
		this.jwtClaimsSetVerifier = jwtClaimsSetVerifier;
	}

	/**
	 * Builds up a default verifier assuming ECDSA signed key, plus common-sense
	 * org.springframework.cloud.gcp.security.iap.claims checks.
	 */
	public JwtTokenVerifier(URL jwkRegistryUrl) {
		this(new JwtECSignatureVerifier(jwkRegistryUrl),
				new CompositeJwtClaimsSetVerifier(
						new DefaultJWTClaimsVerifier<SimpleSecurityContext>(),
						new RequiredFieldsJwtClaimsSetVerifier(),
						new IssuerJwtClaimsSetVerifier()));
	}

	/**
	 * Returns IapAuthentication principal object if token verification succeeds.
	 * Returns null if any signature or claim verification fails.
	 */
	public IapAuthentication verifyAndExtractPrincipal(String jwtToken) {
		if (jwtToken == null) {
			LOGGER.warn("JWT token verification requested, yet no token passed in; not authenticating.");
			return null;
		}

		SignedJWT signedJwt = extractSignedToken(jwtToken);

		if (signedJwt == null || !this.jwtSignatureVerifier.validateJwt(signedJwt)) {
			LOGGER.info("Jwt public key verification failed; not authenticating.");
			return null;
		}

		JWTClaimsSet claims = extractClaims(signedJwt);
		return validateClaims(claims) ? extractPrincipal(claims, jwtToken) : null;
	}

	private IapAuthentication extractPrincipal(JWTClaimsSet claims, String jwtToken) {
		try {
			String email = claims.getStringClaim("email");
			return new IapAuthentication(email, claims.getSubject(), jwtToken);
		}
		catch (ParseException e) {
			LOGGER.warn("String value could not be parsed from org.springframework.cloud.gcp.security.iap.claims.", e);
			return null;
		}
	}

	private boolean validateClaims(JWTClaimsSet claims) {
		if (claims == null) {
			LOGGER.warn("Null claims; cannot validate.");
			return false;
		}

		try {
			this.jwtClaimsSetVerifier.verify(claims, null);
		}
		catch (BadJWTException e) {
			LOGGER.info("Claim verification failed.");
			return false;
		}

		return true;
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
