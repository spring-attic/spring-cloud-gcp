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

package org.springframework.cloud.gcp.data.spanner.core.mapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.spanner.v1.TypeCode;

/**
 * Annotation for a {@link SpannerPersistentProperty} that allows specifying the column name
 * instead of deriving it from the field's name.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

	/**
	 * The custom name of the column in the Spanner table, which can differ from the name of the
	 * field which it annotates.
	 * @return the name of the column in the Spanner table
	 */
	String name() default "";

	/**
	 * The maximum length of the column in Cloud Spanner terms. For example, for STRING
	 * columns this refers to the number of characters. For BYTES columns this refers to
	 * the number of bytes. This setting is only used when generating schema from an
	 * entity class. A setting of less than 0 indicates an unlimited maximum length.
	 *
	 * @return the maximum length for the column
	 */
	long spannerTypeMaxLength() default -1;

	/**
	 * If the column's schema should be NOT NULL when generating a schema based on an
	 * entity class.
	 * @return {@code false} if the column should be NOT NULL in generated DDL.
	 * {@code true} otherwise.
	 */
	boolean nullable() default true;

	/**
	 * Optionally directly specify the column type in Cloud Spanner. For ARRAY columns
	 * this refers to type of the item the array holds. If this is not specified then it
	 * is inferred from the Java property type.
	 * @return The user-specified column item type.
	 */
	TypeCode spannerType() default TypeCode.TYPE_CODE_UNSPECIFIED;
}
