/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.security.iap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;


/**
 * {@link Jwt} token validator for a custom audience claim.
 *
 * @author Elena Felder
 *
 * @since 1.1
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt>, InitializingBean {

	private static final Log LOGGER = LogFactory.getLog(AudienceValidator.class);

	private static final OAuth2Error INVALID_AUDIENCE = new OAuth2Error(
			OAuth2ErrorCodes.INVALID_REQUEST,
					"This aud claim is not equal to the configured audience",
							"https://tools.ietf.org/html/rfc6750#section-3.1");

	private final AudienceProvider audienceProvider;

	private String audience;

	public AudienceValidator(AudienceProvider audienceProvider) {
		Assert.notNull(audienceProvider, "Audience Provider cannot be null");
		this.audienceProvider = audienceProvider;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt t) {
		if (t.getAudience() != null && t.getAudience().contains(this.audience)) {
			return OAuth2TokenValidatorResult.success();
		}
		else {
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn(String.format(
						"Expected audience %s did not match token audience %s", this.audience, t.getAudience()));
			}
			return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.audience = this.audienceProvider.getAudience();
		Assert.notNull(this.audience, "Audience cannot be null.");
	}

	public String getAudience() {
		return this.audience;
	}
}
