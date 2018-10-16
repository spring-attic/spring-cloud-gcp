package org.springframework.cloud.gcp.security.iap.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

public class IapAuthenticationFilter implements Filter {

	private VerifyIapRequestHeader verifyIapRequestHeader;

	public IapAuthenticationFilter(VerifyIapRequestHeader verifyIapRequestHeader) {
		this.verifyIapRequestHeader = verifyIapRequestHeader;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		System.out.println("init: " + filterConfig);
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		System.out.println("In SomeFilter: request = " + request
		+ "; headers = ");
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String name = headerNames.nextElement();
			System.out.println("Header: " + name + "; value = " + request.getHeader(name));
		}
		System.out.println("path info = " + request.getServletPath());

		System.out.println("VERIFIER = " + verifyIapRequestHeader);
		Authentication authentication = this.verifyIapRequestHeader.verifyAndExtractPrincipal(
				request.getHeader("x-goog-iap-jwt-assertion"), "does not matter right now");
		if (authentication != null) {
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		filterChain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		System.out.println("destroy: ");
	}
}
