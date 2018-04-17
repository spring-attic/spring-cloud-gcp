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
import com.google.cloud.spanner.DatabaseId;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.util.Assert;

/**
 * @author Chengyuan Zhao
 * @author Balint Pato
 */
public class SpannerDatabaseAdminTemplate {

	private final DatabaseAdminClient databaseAdminClient;

	private final DatabaseId databaseId;

	/**
	 * Constructor that takes in the database admin client used to perform operations and the
	 * {@link DatabaseId} object holding the project, instance, and database IDs used for all
	 * operations. While operations can be optionally performed for a database that does not yet
	 * exist, the project and instance IDs must already exist for Spanner.
	 * @param databaseAdminClient the client used to create databases and execute DDL
	 * statements.
	 * @param databaseId the combination of Spanner Instance Id and Database Id. While
	 * databases can be created automatically by this template, instances determine
	 * billing and are not created automatically.
	 */
	public SpannerDatabaseAdminTemplate(DatabaseAdminClient databaseAdminClient,
			DatabaseId databaseId) {
		Assert.notNull(databaseAdminClient, "A valid database admin client is required.");
		Assert.notNull(databaseId, "A valid database ID is required.");
		this.databaseAdminClient = databaseAdminClient;
		this.databaseId = databaseId;
	}

	/**
	 * Execute the given DDL strings in order and creates the database if it does not
	 * exist.
	 * @param ddlStrings
	 * @param createDatabaseIfNotExists Has no effect if the database already exists.
	 * Otherwise, if True then the database is created and the DDL strings are executed;
	 * the DDL is not executed if False.
	 */
	public void executeDdlStrings(Iterable<String> ddlStrings,
			boolean createDatabaseIfNotExists) {
		if (createDatabaseIfNotExists && !databaseExists()) {
			this.databaseAdminClient
					.createDatabase(getInstanceId(), getDatabase(), ddlStrings).waitFor();
		}
		else if (databaseExists()) {
			this.databaseAdminClient
					.updateDatabaseDdl(getInstanceId(), getDatabase(), ddlStrings, null)
					.waitFor();
		}
		else {
			throw new SpannerDataException(
					"DDL could not be executed because the database does"
							+ " not exist and it was not auto-created");
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
	 * Return a map of parent and child table relationships in the database at the
	 * moment.
	 * @return A map where the keys are parent table names, and the value is a set of that
	 * parent's children.
	 */
	public Map<String, Set<String>> getParentChildTablesMap() {
		Map<String, Set<String>> relationships = new HashMap<>();
		for (String ddl : this.databaseAdminClient
				.getDatabase(getInstanceId(), getDatabase()).getDdl()) {
			if (!ddl.contains("INTERLEAVE IN PARENT")
					|| !ddl.startsWith("CREATE TABLE ")) {
				continue;
			}
			String child = ddl.split(" ")[2];
			String parent = ddl.substring(ddl.indexOf("INTERLEAVE IN PARENT"))
					.split(" ")[3];
			Set<String> children = relationships.get(parent);
			if (children == null) {
				children = new HashSet<>();
				relationships.put(parent, children);
			}
			children.add(child);
		}
		return relationships;
	}
}
