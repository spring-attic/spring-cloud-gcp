package org.springframework.cloud.gcp.security.iap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@EnableWebSecurity
public class IapWebSecurityConfig extends WebSecurityConfigurerAdapter {
	private static final Log LOGGER = LogFactory.getLog(IapWebSecurityConfig.class);

	@Autowired IapAuthenticationFilter filter;

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		http.addFilterBefore(filter, BasicAuthenticationFilter.class);

		LOGGER.info("IapWebSecurityConfig picked up: " + http);
	}
}