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

import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseId;

import org.springframework.util.Assert;

/**
 * @author Chengyuan Zhao
 * @author Balint Pato
 */
public class SpannerDatabaseAdminTemplate {

	private final DatabaseAdminClient databaseAdminClient;

	private final DatabaseId databaseId;

	/**
	 * Constructor.
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
	 * Executes the given DDL strings in order and creates the database if it does not
	 * exist.
	 * @param ddlStrings
	 * @param createDatabaseIfNotExists Has no effect if the database already exists.
	 * Otherwise, if True then the database is created and the DDL strings are executed;
	 * the DDL is not executed if False.
	 */
	public void executeDDLStrings(Iterable<String> ddlStrings,
			boolean createDatabaseIfNotExists) {
		if (!databaseExists() && createDatabaseIfNotExists) {
			this.databaseAdminClient
					.createDatabase(getInstanceId(), getDatabase(), ddlStrings).waitFor();
		}
		else if (databaseExists()) {
			this.databaseAdminClient
					.updateDatabaseDdl(getInstanceId(), getDatabase(), ddlStrings, null)
					.waitFor();
		}
	}

	private String getInstanceId() {
		return this.databaseId.getInstanceId().getInstance();
	}

	private String getDatabase() {
		return this.databaseId.getDatabase();
	}

	/**
	 * Returns true if the configured database ID refers to an existing database. False
	 * otherwise.
	 * @return
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
}
