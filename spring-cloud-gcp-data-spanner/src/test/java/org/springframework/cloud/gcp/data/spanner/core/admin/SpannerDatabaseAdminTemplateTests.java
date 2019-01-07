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

package org.springframework.cloud.gcp.data.spanner.core.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.paging.Page;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.DatabaseInfo.State;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Value;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.database.v1.UpdateDatabaseDdlMetadata;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the Spanner database admin template.
 *
 * @author Chengyuan Zhao
 */
public class SpannerDatabaseAdminTemplateTests {

	private SpannerDatabaseAdminTemplate spannerDatabaseAdminTemplate;

	private DatabaseAdminClient databaseAdminClient;

	private DatabaseClient databaseClient;

	private DatabaseId databaseId;

	private Page<Database> mockDatabasePage;

	private List<String> ddlList;

	@Before
	public void setup() {
		this.databaseAdminClient = mock(DatabaseAdminClient.class);
		this.databaseClient = mock(DatabaseClient.class);
		this.mockDatabasePage = mock(Page.class);
		this.databaseId = DatabaseId.of("fakeproject", "fakeinstance", "fakedb");
		this.spannerDatabaseAdminTemplate = new SpannerDatabaseAdminTemplate(
				this.databaseAdminClient, this.databaseClient, this.databaseId);
		this.ddlList = new ArrayList<>();
		this.ddlList.add("describe Something");

		when(this.databaseAdminClient.listDatabases("fakeinstance")).thenReturn(this.mockDatabasePage);
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
		when(results.next()).thenAnswer((invocation) -> mockResults.next());
		when(results.getCurrentRowAsStruct())
				.thenAnswer((invocation) -> mockResults.getCurrent());
		when(this.databaseClient.singleUse()).thenReturn(readContext);
		when(readContext.executeQuery(any())).thenReturn(results);

		Map<String, Set<String>> relationships = this.spannerDatabaseAdminTemplate
				.getParentChildTablesMap();

		assertThat(relationships).hasSize(2);
		assertThat(relationships.get("grandpa")).containsExactlyInAnyOrder("parent_a", "parent_b");
		assertThat(relationships.get("parent_a")).containsExactlyInAnyOrder("child");

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

	@Test
	public void executeDdlStrings_createsDatabaseIfMissing() throws Exception {
		when(this.mockDatabasePage.getValues()).thenReturn(Arrays.asList(
				new Database(this.databaseId, State.READY, this.databaseAdminClient)));

		OperationFuture<Void, UpdateDatabaseDdlMetadata> mockFuture = mock(OperationFuture.class);
		when(this.databaseAdminClient.updateDatabaseDdl("fakeinstance", "fakedb", this.ddlList, null)).thenReturn(mockFuture);
		when(mockFuture.get()).thenReturn(null);

		this.spannerDatabaseAdminTemplate.executeDdlStrings(this.ddlList, true);

		verify(this.databaseAdminClient, times(0)).createDatabase("fakeinstance", "fakedb", Arrays.asList("describe Something"));
		verify(this.databaseAdminClient).updateDatabaseDdl("fakeinstance", "fakedb", this.ddlList, null);
	}

	@Test
	public void executeDdlStrings_doesNotCreateDatabaseIfAlreadyPresent() throws Exception {
		when(this.mockDatabasePage.getValues()).thenReturn(Arrays.asList());

		OperationFuture<Database, CreateDatabaseMetadata> mockFuture = mock(OperationFuture.class);
		when(this.databaseAdminClient.createDatabase("fakeinstance", "fakedb", this.ddlList)).thenReturn(mockFuture);
		when(mockFuture.get()).thenReturn(null);

		this.spannerDatabaseAdminTemplate.executeDdlStrings(this.ddlList, true);

		verify(this.databaseAdminClient).createDatabase("fakeinstance", "fakedb", Arrays.asList("describe Something"));
		verify(this.databaseAdminClient, times(0)).updateDatabaseDdl("fakeinstance", "fakedb", this.ddlList, null);
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
