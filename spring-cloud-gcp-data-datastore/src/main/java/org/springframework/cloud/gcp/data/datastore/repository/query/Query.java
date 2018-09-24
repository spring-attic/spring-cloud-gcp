/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.data.datastore.repository.query;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.QueryAnnotation;

/**
 * Annotation to mark query methods in user-defined repositories that are supplied with
 * Google Query Language custom queries.
 *
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
	 * Takes a Cloud Datastore GQL string to define the actual query to be executed. This
	 * one will take precedence over the method name then.
	 *
	 * @return the SQL Cloud Datstore query string.
	 */
	String value() default "";

	/**
	 * Returns whether the defined query should be executed as a count projection.
	 *
	 * @return {@code true} if this query method returns a count. {@code false} otherwise
	 */
	boolean count() default false;

	/**
	 * Returns whether the defined query should be executed as an exists projection.
	 *
	 * @return {@code true} if this query method returns an exists boolean. {@code false} otherwise
	 */
	boolean exists() default false;
}
