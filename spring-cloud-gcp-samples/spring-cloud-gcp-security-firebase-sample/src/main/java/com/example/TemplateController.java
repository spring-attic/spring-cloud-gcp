package com.example;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller to output the firebase javascript file. Used in order to allow injection of environment variables into
 * client side javascript
 * @author vinicius
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
