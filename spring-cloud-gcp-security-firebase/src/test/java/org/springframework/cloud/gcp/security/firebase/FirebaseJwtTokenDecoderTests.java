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

import java.security.PrivateKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * @author Vinicius Carvalho
 * @since 1.2.2
 */
public class FirebaseJwtTokenDecoderTests {

	private static RSAKeyGeneratorUtils keyGeneratorUtils;

	@BeforeClass
	public static void setup() throws Exception {
		keyGeneratorUtils = new RSAKeyGeneratorUtils();
	}

	@Test
	public void unsignedTokenTests() {
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.subject("test-subject")
				.expirationTime(Date.from(Instant.now().plusSeconds(60)))
				.build();
		PlainJWT plainJWT = new PlainJWT(claimsSet);

		FirebaseJwtTokenDecoder decoder = new FirebaseJwtTokenDecoder(mock(RestOperations.class), "https://spring.local", mock(OAuth2TokenValidator.class));
		assertThatExceptionOfType(JwtException.class)
				.isThrownBy(() -> decoder.decode(plainJWT.serialize()))
				.withMessageStartingWith("An error occurred while attempting to decode the Jwt");
	}

	@Test
	public void signedTokenTests() throws Exception {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("one").build();
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.subject("test-subject")
				.expirationTime(Date.from(Instant.now().plusSeconds(60)))
				.build();
		SignedJWT signedJWT = signedJwt(keyGeneratorUtils.getPrivateKey(), header, claimsSet);
		OAuth2TokenValidator validator = mock(OAuth2TokenValidator.class);
		when(validator.validate(any())).thenReturn(OAuth2TokenValidatorResult.success());
		FirebaseJwtTokenDecoder decoder = new FirebaseJwtTokenDecoder(mockRestOperations(), "https://spring.local", validator);
		decoder.decode(signedJWT.serialize());
	}

	@Test
	public void refreshFlowTests()  throws Exception {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("one").build();
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.subject("test-subject")
				.expirationTime(Date.from(Instant.now().plusSeconds(60)))
				.build();
		SignedJWT signedJWT = signedJwt(keyGeneratorUtils.getPrivateKey(), header, claimsSet);
		OAuth2TokenValidator validator = mock(OAuth2TokenValidator.class);
		when(validator.validate(any())).thenReturn(OAuth2TokenValidatorResult.success());
		RestOperations operations = mockRestOperations();
		FirebaseJwtTokenDecoder decoder = new FirebaseJwtTokenDecoder(operations, "https://spring.local", validator);
		decoder.decode(signedJWT.serialize());
		decoder.decode(signedJWT.serialize());
		verify(operations, times(1)).exchange(eq("https://spring.local"),
				eq(HttpMethod.GET),
				isNull(),
				eq(new ParameterizedTypeReference<Map<String, String>>() { }));
	}

	@Test
	public void keyNotFoundTests() throws Exception {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("two").build();
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.subject("test-subject")
				.expirationTime(Date.from(Instant.now().plusSeconds(60)))
				.build();
		SignedJWT signedJWT = signedJwt(keyGeneratorUtils.getPrivateKey(), header, claimsSet);
		OAuth2TokenValidator validator = mock(OAuth2TokenValidator.class);
		when(validator.validate(any())).thenReturn(OAuth2TokenValidatorResult.success());
		FirebaseJwtTokenDecoder decoder = new FirebaseJwtTokenDecoder(mockRestOperations(), "https://spring.local", validator);
		assertThatExceptionOfType(JwtException.class)
				.isThrownBy(() -> decoder.decode(signedJWT.serialize()))
				.withMessageStartingWith("No certificate found for key: ");
	}

	@Test
	public void connectionErrorTests() throws Exception {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("one").build();
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.subject("test-subject")
				.expirationTime(Date.from(Instant.now().plusSeconds(60)))
				.build();
		SignedJWT signedJWT = signedJwt(keyGeneratorUtils.getPrivateKey(), header, claimsSet);
		OAuth2TokenValidator validator = mock(OAuth2TokenValidator.class);
		when(validator.validate(any())).thenReturn(OAuth2TokenValidatorResult.success());
		RestOperations operations = mock(RestOperations.class);
		when(operations.exchange(eq("https://spring.local"),
				eq(HttpMethod.GET),
				isNull(),
				eq(new ParameterizedTypeReference<Map<String, String>>() { }))).thenThrow(new RestClientException("Could not connect to remote peer"));
		FirebaseJwtTokenDecoder decoder = new FirebaseJwtTokenDecoder(operations, "https://spring.local", validator);
		assertThatExceptionOfType(JwtException.class)
				.isThrownBy(() -> decoder.decode(signedJWT.serialize()))
				.withMessageStartingWith("Error fetching public keys");
	}

