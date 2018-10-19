package org.springframework.cloud.gcp.security.iap.claims;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import com.google.common.base.Preconditions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.token.store.JwtClaimsSetVerifier;

public class ExpirationTimeInFutureClaimVerifier implements JwtClaimsSetVerifier {
	private static final Log LOGGER = LogFactory.getLog(ExpirationTimeInFutureClaimVerifier.class);

	// TODO: make injectable for testing. Or is that a security hole waiting to happen?
	private static Clock clock = Clock.systemUTC();

	@Override
	public void verify(Map<String, Object> claims) throws InvalidTokenException {
		Date currentTime = Date.from(Instant.now(clock));
		LOGGER.info(String.format("Token expires at at %s; current time %s", claims.get("iss"), currentTime));
		if (!Date.from(Instant.ofEpochSecond((Long)claims.get("exp"))).after(currentTime)) {
			throw new InvalidTokenException("Token expiration claim failed.");
		}
	}
}
