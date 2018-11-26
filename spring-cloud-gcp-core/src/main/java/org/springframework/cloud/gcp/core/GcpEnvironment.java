/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.core;

import java.util.function.BooleanSupplier;

import com.google.cloud.MetadataConfig;
import com.google.cloud.ServiceOptions;

/**
 * Enumeration of valid individual GCP environments.
 *
 * <p>Also includes meta-members for convenient evaluation of environment groups, such as {@code ANY_APP_ENGINE}.
 *
 * @author Elena Felder
 *
 * @since 1.1
 */
public enum GcpEnvironment {
	/**
	 * Matches Kubernetes instances.
	 */
	KUBERNETES_ENGINE(() -> System.getenv("KUBERNETES_SERVICE_HOST") != null),

	/**
	 * Matches App Engine Flexible instances.
	 */
	APP_ENGINE_FLEXIBLE(() -> System.getenv("GAE_INSTANCE") != null),

	/**
	 * Matches App Engine Standard instances.
	 */
	APP_ENGINE_STANDARD(() -> ServiceOptions.getAppEngineAppId() != null),

	/**
	 * Matches instances of Compute Engine that are not also AppEngine Flexible.
	 */
	COMPUTE_ENGINE(() -> MetadataConfig.getInstanceId() != null && !APP_ENGINE_FLEXIBLE.matches()),

	/**
	 * Matches both, AppEngine Flexible and AppEngine Standard.
	 */
	ANY_APP_ENGINE(() -> APP_ENGINE_FLEXIBLE.matches() || APP_ENGINE_STANDARD.matches()),

	/**
	 * Matches both, Compute Engine and Kubernetes Engine.
	 */
	ANY_CONTAINER(() ->  KUBERNETES_ENGINE.matches() || COMPUTE_ENGINE.matches());

	private BooleanSupplier matchCondition;

	GcpEnvironment(BooleanSupplier matchCondition) {
		this.matchCondition = matchCondition;
	}

	/**
	 * @return whether the specified environment's presence is detected.
	 */
	public boolean matches() {
		return this.matchCondition.getAsBoolean();
	}
}
