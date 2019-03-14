/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.datastore.repository.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;

/**
 * The annotation used to activate auditing functionality.
 *
 * @author Chengyuan Zhao
 * @since 1.2
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(DatastoreAuditingRegistrar.class)
public @interface EnableDatastoreAuditing {

	/**
	 * Configures a {@link AuditorAware} bean to be used to lookup the current principal.
	 *
	 * @return the name of a custom auditor provider. If blank then one will be looked up bean
	 * type.
	 */
	String auditorAwareRef() default "";

	/**
	 * Configures whether the creation and modification dates are set. Defaults to
	 * {@literal true}.
	 *
	 * @return whether dates are set by the auditing functionality.
	 */
	boolean setDates() default true;

	/**
	 * Configures whether the entity shall be marked as modified on creation. Defaults to
	 * {@literal true}.
	 *
	 * @return whether an entity is marked as modified when it is created.
	 */
	boolean modifyOnCreate() default true;

	/**
	 * Configures a {@link DateTimeProvider} bean name that allows customizing the
	 * {@link java.time.LocalDateTime} to be used for setting creation and modification dates.
	 *
	 * @return the name of the custom time provider. If blank then one will be looked up bean
	 * type.
	 */
	String dateTimeProviderRef() default "";
}
