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
	KUBERNETES_ENGINE,

	/**
	 * Matches App Engine Flexible instances.
	 */
	APP_ENGINE_FLEXIBLE,

	/**
	 * Matches App Engine Standard instances.
	 */
	APP_ENGINE_STANDARD,

	/**
	 * Matches instances of Compute Engine that are not also AppEngine Flexible.
	 */
	COMPUTE_ENGINE,

	/**
	 * Matches both, AppEngine Flexible and AppEngine Standard.
	 */
	ANY_APP_ENGINE {
		public boolean matchesSimpleEnvironment(GcpEnvironment env) {
			validateSimpleEnvironment(env);
			return APP_ENGINE_FLEXIBLE == env || APP_ENGINE_STANDARD == env;
		}
	},

	/**
	 * Matches both, Compute Engine and Kubernetes Engine.
	 */
	GKE_OR_GCE {
		public boolean matchesSimpleEnvironment(GcpEnvironment env) {
			validateSimpleEnvironment(env);
			return COMPUTE_ENGINE == env || KUBERNETES_ENGINE == env;
		}
	},

	/**
	 * Matches nothing; environment cannot be identified.
	 */
	UNKNOWN {
		public boolean matchesSimpleEnvironment(GcpEnvironment env) {
			return false;
		}
	};

	/**
	 * @return whether the specified environment's presence is detected.
	 */
	public boolean matchesSimpleEnvironment(GcpEnvironment env) {
		validateSimpleEnvironment(env);
		return this == env;
	}

	private static void validateSimpleEnvironment(GcpEnvironment env) {
		if (env == ANY_APP_ENGINE || env == GKE_OR_GCE) {
			throw new IllegalArgumentException("Composite environments are not allowed here.");
		}
	}

}