	@Test
	public void expiredTokenTests() throws Exception {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("one").build();
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.subject("test-subject")
				.expirationTime(Date.from(Instant.now().minusSeconds(3600)))
				.build();
		SignedJWT signedJWT = signedJwt(keyGeneratorUtils.getPrivateKey(), header, claimsSet);
		List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
		validators.add(new JwtTimestampValidator());
		DelegatingOAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<Jwt>(validators);
		RestOperations operations = mockRestOperations();
		FirebaseJwtTokenDecoder decoder = new FirebaseJwtTokenDecoder(operations, "https://spring.local", validator);
		assertThatExceptionOfType(JwtException.class)
				.isThrownBy(() -> decoder.decode(signedJWT.serialize()))
				.withMessageStartingWith("An error occurred while attempting to decode the Jwt: Jwt expired at");
	}

	@Test
	public void invalidIssuerTests() throws Exception {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("one").build();
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.subject("test-subject")
				.audience("123456")
				.expirationTime(Date.from(Instant.now().plusSeconds(36000)))
				.issuer("https://spring.local/123456")
				.issueTime(Date.from(Instant.now().minusSeconds(3600)))
				.claim("auth_time", Instant.now().minusSeconds(3600).getEpochSecond())
				.build();
		SignedJWT signedJWT = signedJwt(keyGeneratorUtils.getPrivateKey(), header, claimsSet);
		List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
		validators.add(new JwtTimestampValidator());
		validators.add(new JwtIssuerValidator("https://securetoken.google.com/123456"));
		DelegatingOAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<Jwt>(validators);
		RestOperations operations = mockRestOperations();
		FirebaseJwtTokenDecoder decoder = new FirebaseJwtTokenDecoder(operations, "https://spring.local", validator);
		assertThatExceptionOfType(JwtException.class)
				.isThrownBy(() -> decoder.decode(signedJWT.serialize()))
				.withMessageStartingWith("An error occurred while attempting to decode the Jwt");
	}

	@Test
	public void validTokenTests() throws Exception {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("one").build();
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.subject("test-subject")
				.audience("123456")
				.expirationTime(Date.from(Instant.now().plusSeconds(36000)))
				.issuer("https://securetoken.google.com/123456")
				.issueTime(Date.from(Instant.now().minusSeconds(3600)))
				.claim("auth_time", Instant.now().minusSeconds(3600).getEpochSecond())
				.build();
		SignedJWT signedJWT = signedJwt(keyGeneratorUtils.getPrivateKey(), header, claimsSet);
		List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
		validators.add(new JwtTimestampValidator());
		validators.add(new JwtIssuerValidator("https://securetoken.google.com/123456"));
		validators.add(new FirebaseTokenValidator("123456"));
		DelegatingOAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<Jwt>(validators);
		RestOperations operations = mockRestOperations();
		FirebaseJwtTokenDecoder decoder = new FirebaseJwtTokenDecoder(operations, "https://spring.local", validator);
		Jwt jwt = decoder.decode(signedJWT.serialize());
		assertThat(jwt.getClaims()).isNotEmpty();
	}

	@Test
	public void invalidAudienceTests() throws Exception {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("one").build();
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.subject("test-subject")
				.audience("123")
				.expirationTime(Date.from(Instant.now().plusSeconds(36000)))
				.issuer("https://securetoken.google.com/123456")
				.issueTime(Date.from(Instant.now().minusSeconds(3600)))
				.claim("auth_time", Instant.now().minusSeconds(3600).getEpochSecond())
				.build();
		SignedJWT signedJWT = signedJwt(keyGeneratorUtils.getPrivateKey(), header, claimsSet);
		List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
		validators.add(new JwtTimestampValidator());
		validators.add(new JwtIssuerValidator("https://securetoken.google.com/123456"));
		validators.add(new FirebaseTokenValidator("123456"));
		DelegatingOAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<Jwt>(validators);
		RestOperations operations = mockRestOperations();
		FirebaseJwtTokenDecoder decoder = new FirebaseJwtTokenDecoder(operations, "https://spring.local", validator);
		assertThatExceptionOfType(JwtException.class)
				.isThrownBy(() -> decoder.decode(signedJWT.serialize()))
				.withMessageStartingWith("An error occurred while attempting to decode the Jwt: This aud claim is not equal to the configured audience");
	}

