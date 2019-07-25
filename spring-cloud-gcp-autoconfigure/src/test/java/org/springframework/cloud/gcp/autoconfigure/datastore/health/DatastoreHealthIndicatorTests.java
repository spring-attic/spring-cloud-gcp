/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.datastore.health;

import com.google.cloud.datastore.Datastore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DatastoreHealthIndicator}.
 *
 * @author Mike Eltsufin
 *
 * @since 1.2
 */
@RunWith(MockitoJUnitRunner.class)
public class DatastoreHealthIndicatorTests {

	@Mock
	private Datastore datastore;

	@Test
	public void testdoHealthCheckUp() throws Exception {
		DatastoreHealthIndicator datastoreHealthIndicator = new DatastoreHealthIndicator(() -> datastore);

		when(datastore.run(any())).thenReturn(null);

		Health.Builder builder = new Health.Builder();

		datastoreHealthIndicator.doHealthCheck(builder);

		assertThat(builder.build().getStatus()).isSameAs(Status.UP);
	}

	@Test(expected = Exception.class)
	public void testdoHealthCheckDown() throws Exception {
		DatastoreHealthIndicator datastoreHealthIndicator = new DatastoreHealthIndicator(() -> datastore);

		when(datastore.run(any())).thenThrow(new RuntimeException("Cloud Datastore is down!!!"));

		Health.Builder builder = new Health.Builder();

		datastoreHealthIndicator.doHealthCheck(builder);
	}

	@Test
	public void testHealthy()  {
		DatastoreHealthIndicator datastoreHealthIndicator = new DatastoreHealthIndicator(() -> datastore);

		when(datastore.run(any())).thenReturn(null);

		assertThat(datastoreHealthIndicator.health().getStatus()).isSameAs(Status.UP);
	}

	@Test
	public void testUnhealthy() {
		DatastoreHealthIndicator datastoreHealthIndicator = new DatastoreHealthIndicator(() -> datastore);

		when(datastore.run(any())).thenThrow(new RuntimeException("Cloud Datastore is down!!!"));

		assertThat(datastoreHealthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
	}
}
