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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class RequiredFieldsClaimVerifierTest {

	LocalDateTime fakeIssueTime = LocalDateTime.of(2018, 11, 1, 11, 10, 00);

	LocalDateTime fakeExpirationTime = LocalDateTime.of(2018, 11, 1, 11, 20, 00);

	private JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
			.claim("email", "ford@betelgeuse.tours")
			.subject("Ix")
			.issueTime(new Date(this.fakeIssueTime.toEpochSecond(ZoneOffset.UTC)))
			.expirationTime(new Date(this.fakeIssueTime.toEpochSecond(ZoneOffset.UTC)))
			.build();

	RequiredFieldsClaimVerifier verifier = new RequiredFieldsClaimVerifier();

	@Test
	public void allRequiredFieldsPresentSucceeds() {
		assertTrue(this.verifier.verify(this.claimsSet));
	}
}
