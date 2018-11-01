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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ExpirationTimeInFutureClaimVerifierTest {

	LocalDateTime fakeDateTime = LocalDateTime.of(2018, 11, 1, 11, 10, 00);

	Clock clock = Clock.fixed(this.fakeDateTime.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

	ExpirationTimeInFutureClaimVerifier verifier = new ExpirationTimeInFutureClaimVerifier();

	public ExpirationTimeInFutureClaimVerifierTest() {
		this.verifier.setClock(this.clock);
	}

	@Test
	public void expirationInPastFails() {
		Date pastDateTime = Date.from(this.fakeDateTime.minusMinutes(1).toInstant(ZoneOffset.UTC));
		assertFalse(this.verifier.verify(makeClaimsSet(pastDateTime)));
	}

	@Test
	public void expirationRightNowFails() {
		Date currentDateTime = Date.from(this.fakeDateTime.toInstant(ZoneOffset.UTC));
		assertFalse(this.verifier.verify(makeClaimsSet(currentDateTime)));
	}

	@Test
	public void expirationInFutureSucceeds() {
		Date futureDateTime = Date.from(this.fakeDateTime.plusMinutes(1).toInstant(ZoneOffset.UTC));
		assertTrue(this.verifier.verify(makeClaimsSet(futureDateTime)));
	}

	private JWTClaimsSet makeClaimsSet(Date date) {
		return new JWTClaimsSet.Builder().expirationTime(date).build();
	}
}
