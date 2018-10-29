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

package org.springframework.cloud.gcp.security.iap.web;

import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.cloud.gcp.security.iap.IapAuthentication;
import org.springframework.cloud.gcp.security.iap.jwt.DefaultJwtTokenVerifier;
import org.springframework.cloud.gcp.security.iap.jwt.JwtTokenVerifier;
import org.springframework.test.context.junit4.SpringRunner;

// TODO: this can't exist as is. The token needs to be either signed by local authority.
// Plus, it's misnamed and is in the wrong package.
// or heavily mocked, or both.
@RunWith(SpringRunner.class)
public class VerifyIapRequestHeaderTest {

	String PUBLIC_KEY_VERIFICATION_LINK = "https://www.gstatic.com/iap/verify/public_key-jwk";

	@Test
	public void contextLoads() throws Exception {

		String token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InJUbGstZyJ9.eyJpc3MiOiJodHRwczovL2Nsb3VkLmdvb2dsZS"
		+ "5jb20vaWFwIiwic3ViIjoiYWNjb3VudHMuZ29vZ2xlLmNvbToxMTI1MzI1MjA0MjA4MzEzMTcxNTAiLCJlbWFpbCI6ImVsZmVsQGdvb2dsZS"
		+ "5jb20iLCJhdWQiOiIvcHJvamVjdHMvMzkxNzY1NzIwODA0L2FwcHMvZWxmZWwtc3ByaW5nIiwiZXhwIjoxNTM5NjE3NDI3LCJpYXQiOjE1Mz"
		+ "k2MTY4MjcsImhkIjoiZ29vZ2xlLmNvbSJ9.dGPlQIcJo--9H1vwY73lHvAK2rPleHltPXUADFvlbC7vZtoLRtPcRc0w0xPvaVpdf8lY7U7iZ"
		+ "TQySuUw7gJj9w";

		JwtTokenVerifier verifier = new DefaultJwtTokenVerifier(new URL(PUBLIC_KEY_VERIFICATION_LINK));
		IapAuthentication verified = verifier.verifyAndExtractPrincipal(token);

		System.out.println("VERIFIED? " + verified);
	}

}
