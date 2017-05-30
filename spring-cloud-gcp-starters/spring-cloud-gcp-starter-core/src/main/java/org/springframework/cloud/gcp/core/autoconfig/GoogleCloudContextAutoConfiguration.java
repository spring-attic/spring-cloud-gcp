package org.springframework.cloud.gcp.core.autoconfig;

import java.io.FileInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.core.GoogleCloudProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import com.google.auth.oauth2.GoogleCredentials;

/**
 * @author Vinicius Carvalho
 *
 * Base starter for Google Cloud Projects. Provide defaults for {@link GoogleCredentials}.
 * Binds properties from {@link org.springframework.cloud.gcp.core.GoogleCloudProperties}
 *
 */
@Configuration
@ConditionalOnClass(GoogleCredentials.class)
@EnableConfigurationProperties(GoogleCloudProperties.class)
public class GoogleCloudContextAutoConfiguration {


	@Autowired
	private GoogleCloudProperties googleCloudProperties;

	@Bean
	public GoogleCredentials googleCredentials(ResourceLoader resourceLoader)
			throws Exception {
		if (!StringUtils.isEmpty(this.googleCloudProperties.getJsonKeyFile())) {
			return GoogleCredentials.fromStream(new FileInputStream(resourceLoader
					.getResource(this.googleCloudProperties.getJsonKeyFile())
					.getFile()));
		}
		else {
			return GoogleCredentials.getApplicationDefault();
		}
	}
}
