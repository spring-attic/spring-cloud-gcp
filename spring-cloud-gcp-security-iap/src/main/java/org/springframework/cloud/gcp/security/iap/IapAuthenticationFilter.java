package org.springframework.cloud.gcp.security.iap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

public class IapAuthenticationFilter implements Filter {
	private static final Log LOGGER = LogFactory.getLog(IapAuthenticationFilter.class);

	private JwtTokenVerifier verifyIapRequestHeader;

	public IapAuthenticationFilter(JwtTokenVerifier verifyIapRequestHeader) {
		this.verifyIapRequestHeader = verifyIapRequestHeader;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		LOGGER.info("In IapAuthenticationFilter");

		String assertion = request.getHeader("x-goog-iap-jwt-assertion");
		
		if (assertion != null) {
			Authentication authentication = this.verifyIapRequestHeader.verifyAndExtractPrincipal(
					request.getHeader("x-goog-iap-jwt-assertion"), "TODO: either specify here or at filter level");
			if (authentication != null) {
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		}
		filterChain.doFilter(request, response);
	}
}
