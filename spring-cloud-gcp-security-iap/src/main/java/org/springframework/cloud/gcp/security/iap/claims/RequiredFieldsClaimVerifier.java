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

package org.springframework.cloud.gcp.security.iap.claims;

import com.nimbusds.jwt.JWTClaimsSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RequiredFieldsClaimVerifier implements ClaimVerifier {
	private static final Log LOGGER = LogFactory.getLog(RequiredFieldsClaimVerifier.class);

	@Override
	public boolean verify(JWTClaimsSet claims) {

		boolean requiredFieldsPresent = true;

		if (claims.getSubject() == null) {
			LOGGER.warn("Subject missing.");
			requiredFieldsPresent = false;
		}

		if (claims.getClaim("email") == null) {
			LOGGER.warn("E-mail missing.");
			requiredFieldsPresent =  false;
		}

		if (claims.getAudience() == null) {
			LOGGER.warn("Audience missing.");
			requiredFieldsPresent =  false;
		}

		if (claims.getExpirationTime() == null) {
			LOGGER.warn("Expiration missing.");
			requiredFieldsPresent =  false;
		}

		if (claims.getIssueTime() == null) {
			LOGGER.warn("Issue time missing.");
			requiredFieldsPresent =  false;
		}

		return requiredFieldsPresent;
	}
}
