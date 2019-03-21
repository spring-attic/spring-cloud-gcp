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

package org.springframework.cloud.gcp.data.spanner.core.mapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Annotation for a {@link SpannerPersistentEntity} that allows specifying the primary key
 * columns and their order.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PrimaryKey {

	/**
	 * The order of columns that comprise the primary key. Starts at 1 is consecutive.
	 * @return the key order starting with 1 being first.
	 */
	@AliasFor(attribute = "value")
	int keyOrder() default 1;

	/**
	 * The order of columns that comprise the primary key. Starts at 1 is consecutive.
	 * @return the key order starting with 1 being first.
	 */
	@AliasFor(attribute = "keyOrder")
	int value() default 1;
}
