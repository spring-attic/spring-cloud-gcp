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
public class CompositeClaimVerifierTest {

	private static JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
			.build();

	@Test
	public void emptyCompositeSucceeds() {
		CompositeClaimVerifier verifier = new CompositeClaimVerifier();
		assertTrue(verifier.verify(claimsSet));
	}

	@Test
	public void singleSuccessfulTestCompositeSucceeds() {
		CompositeClaimVerifier verifier = new CompositeClaimVerifier(claims -> true);
		assertTrue(verifier.verify(claimsSet));
	}

	@Test
	public void singleFailingTestCompositeFails() {
		CompositeClaimVerifier verifier = new CompositeClaimVerifier(claims -> false);
		assertFalse(verifier.verify(claimsSet));
	}

	@Test
	public void manySuccessfulVerifiersCompositeSucceeds() {
		CompositeClaimVerifier verifier = new CompositeClaimVerifier(
				claims -> true, claims -> true, claims -> true);
		assertTrue(verifier.verify(claimsSet));
	}

	@Test
	public void singleFailedVerifiersCompositeFails() {
		CompositeClaimVerifier verifier = new CompositeClaimVerifier(
				claims -> true, claims -> true, claims -> false);
		assertFalse(verifier.verify(claimsSet));
	}
}
