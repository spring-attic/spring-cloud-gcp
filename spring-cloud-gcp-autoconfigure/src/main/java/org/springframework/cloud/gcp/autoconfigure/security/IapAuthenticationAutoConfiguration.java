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

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoderJwkSupport;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 * Configures GCP IAP pre-authentication.
 * If the property is missing, this auto configuration is skipped.
 */
@Configuration
@ConditionalOnProperty(value = "spring.cloud.gcp.security.iap.enabled", matchIfMissing = false)
@ConditionalOnClass({NimbusJwtDecoderJwkSupport.class})
@AutoConfigureBefore(OAuth2ResourceServerAutoConfiguration.class)
public class IapAuthenticationAutoConfiguration {

	// TODO: externalize as properties.
	private static final String PUBLIC_KEY_VERIFICATION_LINK = "https://www.gstatic.com/iap/verify/public_key-jwk";

	private static final String ENCRYPTION_ALGORITHM = "ES256";

	private static final String IAP_HEADER = "x-goog-iap-jwt-assertion";


	@Bean
	@ConditionalOnMissingBean
	public JwtDecoder iapJwtDecoder() {
		return new NimbusJwtDecoderJwkSupport(PUBLIC_KEY_VERIFICATION_LINK, ENCRYPTION_ALGORITHM);
	}

	@Bean
	@ConditionalOnMissingBean
	public BearerTokenResolver iatTokenResolver() {
		return r -> r.getHeader(IAP_HEADER);
	}
}
