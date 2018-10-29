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

import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import com.nimbusds.jwt.JWTClaimsSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExpirationTimeInFutureClaimVerifier implements ClaimVerifier {
	private static final Log LOGGER = LogFactory.getLog(ExpirationTimeInFutureClaimVerifier.class);

	// TODO: make injectable for testing. Or is that a security hole waiting to happen?
	private static Clock clock = Clock.systemUTC();

	@Override
	public boolean verify(JWTClaimsSet claims) {
		Date currentTime = Date.from(Instant.now(clock));
		LOGGER.info(String.format("Token expires at at %s; current time %s", claims.getExpirationTime(), currentTime));
		if (!claims.getExpirationTime().after(currentTime)) {
			LOGGER.warn("Token expiration claim failed.");
			return false;
		}
		return true;
	}
}
