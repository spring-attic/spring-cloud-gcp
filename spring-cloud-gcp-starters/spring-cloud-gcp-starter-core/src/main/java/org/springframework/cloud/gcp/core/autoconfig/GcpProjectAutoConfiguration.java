package org.springframework.cloud.gcp.core.autoconfig;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Strings;

/**
 * @author João André Martins
 */
@Configuration
@EnableConfigurationProperties(GcpProject.class)
public class GcpProjectAutoConfiguration {

  private static final Logger LOGGER =
      Logger.getLogger(GcpProjectAutoConfiguration.class.getName());

  private static final String PROJECT_ID_ENV_VAR_NAME = "GCP_PROJECT_ID";

  private GcpProject gcpProjectFromProperties;

  @Autowired
  public GcpProjectAutoConfiguration(GcpProject gcpProjectFromProperties) {
    this.gcpProjectFromProperties = gcpProjectFromProperties;
  }

  @Bean
  public String gcpProjectId() {
    String projectId = System.getProperty(PROJECT_ID_ENV_VAR_NAME);
    if (!Strings.isNullOrEmpty(projectId)) {
      LOGGER.info("Getting project ID from an environment variable.");
      return projectId;
    }

    if (gcpProjectFromProperties != null) {
      LOGGER.info("Getting project ID from the properties file.");
      return gcpProjectFromProperties.getId();
    }

    // TODO(joaomartins): Try to get project ID in credentials.

    // TODO(joaomartins): Try to get project ID from Metadata server.

    return null;
  }
}
