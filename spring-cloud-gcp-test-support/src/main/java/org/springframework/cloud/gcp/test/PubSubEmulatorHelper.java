package org.springframework.cloud.gcp.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PubSubEmulatorHelper extends AbstractEmulatorHelper {
  private static final Log LOGGER = LogFactory.getLog(PubSubEmulatorHelper.class);

  String getGatingPropertyName() {
    return "it.pubsub-emulator";
  }

  String getEmulatorName() {
    return "pubsub";
  }

  @Override
  protected void afterEmulatorDestroyed() {
    String hostPort = getEmulatorHostPort();

    // find destory emulator process spawned by gcloud
    if (hostPort == null) {
      LOGGER.warn("Host/port null after the test.");
    } else {
      int portSeparatorIndex = hostPort.lastIndexOf(":");
      if (portSeparatorIndex < 0) {
        LOGGER.warn("Malformed host: " + hostPort);
        return;
      }

      String emulatorHost = hostPort.substring(0, portSeparatorIndex);
      String emulatorPort = hostPort.substring(portSeparatorIndex + 1);

      String hostPortParams = String.format("--host=%s --port=%s", emulatorHost, emulatorPort);
      killByCommand(hostPortParams);
    }
  }
}
