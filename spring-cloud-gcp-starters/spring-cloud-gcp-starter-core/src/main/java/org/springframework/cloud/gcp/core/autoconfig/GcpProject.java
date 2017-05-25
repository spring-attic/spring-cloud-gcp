package org.springframework.cloud.gcp.core.autoconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author João André Martins
 */
@ConfigurationProperties("cloud.gcp.project")
public class GcpProject {

  private String id;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
