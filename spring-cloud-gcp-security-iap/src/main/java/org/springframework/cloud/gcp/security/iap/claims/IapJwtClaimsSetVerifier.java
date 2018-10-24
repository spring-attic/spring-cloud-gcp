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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.token.store.IssuerClaimVerifier;
import org.springframework.security.oauth2.provider.token.store.JwtClaimsSetVerifier;


//https://openid.net/specs/openid-connect-core-1_0.html#IDToken
public class IapJwtClaimsSetVerifier implements JwtClaimsSetVerifier {
	// TODO: make configurable.
	private static final String IAP_ISSUER_URL = "https://cloud.google.com/iap";


	private Collection<JwtClaimsSetVerifier> verifiers = new ArrayList<>();


	// TODO: make claims list configurable? Or not.
	public IapJwtClaimsSetVerifier() {
		verifiers.add(new RequiredFieldsClaimVerifier());
		verifiers.add(new IssueTimeInPastClaimVerifier());
		// commented out for ease of testing of my old token
		//verifiers.add(new ExpirationTimeInFutureClaimVerifier());

		try {
			verifiers.add(new IssuerClaimVerifier(new URL(IAP_ISSUER_URL)));
		}
		catch (MalformedURLException e) {
			throw new IllegalStateException("Malformed IAP Issuer URL.", e);
		}
	}

	@Override
	public void verify(Map<String, Object> claims) throws InvalidTokenException {
		verifiers.stream().forEach(v -> v.verify(claims));
	}



	/*
	/*
	TODO: Implement filter that checks expected audience based on whether installed in AppEngine or ComputeEngine
	Preconditions.checkArgument(claims.getAudience().contains(expectedAudience));

	// Verify jwt tokens addressed to IAP protected resources on App Engine.
	// The project *number* for your Google Cloud project via 'gcloud projects describe $PROJECT_ID'
	// The project *number* can also be retrieved from the Project Info card in Cloud Console.
	// projectId is The project *ID* for your Google Cloud Project.
	boolean verifyJwtForAppEngine(HttpRequest request, long projectNumber, String projectId)
			throws Exception {
		// Check for iap jwt header in incoming request
		String jwt = request.getHeaders().getFirstHeaderStringValue("x-goog-iap-jwt-assertion");
		if (jwt == null) {
			return false;
		}
		return verifyJwt(
				jwt,
				String.format("/projects/%s/apps/%s", Long.toUnsignedString(projectNumber), projectId));
	}

	boolean verifyJwtForComputeEngine(
			HttpRequest request, long projectNumber, long backendServiceId) throws Exception {
		// Check for iap jwt header in incoming request
		String jwtToken = request.getHeaders()
				.getFirstHeaderStringValue("x-goog-iap-jwt-assertion");
		if (jwtToken == null) {
			return false;
		}
		return verifyJwt(
				jwtToken,
				String.format(
						"/projects/%s/global/backendServices/%s",
						Long.toUnsignedString(projectNumber), Long.toUnsignedString(backendServiceId)));
	}
	*/
}
