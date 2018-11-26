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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GcpEnvironmentTests {

	@Test
	public void testSimpleEnvironments() {
		assertTrue(GcpEnvironment.APP_ENGINE_FLEXIBLE.matchesSimpleEnvironment(GcpEnvironment.APP_ENGINE_FLEXIBLE));
		assertFalse(GcpEnvironment.APP_ENGINE_FLEXIBLE.matchesSimpleEnvironment(GcpEnvironment.APP_ENGINE_STANDARD));
		assertFalse(GcpEnvironment.APP_ENGINE_FLEXIBLE.matchesSimpleEnvironment(GcpEnvironment.KUBERNETES_ENGINE));
		assertFalse(GcpEnvironment.APP_ENGINE_FLEXIBLE.matchesSimpleEnvironment(GcpEnvironment.COMPUTE_ENGINE));
		assertFalse(GcpEnvironment.APP_ENGINE_FLEXIBLE.matchesSimpleEnvironment(GcpEnvironment.UNKNOWN));

		assertTrue(GcpEnvironment.APP_ENGINE_STANDARD.matchesSimpleEnvironment(GcpEnvironment.APP_ENGINE_STANDARD));
		assertFalse(GcpEnvironment.APP_ENGINE_STANDARD.matchesSimpleEnvironment(GcpEnvironment.APP_ENGINE_FLEXIBLE));
		assertFalse(GcpEnvironment.APP_ENGINE_STANDARD.matchesSimpleEnvironment(GcpEnvironment.KUBERNETES_ENGINE));
		assertFalse(GcpEnvironment.APP_ENGINE_STANDARD.matchesSimpleEnvironment(GcpEnvironment.COMPUTE_ENGINE));
		assertFalse(GcpEnvironment.APP_ENGINE_STANDARD.matchesSimpleEnvironment(GcpEnvironment.UNKNOWN));

		assertTrue(GcpEnvironment.KUBERNETES_ENGINE.matchesSimpleEnvironment(GcpEnvironment.KUBERNETES_ENGINE));
		assertFalse(GcpEnvironment.KUBERNETES_ENGINE.matchesSimpleEnvironment(GcpEnvironment.APP_ENGINE_STANDARD));
		assertFalse(GcpEnvironment.KUBERNETES_ENGINE.matchesSimpleEnvironment(GcpEnvironment.APP_ENGINE_FLEXIBLE));
		assertFalse(GcpEnvironment.KUBERNETES_ENGINE.matchesSimpleEnvironment(GcpEnvironment.COMPUTE_ENGINE));
		assertFalse(GcpEnvironment.KUBERNETES_ENGINE.matchesSimpleEnvironment(GcpEnvironment.UNKNOWN));

		assertTrue(GcpEnvironment.COMPUTE_ENGINE.matchesSimpleEnvironment(GcpEnvironment.COMPUTE_ENGINE));
		assertFalse(GcpEnvironment.COMPUTE_ENGINE.matchesSimpleEnvironment(GcpEnvironment.APP_ENGINE_STANDARD));
		assertFalse(GcpEnvironment.COMPUTE_ENGINE.matchesSimpleEnvironment(GcpEnvironment.APP_ENGINE_FLEXIBLE));
		assertFalse(GcpEnvironment.COMPUTE_ENGINE.matchesSimpleEnvironment(GcpEnvironment.KUBERNETES_ENGINE));
		assertFalse(GcpEnvironment.KUBERNETES_ENGINE.matchesSimpleEnvironment(GcpEnvironment.UNKNOWN));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCompositeEnvironmentCheckDisallowedGceGke() {
		GcpEnvironment.APP_ENGINE_FLEXIBLE.matchesSimpleEnvironment(GcpEnvironment.GKE_OR_GCE);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCompositeEnvironmentCheckDisallowedAppEngine() {
		GcpEnvironment.APP_ENGINE_FLEXIBLE.matchesSimpleEnvironment(GcpEnvironment.ANY_APP_ENGINE);
	}

	@Test
	public void testUnknownMatchesNothing() {
		assertFalse(GcpEnvironment.UNKNOWN.matchesSimpleEnvironment(GcpEnvironment.APP_ENGINE_FLEXIBLE));
		assertFalse(GcpEnvironment.UNKNOWN.matchesSimpleEnvironment(GcpEnvironment.APP_ENGINE_STANDARD));
		assertFalse(GcpEnvironment.UNKNOWN.matchesSimpleEnvironment(GcpEnvironment.KUBERNETES_ENGINE));
		assertFalse(GcpEnvironment.UNKNOWN.matchesSimpleEnvironment(GcpEnvironment.COMPUTE_ENGINE));
		assertFalse(GcpEnvironment.UNKNOWN.matchesSimpleEnvironment(GcpEnvironment.UNKNOWN));
	}

	@Test
	public void testCompositeAppEngineMatchesCorrectly() {
		assertTrue(GcpEnvironment.ANY_APP_ENGINE.matchesSimpleEnvironment(GcpEnvironment.APP_ENGINE_FLEXIBLE));
		assertTrue(GcpEnvironment.ANY_APP_ENGINE.matchesSimpleEnvironment(GcpEnvironment.APP_ENGINE_STANDARD));
		assertFalse(GcpEnvironment.ANY_APP_ENGINE.matchesSimpleEnvironment(GcpEnvironment.KUBERNETES_ENGINE));
		assertFalse(GcpEnvironment.ANY_APP_ENGINE.matchesSimpleEnvironment(GcpEnvironment.COMPUTE_ENGINE));
		assertFalse(GcpEnvironment.ANY_APP_ENGINE.matchesSimpleEnvironment(GcpEnvironment.UNKNOWN));
	}

	@Test
	public void testCompositeGkeGceMatchesCorrectly() {
		assertTrue(GcpEnvironment.GKE_OR_GCE.matchesSimpleEnvironment(GcpEnvironment.KUBERNETES_ENGINE));
		assertTrue(GcpEnvironment.GKE_OR_GCE.matchesSimpleEnvironment(GcpEnvironment.COMPUTE_ENGINE));
		assertFalse(GcpEnvironment.GKE_OR_GCE.matchesSimpleEnvironment(GcpEnvironment.APP_ENGINE_FLEXIBLE));
		assertFalse(GcpEnvironment.GKE_OR_GCE.matchesSimpleEnvironment(GcpEnvironment.APP_ENGINE_STANDARD));
		assertFalse(GcpEnvironment.GKE_OR_GCE.matchesSimpleEnvironment(GcpEnvironment.UNKNOWN));
	}
}
