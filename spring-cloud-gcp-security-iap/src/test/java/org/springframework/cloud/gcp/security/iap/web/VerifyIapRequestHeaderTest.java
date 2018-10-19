package org.springframework.cloud.gcp.security.iap.web;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.cloud.gcp.security.iap.IapAuthentication;
import org.springframework.cloud.gcp.security.iap.JwtTokenVerifier;
import org.springframework.test.context.junit4.SpringRunner;

// TODO: this can't exist as is. The token needs to be either signed by local authority (and verifier needs to take a strategy)
// Plus, it's misnamed and is in the wrong package.
// or heavily mocked, or both.
@RunWith(SpringRunner.class)
public class VerifyIapRequestHeaderTest {

	@Test
	public void contextLoads() throws Exception {

		String token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InJUbGstZyJ9.eyJpc3MiOiJodHRwczovL2Nsb3VkLmdvb2dsZS5jb20vaWFwIiwic3ViIjoiYWNjb3VudHMuZ29vZ2xlLmNvbToxMTI1MzI1MjA0MjA4MzEzMTcxNTAiLCJlbWFpbCI6ImVsZmVsQGdvb2dsZS5jb20iLCJhdWQiOiIvcHJvamVjdHMvMzkxNzY1NzIwODA0L2FwcHMvZWxmZWwtc3ByaW5nIiwiZXhwIjoxNTM5NjE3NDI3LCJpYXQiOjE1Mzk2MTY4MjcsImhkIjoiZ29vZ2xlLmNvbSJ9.dGPlQIcJo--9H1vwY73lHvAK2rPleHltPXUADFvlbC7vZtoLRtPcRc0w0xPvaVpdf8lY7U7iZTQySuUw7gJj9w";

		JwtTokenVerifier verifier = new JwtTokenVerifier();
		IapAuthentication verified = verifier.verifyAndExtractPrincipal(token, "does not matter right now");

		System.out.println("VERIFIED? " + verified);
	}

}
