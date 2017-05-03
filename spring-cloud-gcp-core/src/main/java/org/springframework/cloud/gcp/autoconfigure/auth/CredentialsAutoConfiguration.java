package org.springframework.cloud.gcp.autoconfigure.auth;

import java.io.FileInputStream;

import com.google.auth.oauth2.GoogleCredentials;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

/**
 * @author Vinicius Carvalho
 */
@Configuration
@ConditionalOnClass(GoogleCredentials.class)
public class CredentialsAutoConfiguration implements EnvironmentAware{

	private Environment environment;

	@Bean
	public GoogleCredentials googleCredentials(ResourceLoader resourceLoader) throws Exception{
		if(!StringUtils.isEmpty(this.environment.getProperty("cloud.gcp.auth.location"))){
			return GoogleCredentials.fromStream(
					new FileInputStream(resourceLoader
							.getResource(this.environment.getProperty("cloud.gcp.auth.location")).getFile()));
		}
		else{
			return GoogleCredentials.getApplicationDefault();
		}
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}
}
