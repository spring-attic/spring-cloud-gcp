package org.springframework.cloud.gcp.security.iap.web;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.api.client.testing.http.HttpTesting;
import com.google.api.client.testing.http.MockHttpTransport;

// TODO: this can't exist as is. The token needs to be either signed by local authority (and verifier needs to take a strategy)
// or heavily mocked, or both.
@RunWith(SpringRunner.class)
public class VerifyIapRequestHeaderTest {

	@Test
	public void contextLoads() throws Exception {

		MockHttpTransport transport = new MockHttpTransport();
		HttpRequest request =
				transport.createRequestFactory().buildGetRequest(HttpTesting.SIMPLE_GENERIC_URL);
		request.setRequestMethod("GET");

		HttpHeaders requestHeaders = new HttpHeaders();
		String token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InJUbGstZyJ9.eyJpc3MiOiJodHRwczovL2Nsb3VkLmdvb2dsZS5jb20vaWFwIiwic3ViIjoiYWNjb3VudHMuZ29vZ2xlLmNvbToxMTI1MzI1MjA0MjA4MzEzMTcxNTAiLCJlbWFpbCI6ImVsZmVsQGdvb2dsZS5jb20iLCJhdWQiOiIvcHJvamVjdHMvMzkxNzY1NzIwODA0L2FwcHMvZWxmZWwtc3ByaW5nIiwiZXhwIjoxNTM5NjE3NDI3LCJpYXQiOjE1Mzk2MTY4MjcsImhkIjoiZ29vZ2xlLmNvbSJ9.dGPlQIcJo--9H1vwY73lHvAK2rPleHltPXUADFvlbC7vZtoLRtPcRc0w0xPvaVpdf8lY7U7iZTQySuUw7gJj9w";
		requestHeaders.set("x-goog-iap-jwt-assertion", token);

		request.setHeaders(requestHeaders);


		VerifyIapRequestHeader verifier = new VerifyIapRequestHeader();
		IapAuthentication verified = verifier.verifyAndExtractPrincipal(token, "does not matter right now");

		System.out.println("VERIFIED? " + verified);
	}

}
