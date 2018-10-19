package org.springframework.cloud.gcp.security.iap;

import org.springframework.cloud.gcp.security.iap.claims.IapJwtClaimsSetVerifier;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkTokenStore;

/** Verify IAP authorization JWT token in incoming request.
 *
 * JwkTokenStore verifies the signature through JwkVerifyingJwtAccessTokenConverter.
 */
public class JwtTokenVerifier {

	private static final String PUBLIC_KEY_VERIFICATION_URL =
			"https://www.gstatic.com/iap/verify/public_key-jwk";

	private JwkTokenStore tokenStore = new JwkTokenStore(PUBLIC_KEY_VERIFICATION_URL, new IapJwtClaimsSetVerifier());

	public IapAuthentication verifyAndExtractPrincipal(String jwtToken, String expectedAudience) {

		// reads and validates
		OAuth2AccessToken token = tokenStore.readAccessToken(jwtToken);

		IapAuthentication iapAuth
				= new IapAuthentication((String)token.getAdditionalInformation().get("email"), null, jwtToken);
		return iapAuth;
	}
}