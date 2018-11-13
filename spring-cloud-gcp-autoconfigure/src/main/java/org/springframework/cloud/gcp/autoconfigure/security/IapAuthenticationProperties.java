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

package org.springframework.cloud.gcp.autoconfigure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cloud IAP Authentication properties.
 *
 * @author Elena Felder
 * @since 1.1
 */
@ConfigurationProperties("spring.cloud.gcp.security.iap")
public class IapAuthenticationProperties {

	/**
	 * Link to JWK public key registry.
	 */
	private String registry = "https://www.gstatic.com/iap/verify/public_key-jwk";

	/**
	 * Encryption algorithm used to sign the JWK token.
	 */
	private String algorithm = "ES256";

	/**
	 * Header from which to extract the JWK key.
	 */
	private String header = "x-goog-iap-jwt-assertion";

	public String getRegistry() {
		return this.registry;
	}

	public void setRegistry(String registry) {
		this.registry = registry;
	}

	public String getAlgorithm() {
		return this.algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public String getHeader() {
		return this.header;
	}

	public void setHeader(String header) {
		this.header = header;
	}
}
