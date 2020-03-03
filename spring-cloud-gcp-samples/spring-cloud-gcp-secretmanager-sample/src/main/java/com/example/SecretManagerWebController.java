/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.secretmanager.SecretManagerTemplate;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class SecretManagerWebController {

	@Autowired
	private Environment environment;

	@Autowired
	private MyAppProperties properties;

	@Autowired
	private SecretManagerTemplate secretManagerTemplate;

	// Application secrets can be accessed using @Value and passing in the secret name.
	// Note that the secret name is prefixed with "secrets" because of the prefix setting in
	// bootstrap.properties.
	@Value("${secrets.application-secret}")
	private String applicationSecretValue;

	@GetMapping("/")
	public ModelAndView renderIndex(ModelMap map) {
		map.put("applicationSecret", this.applicationSecretValue);
		return new ModelAndView("index.html", map);
	}

	@GetMapping("/getSecret")
	@ResponseBody
	public String getSecret(@RequestParam String secretId, ModelMap map) {
		String secretPayload = this.secretManagerTemplate.getSecretString(secretId);
		return "Secret ID: " + secretId + " | Value: " + secretPayload
				+ "<br/><br/><a href='/'>Go back</a>";
	}

	@PostMapping("/createSecret")
	public ModelAndView createSecret(
			@RequestParam String secretId, @RequestParam String secretPayload, ModelMap map) {
		this.secretManagerTemplate.createSecret(secretId, secretPayload);
		map.put("applicationSecret", this.applicationSecretValue);
		map.put("message", "Secret created!");
		return new ModelAndView("index.html", map);
	}
}
