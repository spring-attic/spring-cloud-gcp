package org.springframework.cloud.gcp.security.iap.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

@EnableWebSecurity
public class IapWebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired IapAuthenticationFilter filter;

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		http.addFilterBefore(filter, AbstractPreAuthenticatedProcessingFilter.class);

		System.out.println("***************** Web security picked up: " + http);
	}
}