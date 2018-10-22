package org.springframework.cloud.gcp.security.iap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

public class IapAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {
	private static final Log LOGGER = LogFactory.getLog(IapAuthenticationFilter.class);

	private JwtTokenVerifier verifyIapRequestHeader;

	public IapAuthenticationFilter(JwtTokenVerifier verifyIapRequestHeader) {
		this.verifyIapRequestHeader = verifyIapRequestHeader;
	}

	@Override
	protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
		String assertion = request.getHeader("x-goog-iap-jwt-assertion");
		Authentication authentication = null;

		if (assertion != null) {
			authentication = this.verifyIapRequestHeader.verifyAndExtractPrincipal(
					assertion, "TODO: either specify here or at filter level");
			if (authentication != null) {
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		}

		return authentication;
	}

	@Override
	protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
		return request.getHeader("x-goog-iap-jwt-assertion");
	}
}
