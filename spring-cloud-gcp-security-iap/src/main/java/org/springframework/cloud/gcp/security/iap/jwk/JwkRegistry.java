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

package org.springframework.cloud.gcp.security.iap.jwk;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * https://tools.ietf.org/html/rfc7517#section-8.1.2
 */
public class JwkRegistry {

	private static final Log LOGGER = LogFactory.getLog(JwkRegistry.class);

	// using a simple cache with no eviction for this sample
	private Map<String, JWK> keyCache = new HashMap<>();

	private static Clock clock = Clock.systemUTC();

	// Wait at least 60 seconds before cache can be redownloaded.
	private static final int MIN_MS_BEFORE_RETRY = 60000;

	private long lastJwkStoreDownloadTimestamp;

	private final URL publicKeyVerificationUrl;

	public JwkRegistry(String verificationUrl) throws MalformedURLException {
		this.publicKeyVerificationUrl = new URL(verificationUrl);
	}

	public ECPublicKey getPublicKey(String kid, String alg) {
		JWK jwk = keyCache.get(kid);
		if (jwk == null) {
			jwk = downloadJwkKeysIfCacheNotFresh(kid);
		}

		ECPublicKey ecPublicKey = null;

		if (jwk == null) {
			LOGGER.warn(String.format("JWK key [%s] not found.", kid));
		}
		else if (!jwk.getAlgorithm().getName().equals(alg)) {
			LOGGER.warn(String.format(
					"JWK key alorithm [%s] does not match expected algorithm [%s].", jwk.getAlgorithm(), alg));
		}
		else {
			try {
				ecPublicKey = ECKey.parse(jwk.toJSONString()).toECPublicKey();
			}
			catch (JOSEException | ParseException e) {
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
			}
			catch (IOException | ParseException e) {
				LOGGER.warn("Downloading JWK keys failed.", e);
				return null;
			}
			keyCache = jwkSet.getKeys().stream().collect(Collectors.toMap(key -> key.getKeyID(), Function.identity()));
		}
		return keyCache.get(kid);
	}
}
