package org.springframework.cloud.gcp.autoconfigure.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gcp.security.iap.IapAuthenticationFilter;
import org.springframework.cloud.gcp.security.iap.JwtTokenVerifier;
import org.springframework.cloud.gcp.security.iap.IapWebSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@ConditionalOnProperty(value = "spring.cloud.gcp.security.iap.enabled")
public class IapAuthenticationAutoConfiguration {
	private static final Log LOGGER = LogFactory.getLog(IapAuthenticationAutoConfiguration.class);

	// TODO: differentiate based on either AppEngine or ComputeEngine
	@Bean
	public JwtTokenVerifier verifyIapRequestHeader() {
		LOGGER.info("IapAuthenticationAutoConfiguration verifier bean creation");
		return new JwtTokenVerifier();
	}

	@Bean
	public IapAuthenticationFilter iapAuthenticationFilter(JwtTokenVerifier verifyIapRequestHeader) {
		return new IapAuthenticationFilter(verifyIapRequestHeader);
	}

	@Bean
	public WebSecurityConfigurerAdapter webSecurityConfigurerAdapter() {
		return new IapWebSecurityConfig();
	}
}
