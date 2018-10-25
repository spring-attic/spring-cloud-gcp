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
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sample REST Controller to demonstrate IAP security header extraction
 *
 * @author Elena Felder
 */
@RestController
public class ExampleController {
	private static final Log LOGGER = LogFactory.getLog(ExampleController.class);


	@RequestMapping("/")
	public String unsecured() {
		return "No secrets here!";
	}

	@RequestMapping("/topsecret")
	public String secured() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return String.format("You are [%s], as determined by [%s]\n",
				authentication.getPrincipal(), authentication.getCredentials());
	}

	@RequestMapping("/headers")
	public List<String> headers(HttpServletRequest req) {
		return Collections.list(req.getHeaderNames())
				.stream()
				.map(name -> req.getHeader(name))
				.collect(Collectors.toList());
	}
}
