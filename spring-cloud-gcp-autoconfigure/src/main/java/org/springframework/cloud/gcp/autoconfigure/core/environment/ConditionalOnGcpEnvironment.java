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

package org.springframework.cloud.gcp.autoconfigure.core.environment;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cloud.gcp.core.GcpEnvironment;
import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional} that matches based on GCP environment in which the application
 * currently runs.
 *
 * Supported environments are enumerated in {@link GcpEnvironment}.
 *
 * @author Elena Felder
 *
 * @since 1.1
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnGcpEnvironmentCondition.class)
public @interface ConditionalOnGcpEnvironment {

	/**
	 * Configures which environment to match.
	 * @return an array of GCP Environments.
	 */
	GcpEnvironment[] value();

}
