package org.springframework.cloud.gcp.autoconfigure.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gcp.security.iap.web.IapAuthenticationFilter;
import org.springframework.cloud.gcp.security.iap.web.VerifyIapRequestHeader;
import org.springframework.cloud.gcp.security.iap.web.IapWebSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@ConditionalOnProperty(value = "spring.cloud.gcp.security.iap.enabled")
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

	@Bean
	public WebSecurityConfigurerAdapter webSecurityConfigurerAdapter() {
		return new IapWebSecurityConfig();
	}
}
