/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.spanner.repository.query;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.QueryAnnotation;

/**
 * Annotation used in user-defined repositories to provide SQL for custom Query Methods.
 *
 * @author Balint Pato
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Documented
@QueryAnnotation
public @interface Query {
	/**
	 * Takes a Cloud Spanner SQL string to define the actual query to be executed. This one will
	 * take precedence over the method name then.
	 *
	 * @return the SQL Cloud Spanner query string.
	 */
	String value() default "";

	/**
	 * Indicates if the annotated Query Method is a DML statement or an SQL statement.
	 * @return {@code false} if the query method is a read-only SQL query. {@code true} if the
	 * query method is executed as a DML query.
	 */
	boolean dmlStatement() default false;
}
