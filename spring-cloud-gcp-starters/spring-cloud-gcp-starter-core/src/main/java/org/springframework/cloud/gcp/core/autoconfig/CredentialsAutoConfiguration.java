package org.springframework.cloud.gcp.core.autoconfig;

import java.io.FileInputStream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import com.google.auth.oauth2.GoogleCredentials;

/**
 * @author Vinicius Carvalho
 */
@Configuration
@ConditionalOnClass(GoogleCredentials.class)
public class CredentialsAutoConfiguration implements EnvironmentAware {

	private static final String AUTH_LOCATION_KEY = "cloud.gcp.auth.location";

	private Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Bean
	public GoogleCredentials googleCredentials(ResourceLoader resourceLoader)
			throws Exception {
		if (!StringUtils.isEmpty(this.environment.getProperty(AUTH_LOCATION_KEY))) {
			return GoogleCredentials.fromStream(new FileInputStream(resourceLoader
					.getResource(this.environment.getProperty(AUTH_LOCATION_KEY))
					.getFile()));
		}
		else {
			return GoogleCredentials.getApplicationDefault();
		}
	}
}
