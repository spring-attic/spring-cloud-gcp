package org.springframework.cloud.gcp.security.firebase;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.util.Assert;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Checks if the claim header "iat" is in the past. This class uses the same logic as {@link org.springframework.security.oauth2.jwt.JwtTimestampValidator}
 * but with reverse logic on the timestamp field.
 *
 * @author Vinicius Carvalho
 * @since 1.3
 *
 */
public class JwtIssuedAtValidator implements OAuth2TokenValidator<Jwt> {

    private static final Duration DEFAULT_MAX_CLOCK_SKEW = Duration.of(60, ChronoUnit.SECONDS);

    private final Duration clockSkew;

    private Clock clock = Clock.systemUTC();

    public JwtIssuedAtValidator(){
        this(DEFAULT_MAX_CLOCK_SKEW);
    }

    public JwtIssuedAtValidator(Duration clockSkew) {
        Assert.notNull(clockSkew, "clockSkew cannot be null");
        this.clockSkew = clockSkew;
    }


    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Assert.notNull(token, "jwt cannot be null");

        Instant issuedAt = token.getIssuedAt();

        if (issuedAt != null) {
            if (Instant.now(this.clock).plus(clockSkew).isBefore(issuedAt)) {
                OAuth2Error error = new OAuth2Error(
                        OAuth2ErrorCodes.INVALID_REQUEST,
                        String.format("iat claim header must be in the past"),
                        "https://tools.ietf.org/html/rfc6750#section-3.1");
                return OAuth2TokenValidatorResult.failure(error);
            }
        }

        return OAuth2TokenValidatorResult.success();
    }

}
