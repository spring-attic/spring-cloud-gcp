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

package org.springframework.cloud.gcp.data.datastore.it;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.awaitility.Awaitility;

/**
 * Abstract base class for integration tests.
 *
 * @author Chengyuan Zhao
 */
public abstract class AbstractDatastoreIntegrationTests {

	// queries are eventually consistent, so we may need to retry a few times.
	private static final int QUERY_WAIT_INTERVAL_SECONDS = 15;

	protected long waitUntilTrue(Supplier<Boolean> condition) {
		long startTime = System.currentTimeMillis();
		Awaitility.await().atMost(QUERY_WAIT_INTERVAL_SECONDS, TimeUnit.SECONDS).until(condition::get);

		return System.currentTimeMillis() - startTime;
	}
}