	@Test
	public void invalidIssuedAt() throws Exception {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("one").build();
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.subject("test-subject")
				.audience("123456")
				.expirationTime(Date.from(Instant.now().plusSeconds(36000)))
				.issuer("https://securetoken.google.com/123456")
				.issueTime(Date.from(Instant.now().plusSeconds(3600)))
				.claim("auth_time", Instant.now().minusSeconds(3600).getEpochSecond())
				.build();
		SignedJWT signedJWT = signedJwt(keyGeneratorUtils.getPrivateKey(), header, claimsSet);
		List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
		validators.add(new JwtTimestampValidator());
		validators.add(new JwtIssuerValidator("https://securetoken.google.com/123456"));
		validators.add(new FirebaseTokenValidator("123456"));
		DelegatingOAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<Jwt>(validators);
		RestOperations operations = mockRestOperations();
		FirebaseJwtTokenDecoder decoder = new FirebaseJwtTokenDecoder(operations, "https://spring.local", validator);
		assertThatExceptionOfType(JwtException.class)
				.isThrownBy(() -> decoder.decode(signedJWT.serialize()))
				.withMessageStartingWith("An error occurred while attempting to decode the Jwt: iat claim header must be in the past");
	}

	@Test
	public void invalidSubject() throws Exception {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("one").build();
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.audience("123456")
				.expirationTime(Date.from(Instant.now().plusSeconds(36000)))
				.issuer("https://securetoken.google.com/123456")
				.issueTime(Date.from(Instant.now().minusSeconds(3600)))
				.claim("auth_time", Instant.now().minusSeconds(3600).getEpochSecond())
				.build();
		SignedJWT signedJWT = signedJwt(keyGeneratorUtils.getPrivateKey(), header, claimsSet);
		List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
		validators.add(new JwtTimestampValidator());
		validators.add(new JwtIssuerValidator("https://securetoken.google.com/123456"));
		validators.add(new FirebaseTokenValidator("123456"));
		DelegatingOAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<Jwt>(validators);
		RestOperations operations = mockRestOperations();
		FirebaseJwtTokenDecoder decoder = new FirebaseJwtTokenDecoder(operations, "https://spring.local", validator);
		assertThatExceptionOfType(JwtException.class)
				.isThrownBy(() -> decoder.decode(signedJWT.serialize()))
				.withMessageStartingWith("An error occurred while attempting to decode the Jwt: sub claim can not be empty");
	}

	private RestOperations mockRestOperations() throws Exception {
		Map<String, String> payload = new HashMap<>();
		payload.put("one", keyGeneratorUtils.getPublicKeyCertificate());
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(3600L, TimeUnit.SECONDS).getHeaderValue());
		ResponseEntity<Map<String, String>> response = new ResponseEntity<>(payload, headers, HttpStatus.OK);
		return mockRestOperations(response);
	}

	private RestOperations mockRestOperations(ResponseEntity<Map<String, String>> response) {
		RestOperations mock = mock(RestOperations.class);
		when(mock.exchange(eq("https://spring.local"),
				eq(HttpMethod.GET),
				isNull(),
				eq(new ParameterizedTypeReference<Map<String, String>>() { })))
				.thenReturn(response);
		return mock;
	}

	private SignedJWT signedJwt(PrivateKey privateKey, JWSHeader header, JWTClaimsSet claimsSet) throws Exception {
		JWSSigner signer = new RSASSASigner(privateKey);
		return signedJwt(signer, header, claimsSet);
	}

	private SignedJWT signedJwt(JWSSigner signer, JWSHeader header, JWTClaimsSet claimsSet) throws Exception {
		SignedJWT signedJWT = new SignedJWT(header, claimsSet);
		signedJWT.sign(signer);
		return signedJWT;
	}

}
