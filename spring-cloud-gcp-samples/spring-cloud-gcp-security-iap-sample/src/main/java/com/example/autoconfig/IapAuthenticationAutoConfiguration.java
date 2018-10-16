package com.example.autoconfig;

import com.example.iap.IapAuthenticationFilter;
import com.example.iap.VerifyIapRequestHeader;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.filter.RequestContextFilter;

@Configuration
public class IapAuthenticationAutoConfiguration {

	// TODO: differentiate based on either AppEngine or ComputeEngine
	@Bean
	public VerifyIapRequestHeader verifyIapRequestHeader() {
		System.out.println("================= in IapAuthenticationAutoConfiguration ============== ");
		return new VerifyIapRequestHeader();
	}

	@Bean
	public IapAuthenticationFilter iapAuthenticationFilter(VerifyIapRequestHeader verifyIapRequestHeader) {
		return new IapAuthenticationFilter(verifyIapRequestHeader);
	}
}
