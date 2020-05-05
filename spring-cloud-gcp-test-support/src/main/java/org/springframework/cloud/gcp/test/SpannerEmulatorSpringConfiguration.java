package org.springframework.cloud.gcp.test;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

@Configuration
@Order(-1000)
public class SpannerEmulatorSpringConfiguration {

  private SpannerEmulatorHelper emulatorHelper = new SpannerEmulatorHelper();

  @Bean
  public SpannerOptions spannerOptions() {
    return SpannerOptions.newBuilder()
        .setCredentials(NoCredentials.getInstance())
        .setEmulatorHost("localhost:9010")
        .build();
  }

  @Bean (destroyMethod = "")
  public Spanner spanner(SpannerOptions spannerOptions) throws IOException, InterruptedException {
    emulatorHelper.startEmulator();
    return spannerOptions.getService();
  }

  @EventListener
  public void afterCloseEvent(ContextClosedEvent event) {
    emulatorHelper.shutdownEmulator();
  }
}
