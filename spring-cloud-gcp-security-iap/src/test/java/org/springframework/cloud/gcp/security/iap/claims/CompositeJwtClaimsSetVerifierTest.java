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

import com.nimbusds.jose.proc.SimpleSecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.JWTClaimsSetVerifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CompositeJwtClaimsSetVerifierTest {

	private static JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
			.build();

	private static JWTClaimsSetVerifier<SimpleSecurityContext> NOOP = (claims, ctx) -> { };

	private static JWTClaimsSetVerifier<SimpleSecurityContext> FAIL =  (claims, ctx) -> {
		throw new BadJWTException("gaaah!");
	};

	@Test
	public void emptyCompositeSucceeds() throws BadJWTException {
		CompositeJwtClaimsSetVerifier verifier = new CompositeJwtClaimsSetVerifier();
		verifier.verify(claimsSet, null);
	}

	@Test
	public void singleSuccessfulTestCompositeSucceeds() throws BadJWTException {
		CompositeJwtClaimsSetVerifier verifier = new CompositeJwtClaimsSetVerifier(NOOP);
		verifier.verify(claimsSet, null);
	}

	@Test(expected = BadJWTException.class)
	public void singleFailingTestCompositeFails() throws BadJWTException {
		CompositeJwtClaimsSetVerifier verifier = new CompositeJwtClaimsSetVerifier(FAIL);
		verifier.verify(claimsSet, null);
	}

	@Test
	public void manySuccessfulVerifiersCompositeSucceeds() throws BadJWTException {
		CompositeJwtClaimsSetVerifier verifier = new CompositeJwtClaimsSetVerifier(NOOP, NOOP, NOOP);
		verifier.verify(claimsSet, null);
	}

	@Test(expected = BadJWTException.class)
	public void singleFailedVerifiersCompositeFails() throws BadJWTException {
		CompositeJwtClaimsSetVerifier verifier = new CompositeJwtClaimsSetVerifier(NOOP, NOOP, FAIL);
		verifier.verify(claimsSet, null);
	}
}
