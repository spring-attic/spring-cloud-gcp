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

import java.util.HashMap;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller to output the firebase javascript file. Used in order to allow injection of environment variables into
 * client side javascript
 *
 * @author Vinicius Carvalho
 * @since 1.3
 */
@Controller
@RequestMapping("/templates")
public class TemplateController {

	private final Configuration configuration;
	private final GcpProjectIdProvider gcpProjectIdProvider;
	private final FirebaseConfig firebaseConfig;

	public TemplateController(Configuration configuration, GcpProjectIdProvider gcpProjectIdProvider, FirebaseConfig firebaseConfig) {
		this.configuration = configuration;
		this.gcpProjectIdProvider = gcpProjectIdProvider;
		this.firebaseConfig = firebaseConfig;
	}

	@GetMapping(value = "/js/app", produces = "application/javascript")
	public @ResponseBody String appJs() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("projectId", this.gcpProjectIdProvider.getProjectId());
		model.put("firebaseConfig", firebaseConfig);
		Template template = configuration.getTemplate("app.ftl");
		String js = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
		return js;
	}

}
