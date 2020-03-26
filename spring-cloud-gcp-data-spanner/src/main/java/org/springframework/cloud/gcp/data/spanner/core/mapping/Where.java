/*
 * Copyright 2017-2020 the original author or authors.
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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Where clause to add to the element Entity or target entity of a collection.
 * The clause is written in SQL. A common use case here is for soft-deletes.
 * It can be used on class level or on interleaved list as well.
 * It overwrites the class-level `@Where` annotation when used on an interleaved list property of the same type.
 *
 * @author Roman Solodovnichenko
 *
 * @since 1.2.2
 */
@Target({TYPE, FIELD})
@Retention(RUNTIME)
public @interface Where {
	/**
	 * The where-clause predicate.
	 * @return SQL where cause
	 */
	String value();
}
