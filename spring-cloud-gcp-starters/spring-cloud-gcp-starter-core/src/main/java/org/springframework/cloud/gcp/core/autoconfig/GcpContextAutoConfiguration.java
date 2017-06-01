package org.springframework.cloud.gcp.core.autoconfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.core.GcpProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.google.auth.oauth2.GoogleCredentials;

/**
 *
 * Base starter for Google Cloud Projects. Provide defaults for {@link GoogleCredentials}.
 * Binds properties from {@link GcpProperties}
 *
 * @author Vinicius Carvalho
 */
@Configuration
@ConditionalOnClass(GoogleCredentials.class)
@EnableConfigurationProperties(GcpProperties.class)
public class GcpContextAutoConfiguration {

	@Autowired
	private GcpProperties gcpProperties;

	@Bean
	public GoogleCredentials googleCredentials() throws Exception {
		if (!StringUtils.isEmpty(this.gcpProperties.getCredentialsLocation())) {
			return GoogleCredentials.fromStream(
					this.gcpProperties.getCredentialsLocation().getInputStream());
		}
		else {
			return GoogleCredentials.getApplicationDefault();
		}
	}

}
