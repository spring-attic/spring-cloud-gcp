package org.springframework.cloud.gcp.data.spanner.repository.query;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.data.annotation.QueryAnnotation;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Documented
@QueryAnnotation
public @interface Query {
  /**
   * Takes a Spanner SQL string to define the actual query to be executed. This one will take precedence over the
   * method name then.
   *
   * @return
   */
  String value() default "";

}
