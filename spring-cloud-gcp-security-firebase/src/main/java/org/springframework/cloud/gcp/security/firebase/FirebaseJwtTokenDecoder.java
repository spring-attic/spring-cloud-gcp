/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.security.firebase;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.Assert;
import org.springframework.web.client.RestOperations;


/**
 * Decodes a Firebase token into a {@link Jwt} token.
 * This decoder downloads public keys from https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com.
 * Keys are rotated often, and expiration date is returned as part of a Cache-Control max-age header.
 * The keys are cached locally and only refreshed when the expiration time is past.
 * Besides using the RSA keys to validate the token signature, this decoder also uses a pre=configured {@link org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator}
 * to validate all the claims.
 * The following validators are used by this class:
 * {@link org.springframework.security.oauth2.jwt.JwtTimestampValidator} - Validates the expiration date of the Token
 * {@link org.springframework.security.oauth2.jwt.JwtIssuerValidator} - Validates the iss claim header
 * {@link FirebaseTokenValidator} - Validates all other headers according to definition at https://firebase.google.com/docs/auth/admin/verify-id-tokens
 * @author Vinicius Carvalho
 * @since 1.2.2
 */
public class FirebaseJwtTokenDecoder implements JwtDecoder {
	private static final String DECODING_ERROR_MESSAGE_TEMPLATE =
		"An error occurred while attempting to decode the Jwt: %s";
	private final RestOperations restClient;
	private final String googlePublicKeysEndpoint;
	private final OAuth2TokenValidator<Jwt> tokenValidator;
	private final Logger logger = LoggerFactory.getLogger(FirebaseJwtTokenDecoder.class);
	private Pattern maxAgePattern = Pattern.compile("max-age=(\\d*)");
	private ReentrantLock keysLock = new ReentrantLock();
	private volatile Long expires = 0L;
	private Map<String, JwtDecoder> delegates = new ConcurrentHashMap<>();
	public FirebaseJwtTokenDecoder(RestOperations restClient, String googlePublicKeysEndpoint, OAuth2TokenValidator<Jwt> tokenValidator) {
		this.restClient = restClient;
		this.googlePublicKeysEndpoint = googlePublicKeysEndpoint;
		this.tokenValidator = tokenValidator;
	}
	@Override
	public Jwt decode(String token) throws JwtException {
		SignedJWT jwt = parse(token);
		if (isExpired()) {
			try {
				keysLock.tryLock();
				refresh();
			}
			finally {
				keysLock.unlock();
			}
		}
		JwtDecoder decoder = delegates.get(jwt.getHeader().getKeyID());
		if (decoder == null) {
			throw new JwtException("No certificate found for key: " + jwt.getHeader().getKeyID());
		}
		return decoder.decode(token);
	}

	private void refresh() {
		if (!isExpired()) {
			return;
		}
		try {
			ResponseEntity<Map<String, String>> response = restClient.exchange(googlePublicKeysEndpoint, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, String>>() {
			});
			Long expiresAt = parseCacheControlHeaders(response.getHeaders());
			this.expires = expiresAt > -1L ? (System.currentTimeMillis() + expiresAt * 1000) : 0L;
			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new JwtException("Error retrieving public certificates from remote endpoint");
			}
			delegates.clear();
			for (String key : response.getBody().keySet()) {
				try {
					NimbusJwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) convertToX509Cert(response.getBody().get(key)).getPublicKey())
							.signatureAlgorithm(SignatureAlgorithm.from("RS256"))
							.build();
					nimbusJwtDecoder.setJwtValidator(tokenValidator);
					delegates.put(key, nimbusJwtDecoder);
				}
				catch (Exception ce) {
					logger.error("Could not read certificate for key {}", key);
				}
			}
		}
		catch (Exception e) {
			throw new JwtException("Error fetching public keys", e);
		}
	}

	private SignedJWT parse(String token) {
		try {
			JWT jwt = JWTParser.parse(token);
			if (!(jwt instanceof SignedJWT)) {
				throw new JwtException("Unsupported algorithm of " + jwt.getHeader().getAlgorithm());
			}
			return (SignedJWT) jwt;
		}
		catch (Exception ex) {
			throw new JwtException(String.format(DECODING_ERROR_MESSAGE_TEMPLATE, ex.getMessage()), ex);
		}
	}

	private Boolean isExpired() {
		return System.currentTimeMillis() >= this.expires;
	}

	private X509Certificate convertToX509Cert(String certificateString) {
		X509Certificate certificate = X509CertUtils.parse(certificateString);
		Assert.notNull(certificate, "Could not parse certificate String");
		return certificate;
	}

	/**
	 * Parsers the HTTP Cache-Control Headers.
	 *
	 * @param httpHeaders HTTP Headers
	 * @return the max-age value when present, or -1 if not found.
	 */
	private Long parseCacheControlHeaders(HttpHeaders httpHeaders) {
		Long maxAge = -1L;
		List<String> cacheControlHeaders = httpHeaders.get(HttpHeaders.CACHE_CONTROL);
		if (cacheControlHeaders == null || cacheControlHeaders.isEmpty()) {
			return maxAge;
		}
		String[] headers = cacheControlHeaders.get(0).split(",");
		for (String header : headers) {
			Matcher matcher = maxAgePattern.matcher(header.trim());
			if (matcher.matches()) {
				return Long.valueOf(matcher.group(1));
			}

		}
		return maxAge;
	}
}
