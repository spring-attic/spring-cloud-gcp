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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Verify IAP authorization JWT token in incoming request.
 *
 * JwkTokenStore verifies the signature through JwkVerifyingJwtAccessTokenConverter.
 */
public class JwtTokenVerifier {

	private static final Log LOGGER = LogFactory.getLog(JwtTokenVerifier.class);

	// todo: externalize as properties?
	private static final String PUBLIC_KEY_VERIFICATION_LINK = "https://www.gstatic.com/iap/verify/public_key-jwk";

	// using a simple cache with no eviction for this sample
	private Map<String, JWK> keyCache = new HashMap<>();

	private static Clock clock = Clock.systemUTC();

	// Wait at least 60 seconds before cache can be redownloaded.
	private static final int MIN_MS_BEFORE_RETRY = 60000;

	private long lastJwkStoreDownloadTimestamp;

	private final URL publicKeyVerificationUrl;

	private static final String IAP_ISSUER_URL = "https://cloud.google.com/iap";

	public JwtTokenVerifier() throws MalformedURLException {
		this.publicKeyVerificationUrl = new URL(PUBLIC_KEY_VERIFICATION_LINK);
	}

	/*
	 * private JwkTokenStore tokenStore = new JwkTokenStore(PUBLIC_KEY_VERIFICATION_URL, new
	 * IapJwtClaimsSetVerifier());
	 *
	 * public IapAuthentication verifyAndExtractPrincipal(String jwtToken, String
	 * expectedAudience) {
	 *
	 * // reads and validates OAuth2AccessToken token = tokenStore.readAccessToken(jwtToken);
	 *
	 * IapAuthentication iapAuth = new IapAuthentication((String)
	 * token.getAdditionalInformation().get("email"), null, jwtToken); return iapAuth; }
	 */

	public IapAuthentication verifyAndExtractPrincipal(String jwtToken, String expectedAudience) {
		if (jwtToken == null) {
			LOGGER.warn("Jwt token verification requested, yet no token passed in; not authenticating");
			return null;
		}

		// parse signed token into header / claims
		SignedJWT signedJwt = extractSignedToken(jwtToken);
		if (!validateJwt(signedJwt)) {
			LOGGER.warn("Jwt public key verification failed; not authenticating");
			return null;
		}

		JWTClaimsSet claims = null;
		try {
			claims = signedJwt.getJWTClaimsSet();
			System.out.println("IAP_FILTER: got claims = " + claims);
		}
		catch (ParseException e) {
			// TODO: log properly
			System.err.println("JWT Claims could not be parsed");
			e.printStackTrace();
			return null;
		}
		// claims must have audience, issuer
		// TODO: Vary expectec audience based on whether installed in AppEngine or ComputeEngine
		// Preconditions.checkArgument(claims.getAudience().contains(expectedAudience));
		Preconditions.checkArgument(claims.getIssuer().equals(IAP_ISSUER_URL));
		System.out.println("IAP_FILTER: checked issuer URL = ");
		// claim must have issued at time in the past
		Date currentTime = Date.from(Instant.now(clock));
		Preconditions.checkArgument(claims.getIssueTime().before(currentTime));
		// claim must have expiration time in the future
		// Preconditions.checkArgument(claims.getExpirationTime().after(currentTime));
		System.out.println("IAP_FILTER: checked time");
		// must have subject, email
		Preconditions.checkNotNull(claims.getSubject());
		Preconditions.checkNotNull(claims.getClaim("email"));
		System.out.println("IAP_FILTER: subject and claim exist");
		// verify using public key : lookup with key id, algorithm name provided

		String email = null;
		try {
			email = claims.getStringClaim("email");
		}
		catch (ParseException e) {
			// TODO: log properly
			System.err.println("E-mail string could not be parsed from claims.");
			e.printStackTrace();
			return null;
		}
		System.out.println("IAP_FILTER: email = " + email);
		System.out.println("IAP_FILTER: subject = " + claims.getSubject());
		return new IapAuthentication(email, claims.getSubject(), jwtToken);
	}

	private ECPublicKey getPublicKey(String kid, String alg) {
		JWK jwk = keyCache.get(kid);
		if (jwk == null) {
			jwk = downloadJwkKeysIfCacheNotFresh(kid);
		}

		ECPublicKey ecPublicKey = null;

		if (jwk == null) {
			LOGGER.warn(String.format("JWK key [%s] not found.", kid));
		} else if (!jwk.getAlgorithm().getName().equals(alg)) {
			LOGGER.warn(String.format(
							"JWK key alorithm [%s] does not match expected algorithm [%s].", jwk.getAlgorithm(), alg));
		} else {
			try {
				ecPublicKey = ECKey.parse(jwk.toJSONString()).toECPublicKey();
			} catch (JOSEException | ParseException e) {
				LOGGER.warn("JWK Public key extraction failed.", e);
			}
		}

		return ecPublicKey;
	}

	private JWK downloadJwkKeysIfCacheNotFresh(String kid) {
		if (this.clock.millis() - this.lastJwkStoreDownloadTimestamp > MIN_MS_BEFORE_RETRY) {
			JWKSet jwkSet = null;
			try {
				LOGGER.info("Re-downloading JWK cache.");
				jwkSet = JWKSet.load(publicKeyVerificationUrl);
				this.lastJwkStoreDownloadTimestamp = this.clock.millis();
			} catch (IOException | ParseException e) {
				LOGGER.warn("Downloading JWK keys failed.", e);
				return null;
			}
			keyCache = jwkSet.getKeys().stream().collect(Collectors.toMap(key -> key.getKeyID(), Function.identity()));
		}
		return keyCache.get(kid);
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

	private boolean validateJwt(SignedJWT signedJwt) {
		JWSHeader jwsHeader = signedJwt.getHeader();
		ECPublicKey publicKey = null;

		if (jwsHeader.getAlgorithm() == null) {
			LOGGER.warn("JWT header algorithm null.");
		}
		else if (jwsHeader.getKeyID() == null) {
			LOGGER.warn("JWT key ID null.");
		}
		else {
			publicKey = getPublicKey(jwsHeader.getKeyID(), jwsHeader.getAlgorithm().getName());
			if (publicKey != null) {
				return verifyAgainstPublicKey(signedJwt, publicKey);
			}
		}

		return false;
	}

	private boolean verifyAgainstPublicKey(SignedJWT signedJwt, ECPublicKey publicKey) {
		JWSVerifier jwsVerifier = null;
		try {
			jwsVerifier = new ECDSAVerifier(publicKey);
		}
		catch (JOSEException e) {
			LOGGER.warn("Public key verifier could not be created.", e);
			return false;
		}

		try {
			return signedJwt.verify(jwsVerifier);
		}
		catch (JOSEException e) {
			LOGGER.warn("Signed JWT Token could not be verified against public key.", e);
			return false;
		}
	}
}
