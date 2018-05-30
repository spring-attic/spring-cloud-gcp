package org.springframework.cloud.gcp.autoconfigure.config;

import static org.junit.Assert.*;

import java.util.Base64;
import org.junit.Test;
import org.springframework.cloud.gcp.autoconfigure.config.GoogleConfigEnvironment.Variable;

public class GoogleConfigEnvironmentTest {

  @Test
  public void testSetVariabeValue() {
    GoogleConfigEnvironment.Variable var = new Variable();
    String value = "v a l u e";
    String encodedString = Base64.getEncoder().encodeToString(value.getBytes());
    var.setValue(encodedString);
    assertEquals(value, var.getValue());
  }
}