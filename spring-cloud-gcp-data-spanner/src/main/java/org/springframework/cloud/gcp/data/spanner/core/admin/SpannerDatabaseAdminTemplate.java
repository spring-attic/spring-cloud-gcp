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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.util.Assert;

/**
 * @author Chengyuan Zhao
 * @author Balint Pato
 *
 * @since 1.1
 */
public class SpannerDatabaseAdminTemplate {

	private static final String TABLE_NAME_COL_NAME = "table_name";

	private static final String PARENT_TABLE_NAME_COL_NAME = "parent_table_name";

	private static final Statement TABLE_AND_PARENT_QUERY = Statement
			.of("SELECT t." + TABLE_NAME_COL_NAME + ", t." + PARENT_TABLE_NAME_COL_NAME
					+ " FROM information_schema.tables AS t");

	private final DatabaseAdminClient databaseAdminClient;

	private final DatabaseId databaseId;

	private final DatabaseClient databaseClient;

	/**
	 * Constructor that takes in the database admin client used to perform operations and
	 * the {@link DatabaseId} object holding the project, instance, and database IDs used
	 * for all operations. While operations can be optionally performed for a database
	 * that does not yet exist, the project and instance IDs must already exist for
	 * Spanner.
	 * @param databaseAdminClient the client used to create databases and execute DDL
	 * statements.
	 * @param databaseClient the client used to access schema information tables.
	 * @param databaseId the combination of Cloud Spanner Instance Id and Database Id. While
	 * databases can be created automatically by this template, instances determine
	 * billing and are not created automatically.
	 */
	public SpannerDatabaseAdminTemplate(DatabaseAdminClient databaseAdminClient,
			DatabaseClient databaseClient,
			DatabaseId databaseId) {
		Assert.notNull(databaseAdminClient, "A valid database admin client is required.");
		Assert.notNull(databaseId, "A valid database ID is required.");
		Assert.notNull(databaseClient, "A valid database client is required.");
		this.databaseAdminClient = databaseAdminClient;
		this.databaseId = databaseId;
		this.databaseClient = databaseClient;
	}

	/**
	 * Execute the given DDL strings in order and creates the database if it does not
	 * exist.
	 * @param ddlStrings the DDL strings
	 * @param createDatabase if {@code true}, then the database is created at the same
	 * time as the tables using the DDL strings. if {@code false}, then the database must
	 * already exist.
	 */
	public void executeDdlStrings(Iterable<String> ddlStrings,
			boolean createDatabase) {
		try {
			if (createDatabase) {
				this.databaseAdminClient
						.createDatabase(getInstanceId(), getDatabase(), ddlStrings)
						.waitFor();
			}
			else {
				this.databaseAdminClient.updateDatabaseDdl(getInstanceId(), getDatabase(),
						ddlStrings, null).waitFor();
			}
		}
		catch (SpannerException e) {
			throw new SpannerDataException("DDL could not be executed", e);
		}
	}

	/**
	 * Get the instance ID used to perform database operations.
	 * @return the instance ID string.
	 */
	public String getInstanceId() {
		return this.databaseId.getInstanceId().getInstance();
	}

	/**
	 * Get the database ID used to perform database operations.
	 * @return the database ID string.
	 */
	public String getDatabase() {
		return this.databaseId.getDatabase();
	}

	/**
	 * Returns true if the configured database ID refers to an existing database. False
	 * otherwise.
	 * @return true if the database exists, and false if it does not.
	 */
	public boolean databaseExists() {
		for (Database db : this.databaseAdminClient.listDatabases(getInstanceId())
				.getValues()) {
			if (getDatabase().equals(db.getId().getDatabase())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return a map where key is the table name and the value is the parent table name. If
	 * the table name in the key has no parent then the value is null.
	 * @return the map of the table names.
	 */
	public Map<String, String> getChildParentTablesMap() {
		Map<String, String> relationships = new HashMap<>();
		ResultSet results = this.databaseClient.singleUse()
				.executeQuery(TABLE_AND_PARENT_QUERY);
		while (results.next()) {
			Struct row = results.getCurrentRowAsStruct();
			relationships.put(row.getString(TABLE_NAME_COL_NAME),
					row.isNull(PARENT_TABLE_NAME_COL_NAME) ? null
							: row.getString(PARENT_TABLE_NAME_COL_NAME));
		}
		results.close();
		return relationships;
	}

	/**
	 * Return true if the given table names are interleaved as ancestor and descendant.
	 * These may be separated by more than one generation.
	 * @param ancestor the name of the ancestor table
	 * @param descendant the name of the descendant table. this may be a direct child or
	 * further down in the family tree.
	 * @return true the descendant is indeed a descendant table. false otherwise.
	 */
	public boolean isInterleaved(String ancestor, String descendant) {
		Assert.notNull(ancestor, "A non-null ancestor table name is required.");
		Set<String> directChildren = getParentChildTablesMap().get(ancestor);
		if (ancestor.equals(descendant) || directChildren == null) {
			return false;
		}
		for (String child : directChildren) {
			if (child.equals(descendant) || isInterleaved(child, descendant)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return a map of parent and child table relationships in the database at the
	 * moment.
	 * @return A map where the keys are parent table names, and the value is a set of that
	 * parent's children.
	 */
	public Map<String, Set<String>> getParentChildTablesMap() {
		Map<String, Set<String>> relationships = new HashMap<>();
		Map<String, String> childToParent = getChildParentTablesMap();
		for (String child : childToParent.keySet()) {
			if (childToParent.get(child) == null) {
				continue;
			}
			String parent = childToParent.get(child);
			Set<String> children = relationships.get(parent);
			if (children == null) {
				children = new HashSet<>();
				relationships.put(parent, children);
			}
			children.add(child);
		}
		return relationships;
	}

	/**
	 * Return a set of the tables that currently exist in the database.
	 * @return A set of table names.
	 */
	public Set<String> getTables() {
		return getChildParentTablesMap().keySet();
	}

	/**
	 * Returns true if the given table name exists in the database currently.
	 * @param table the name of the table.
	 * @return true if the table exists, false otherwise.
	 */
	public boolean tableExists(String table) {
		return databaseExists() && getTables().contains(table);
	}
}
