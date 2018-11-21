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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Value;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
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

	private DatabaseClient databaseClient;

	private DatabaseId databaseId;

	@Before
	public void setup() {
		this.databaseAdminClient = mock(DatabaseAdminClient.class);
		this.databaseClient = mock(DatabaseClient.class);
		this.databaseId = DatabaseId.of("fakeproject", "fakeinstance", "fakedb");
		this.spannerDatabaseAdminTemplate = new SpannerDatabaseAdminTemplate(
				this.databaseAdminClient, this.databaseClient, this.databaseId);
	}

	@Test
	public void getTableRelationshipsTest() {
		ReadContext readContext = mock(ReadContext.class);

		Struct s1 = Struct.newBuilder().set("table_name").to(Value.string("grandpa"))
				.set("parent_table_name").to(Value.string(null)).build();
		Struct s2 = Struct.newBuilder().set("table_name").to(Value.string("parent_a"))
				.set("parent_table_name").to(Value.string("grandpa")).build();
		Struct s3 = Struct.newBuilder().set("table_name").to(Value.string("parent_b"))
				.set("parent_table_name").to(Value.string("grandpa")).build();
		Struct s4 = Struct.newBuilder().set("table_name").to(Value.string("child"))
				.set("parent_table_name").to(Value.string("parent_a")).build();

		MockResults mockResults = new MockResults();
		mockResults.structs = Arrays.asList(s1, s2, s3, s4);
		ResultSet results = mock(ResultSet.class);
		when(results.next()).thenAnswer(invocation -> mockResults.next());
		when(results.getCurrentRowAsStruct())
				.thenAnswer(invocation -> mockResults.getCurrent());
		when(this.databaseClient.singleUse()).thenReturn(readContext);
		when(readContext.executeQuery(any())).thenReturn(results);

		Map<String, Set<String>> relationships = this.spannerDatabaseAdminTemplate
				.getParentChildTablesMap();

		assertEquals(2, relationships.size());
		assertThat(relationships.get("grandpa"),
				containsInAnyOrder("parent_a", "parent_b"));
		assertThat(relationships.get("parent_a"), containsInAnyOrder("child"));

		assertThat(this.spannerDatabaseAdminTemplate.isInterleaved("grandpa", "child"))
				.as("verify grand-child relationship").isTrue();
		assertThat(this.spannerDatabaseAdminTemplate.isInterleaved("grandpa", "parent_a"))
				.as("verify parent-child relationship").isTrue();
		assertThat(this.spannerDatabaseAdminTemplate.isInterleaved("parent_a", "child"))
				.as("verify parent-child relationship").isTrue();
		assertThat(this.spannerDatabaseAdminTemplate.isInterleaved("grandpa", "parent_b"))
				.as("verify parent-child relationship").isTrue();
		assertThat(this.spannerDatabaseAdminTemplate.isInterleaved("parent_a", "parent_b"))
				.as("verify not parent-child relationship").isFalse();
		assertThat(this.spannerDatabaseAdminTemplate.isInterleaved("parent_b", "child"))
				.as("verify not parent-child relationship").isFalse();
	}

	private static class MockResults {
		List<Struct> structs;

		int counter = -1;

		boolean next() {
			if (this.counter < this.structs.size() - 1) {
				this.counter++;
				return true;
			}
			this.counter = -1;
			return false;
		}

		Struct getCurrent() {
			return this.structs.get(this.counter);
		}
	}
}
