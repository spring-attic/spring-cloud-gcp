package org.springframework.cloud.gcp.security.iap.web;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Collection;

public class IapAuthentication extends AbstractAuthenticationToken {

	private final String email;
	private final String subject;

	public IapAuthentication(String email, String subject) {
		super(AuthorityUtils
				.commaSeparatedStringToAuthorityList("ROLE_CLIENT"));
		this.email = email;
		this.subject = subject;
	}

	@Override
	public Object getCredentials() {
		return "[from IAP header]";
	}

	@Override
	public Object getPrincipal() {
		return email;
	}

	@Override
	public boolean isAuthenticated() {
		return true;
	}

	@Override
	public String toString() {
		return "IapAuthentication{" +
				"email='" + email + '\'' +
				'}';
	}
}
