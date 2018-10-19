package org.springframework.cloud.gcp.security.iap.claims;

import com.google.common.base.Preconditions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.token.store.JwtClaimsSetVerifier;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class IssueTimeInPastClaimVerifier implements JwtClaimsSetVerifier {
	private static final Log LOGGER = LogFactory.getLog(IssueTimeInPastClaimVerifier.class);

	// TODO: make injectable for testing. Or is that a security hole waiting to happen?
	private static Clock clock = Clock.systemUTC();

	@Override
	public void verify(Map<String, Object> claims) throws InvalidTokenException {
		Date currentTime = Date.from(Instant.now(clock));
		LOGGER.info(String.format("Token issued at %s; current time %s", claims.get("iat"), currentTime));
		if (!Date.from(Instant.ofEpochSecond((Integer)claims.get("iat"))).before(currentTime)) {
			throw new InvalidTokenException("Issue time claim verification failed.");
		}
	}
}
