package org.springframework.cloud.gcp.security.iap.claims;

import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.token.store.JwtClaimsSetVerifier;

import java.util.Map;

public class RequiredFieldsClaimVerifier implements JwtClaimsSetVerifier {
	@Override
	public void verify(Map<String, Object> claims) throws InvalidTokenException {

		if (!claims.containsKey("sub")) {
			throw new InvalidTokenException("Subject missing.");
		}

		if (!claims.containsKey("email")) {
			throw new InvalidTokenException("E-mail missing.");
		}

		if (!claims.containsKey("aud")) {
			throw new InvalidTokenException("Audience missing.");
		}

		if (!claims.containsKey("exp")) {
			throw new InvalidTokenException("Expiration missing.");
		}

		if (!claims.containsKey("iat")) {
			throw new InvalidTokenException("Issue time missing.");
		}
	}
}
