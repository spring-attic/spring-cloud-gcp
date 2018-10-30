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

package org.springframework.cloud.gcp.security.iap.claims;

import java.time.Clock;

import com.google.common.annotations.VisibleForTesting;

public abstract class TimeBasedClaimVerifier implements ClaimVerifier {

	private Clock clock = Clock.systemUTC();


	protected Clock getClock() {
		return this.clock;
	}

	@VisibleForTesting
	void setClock(Clock clock) {
		this.clock = clock;
	}
}
