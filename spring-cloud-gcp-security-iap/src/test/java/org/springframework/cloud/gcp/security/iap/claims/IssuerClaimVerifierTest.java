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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class IssuerClaimVerifierTest {

	public static final String ISSUER = "https://cloud.google.com/iap";

	public static final String BAD_ISSUER = "galactic-nomenclaturoid-office";

	@Test
	public void correctIssuerSucceeds() {
		IssuerClaimVerifier verifier = new IssuerClaimVerifier();

		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.issuer(ISSUER)
				.build();

		assertTrue(verifier.verify(claimsSet));
	}

	@Test
	public void incorrectIssuerFails() {
		IssuerClaimVerifier verifier = new IssuerClaimVerifier();

		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.issuer(BAD_ISSUER)
				.build();
		assertFalse(verifier.verify(claimsSet));
	}
}
