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
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.gcp.security.iap.jwk.JwkRegistry;

public class JwtECSignatureVerifier implements JwtSignatureVerifier {

	private static final Log LOGGER = LogFactory.getLog(JwtECSignatureVerifier.class);

	private final JwkRegistry jwkRegistry;

	public JwtECSignatureVerifier(URL registryUrl) {
		this.jwkRegistry = new JwkRegistry(registryUrl);
	}

	public boolean validateJwt(SignedJWT signedJwt) {
		if (signedJwt == null) {
			LOGGER.warn("Null signed JWT is invalid.");
			return false;
		}

		JWSHeader jwsHeader = signedJwt.getHeader();

		if (jwsHeader.getAlgorithm() == null) {
			LOGGER.warn("JWT header algorithm null.");
		}
		else if (jwsHeader.getKeyID() == null) {
			LOGGER.warn("JWT key ID null.");
		}
		else {

			JWK jwk = this.jwkRegistry.getKey(jwsHeader.getKeyID(), jwsHeader.getAlgorithm().getName());
			ECPublicKey ecPublicKey = null;

			try {
				ecPublicKey = ECKey.parse(jwk.toJSONString()).toECPublicKey();
				if (ecPublicKey != null) {
					return verifyAgainstPublicKey(signedJwt, ecPublicKey);
				}
			}
			catch (JOSEException | ParseException e) {
				LOGGER.warn("JWK Public key extraction failed.", e);
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
