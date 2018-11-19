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

package org.springframework.cloud.gcp.core;

import java.util.function.BooleanSupplier;

import com.google.cloud.MetadataConfig;
import com.google.cloud.ServiceOptions;

/**
 * {@code COMPUTE_ENGINE} matches instances of Compute Engine that are not also AppEngine Flexible.
 * {@code ANY_APP_ENGINE} matches both, AppEngine Flexible and AppEngine Standard.
 * {@code ANY_CONTAINER} matches both, Compute Engine and Kubernetes Engine.
 */
public enum GcpEnvironment {
	KUBERNETES_ENGINE(() -> System.getenv("KUBERNETES_SERVICE_HOST") != null),
	APP_ENGINE_FLEXIBLE(() -> System.getenv("GAE_INSTANCE") != null),
	APP_ENGINE_STANDARD(() -> ServiceOptions.getAppEngineAppId() != null),
	COMPUTE_ENGINE(() -> MetadataConfig.getInstanceId() != null && !APP_ENGINE_FLEXIBLE.matches()),
	ANY_APP_ENGINE(() -> APP_ENGINE_FLEXIBLE.matches() || APP_ENGINE_STANDARD.matches()),
	ANY_CONTAINER(() ->  KUBERNETES_ENGINE.matches() || COMPUTE_ENGINE.matches());

	private BooleanSupplier matchCondition;

	GcpEnvironment(BooleanSupplier matchCondition) {
		this.matchCondition = matchCondition;
	}

	public boolean matches() {
		return this.matchCondition.getAsBoolean();
	}
}
