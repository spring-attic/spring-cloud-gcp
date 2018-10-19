package org.springframework.cloud.gcp.security.iap.web;

import com.google.common.base.Preconditions;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.net.URL;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/** Verify IAP authorization JWT token in incoming request. */
public class VerifyIapRequestHeader {

	private static final String PUBLIC_KEY_VERIFICATION_URL =
			"https://www.gstatic.com/iap/verify/public_key-jwk";

	private static final String IAP_ISSUER_URL = "https://cloud.google.com/iap";

	// using a simple cache with no eviction for this sample
	private final Map<String, JWK> keyCache = new HashMap<>();

	private static Clock clock = Clock.systemUTC();

	private ECPublicKey getKey(String kid, String alg) throws Exception {
		JWK jwk = keyCache.get(kid);
		if (jwk == null) {
			// update cache loading jwk public key data from url
			JWKSet jwkSet = JWKSet.load(new URL(PUBLIC_KEY_VERIFICATION_URL));
			for (JWK key : jwkSet.getKeys()) {
				keyCache.put(key.getKeyID(), key);
			}
			jwk = keyCache.get(kid);
		}
		// confirm that algorithm matches
		if (jwk != null && jwk.getAlgorithm().getName().equals(alg)) {
			return ECKey.parse(jwk.toJSONString()).toECPublicKey();
		}
		return null;
	}

	/*
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

	IapAuthentication verifyAndExtractPrincipal(String jwtToken, String expectedAudience) {

		System.out.println("IAP_FILTER: token = " + jwtToken);

		if (jwtToken == null) {
			System.out.println("NO TOKEN IN SESSION; not authenticating");
			return null;
		}

		// parse signed token into header / claims
		SignedJWT signedJwt = null;
		try {
			System.out.println("IAP_FILTER: before extracting signed token" );
			signedJwt = SignedJWT.parse(jwtToken);
			System.out.println("IAP_FILTER: after extracting signed token " + signedJwt);
		} catch (ParseException e) {
			// TODO: log properly
			System.err.println("JWT Token could not be parsed");
			e.printStackTrace();
			return null;
		}
		JWSHeader jwsHeader = signedJwt.getHeader();

		// header must have algorithm("alg") and "kid"
		Preconditions.checkNotNull(jwsHeader.getAlgorithm());
		Preconditions.checkNotNull(jwsHeader.getKeyID());
		System.out.println("IAP_FILTER: algorithm and key id exist");

		JWTClaimsSet claims = null;
		try {
			claims = signedJwt.getJWTClaimsSet();
			System.out.println("IAP_FILTER: got claims = " + claims);
		} catch (ParseException e) {
			// TODO: log properly
			System.err.println("JWT Claims could not be parsed");
			e.printStackTrace();
			return null;
		}

		// claims must have audience, issuer
		// TODO: Vary expectec audience based on whether installed in AppEngine or ComputeEngine
		// Preconditions.checkArgument(claims.getAudience().contains(expectedAudience));
		Preconditions.checkArgument(claims.getIssuer().equals(IAP_ISSUER_URL));

		System.out.println("IAP_FILTER: checked issuer URL = ");

		// claim must have issued at time in the past
		Date currentTime = Date.from(Instant.now(clock));
		Preconditions.checkArgument(claims.getIssueTime().before(currentTime));
		// claim must have expiration time in the future
		//Preconditions.checkArgument(claims.getExpirationTime().after(currentTime));

		System.out.println("IAP_FILTER: checked time");
		// must have subject, email
		Preconditions.checkNotNull(claims.getSubject());
		Preconditions.checkNotNull(claims.getClaim("email"));

		System.out.println("IAP_FILTER: subject and claim exist");
		// verify using public key : lookup with key id, algorithm name provided
		ECPublicKey publicKey = null;
		try {
			publicKey = getKey(jwsHeader.getKeyID(), jwsHeader.getAlgorithm().getName());
			System.out.println("IAP_FILTER: got public key");
		} catch (Exception e) {
			// TODO: log properly
			System.err.println("Public Key could not be acquired");
			e.printStackTrace();
			return null;
		}

		Preconditions.checkNotNull(publicKey);

		System.out.println("IAP_FILTER: public key not null");
		JWSVerifier jwsVerifier = null;
		try {
			jwsVerifier = new ECDSAVerifier(publicKey);
		} catch (JOSEException e) {
			// TODO: log properly
			System.err.println("Public key verifier could not be created.");
			e.printStackTrace();
			return null;
		}

		System.out.println("IAP_FILTER: before verifying");

		try {
			if (!signedJwt.verify(jwsVerifier)) {
				System.out.println("IAP_FILTER: NOT VERIFIED");
				return null;
			}
		} catch (JOSEException e) {
			// TODO: log properly
			System.err.println("Signed JWT Token could not be verified against public key.");
			e.printStackTrace();
			return null;
		}

		System.out.println("IAP_FILTER: VERIFIED");
		String email = null;
		try {
			email = claims.getStringClaim("email");
		} catch (ParseException e) {
			// TODO: log properly
			System.err.println("E-mail string could not be parsed from claims.");
			e.printStackTrace();
			return null;
		}

		System.out.println("IAP_FILTER: email = " + email);
		System.out.println("IAP_FILTER: subject = " + claims.getSubject());
		return new IapAuthentication(email, claims.getSubject(), jwtToken);


	}
}