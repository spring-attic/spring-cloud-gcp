/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.example;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sample REST Controller to demonstrate IAP security header extraction.
 *
 * <p>Makes the following pages available:
 * <ul>
 *     <li>{@code /} - Unsecured page.
 *     <li>{@code /topsecret} - Secured page requiring non-anonymous authentication. Prints IAP identity details.
 *     <li>{@code /headers} - Unsecured page that can be used for troubleshooting.
 * </ul>
 *
 * @author Elena Felder
 * @since 1.1
 */
@RestController
public class ExampleController {

	@RequestMapping("/")
	public String unsecured() {
		return "No secrets here!\n";
	}

	@RequestMapping("/topsecret")
	public String secured() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
			Jwt jwt = (Jwt) authentication.getPrincipal();
			return String.format("You are [%s] with e-mail address [%s].%n",
					jwt.getSubject(), jwt.getClaimAsString("email"));
		}
		else {
			return "Something went wrong; authentication is not provided by IAP/JWT.\n";
		}

	}

	@RequestMapping("/headers")
	public Map<String, String> headers(HttpServletRequest req) {
		return Collections.list(req.getHeaderNames())
				.stream()
				.collect(Collectors.toMap(Function.identity(), req::getHeader));
	}
}
