/*
 * Copyright 2020 the original author or authors.
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
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestOperations;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Vinicius Carvalho
 * @since 1.3
 */
public class FirebaseJwtTokenDecoder implements JwtDecoder {

    private static final String DECODING_ERROR_MESSAGE_TEMPLATE =
            "An error occurred while attempting to decode the Jwt: %s";
    private Pattern maxAgePattern = Pattern.compile("max-age=(\\d*)");
    private ReentrantLock keysLock = new ReentrantLock();
    private volatile Long expires = 0L;
    private Map<String, JwtDecoder> delegates = new ConcurrentHashMap<>();
    private final RestOperations restClient;
    private final String googlePublicKeysEndpoint;
    private final OAuth2TokenValidator<Jwt> tokenValidator;
    private final Logger logger = LoggerFactory.getLogger(FirebaseJwtTokenDecoder.class);

    public FirebaseJwtTokenDecoder(RestOperations restClient, String googlePublicKeysEndpoint, OAuth2TokenValidator<Jwt> tokenValidator) {
        this.restClient = restClient;
        this.googlePublicKeysEndpoint = googlePublicKeysEndpoint;
        this.tokenValidator = tokenValidator;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        SignedJWT jwt = parse(token);
        if(isExpired()){
            try{
                keysLock.tryLock();
                refresh();
            }finally {
                keysLock.unlock();
            }
        }
        JwtDecoder decoder = delegates.get(jwt.getHeader().getKeyID());
        if(decoder == null){
            throw new JwtException("No certificate found for key: " + jwt.getHeader().getKeyID());
        }
        return decoder.decode(token);
    }

    private void refresh() {
        if(!isExpired()){
            return;
        }
        try{
            ResponseEntity<Map<String, String>> response = restClient.exchange(googlePublicKeysEndpoint, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, String>>() {});
            Long expiresAt = parseCacheControlHeaders(response.getHeaders());
            this.expires = expiresAt > -1L ? (System.currentTimeMillis() + expiresAt*1000) : 0L;
            if(!response.getStatusCode().is2xxSuccessful()){
                throw new JwtException("Error retrieving public certificates from remote endpoint");
            }
            delegates.clear();
            for(String key : response.getBody().keySet()){
                try{
                    NimbusJwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) convertToX509Cert(response.getBody().get(key)).getPublicKey())
                            .signatureAlgorithm(SignatureAlgorithm.from("RS256"))
                            .build();
                    nimbusJwtDecoder.setJwtValidator(tokenValidator);
                    delegates.put(key, nimbusJwtDecoder);
                } catch (CertificateException ce){
                    logger.error("Could not read certificate for key {}", key);
                }
            }
        } catch (Exception e){
            throw new JwtException("Error fetching public keys", e);
        }
    }

    private SignedJWT parse(String token) {
        try {
            JWT jwt =  JWTParser.parse(token);
            if (!(jwt instanceof SignedJWT)) {
                throw new JwtException("Unsupported algorithm of " + jwt.getHeader().getAlgorithm());
            }
            return (SignedJWT)jwt;
        } catch (Exception ex) {
            throw new JwtException(String.format(DECODING_ERROR_MESSAGE_TEMPLATE, ex.getMessage()), ex);
        }
    }

    private Boolean isExpired() {
        return System.currentTimeMillis() >= this.expires;
    }

    private X509Certificate convertToX509Cert(String certificateString) throws CertificateException {
        X509Certificate certificate = null;
        CertificateFactory cf = null;
        try {
            if (certificateString != null && !certificateString.trim().isEmpty()) {
                certificateString = certificateString
                        .replace("\n", "")
                        .replace("-----BEGIN CERTIFICATE-----", "")
                        .replace("-----END CERTIFICATE-----", "");
                byte[] certificateData = Base64.getDecoder().decode(certificateString);
                cf = CertificateFactory.getInstance("X509");
                certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificateData));
            }
        } catch (CertificateException e) {
            throw new CertificateException(e);
        }
        return certificate;
    }

    /**
     * Parsers the HTTP Cache-Control Headers
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
        for(String header: headers){
            Matcher matcher = maxAgePattern.matcher(header);
            if(matcher.matches()){
                return Long.valueOf(matcher.group(1));
            }

        }
        return maxAge;
    }
}
