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

package org.springframework.cloud.gcp.data.spanner.core.admin;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseId;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
public class SpannerDatabaseAdminTemplateTests {

	private SpannerDatabaseAdminTemplate spannerDatabaseAdminTemplate;

	private DatabaseAdminClient databaseAdminClient;

	private DatabaseId databaseId;

	@Before
	public void setup() {
		this.databaseAdminClient = mock(DatabaseAdminClient.class);
		this.databaseId = DatabaseId.of("fakeproject", "fakeinstance", "fakedb");
		this.spannerDatabaseAdminTemplate = new SpannerDatabaseAdminTemplate(
				this.databaseAdminClient, this.databaseId);
	}

	@Test
	public void getParentChildTablesMapTest() {
		Database database = mock(Database.class);
		when(this.databaseAdminClient.getDatabase(any(), any())).thenReturn(database);
		when(database.getDdl()).thenReturn(Arrays.asList(new String[] {
				"CREATE TABLE grandpa ( col a, col b ) primary key (a);",
				"CREATE TABLE parent_a ( col a, col b ) primary key (a), "
				+ "INTERLEAVE IN PARENT grandpa ON DELETE CASCADE",
				"CREATE TABLE parent_b ( col a, col b ) primary key (a), "
						+ "INTERLEAVE IN PARENT grandpa",
				"CREATE TABLE child ( col a, col b ) primary key (a), "
						+ "INTERLEAVE IN PARENT parent_a ON DELETE CASCADE" }));

		Map<String, Set<String>> relationships = this.spannerDatabaseAdminTemplate
				.getParentChildTablesMap();

		assertEquals(2, relationships.size());
		assertThat(relationships.get("grandpa"),
				containsInAnyOrder("parent_a", "parent_b"));
		assertThat(relationships.get("parent_a"), containsInAnyOrder("child"));
	}
}
