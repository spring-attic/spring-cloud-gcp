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

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {
	private static OAuth2Error INVALID_AUDIENCE = new OAuth2Error(
			OAuth2ErrorCodes.INVALID_REQUEST,
					"This aud claim is not equal to the configured audience",
							"https://tools.ietf.org/html/rfc6750#section-3.1");

	private String audience;

	public AudienceValidator(String audience) {
		this.audience = audience;
	}

	/**
	 * Allows subclasses to delay setting the audience value after the object is constructed.
	 */
	protected AudienceValidator() {}

	protected void setAudience(String audience) {
		this.audience = audience;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt t) {
		if (t.getAudience() != null && t.getAudience().contains(audience)) {
			return OAuth2TokenValidatorResult.success();
		}

		return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
	}
}
