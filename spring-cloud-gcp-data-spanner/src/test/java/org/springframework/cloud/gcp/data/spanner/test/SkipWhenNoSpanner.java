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

package org.springframework.cloud.gcp.data.spanner.test;

import com.google.cloud.spanner.DatabaseAdminClient;
import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * @author Balint Pato
 */
public class SkipWhenNoSpanner implements TestRule {

	private final DatabaseAdminClient databaseAdminClient;

	private final String instanceId;

	SkipWhenNoSpanner(DatabaseAdminClient databaseAdminClient, String instanceId) {
		this.databaseAdminClient = databaseAdminClient;
		this.instanceId = instanceId;
	}

	@Override
	public Statement apply(Statement base, Description description) {
		DatabaseAdminClient databaseAdminClient = this.databaseAdminClient; // for checkstyle...
		String instanceId = this.instanceId; // for checkstyle...
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				// tests are going to fail if -DforceIntegrationTests=true and no correct Spanner
				// setup exists, this is good for CI environments
				if (!"true".equals(System.getenv("forceIntegrationTests"))) {
					testSpannerConnection(databaseAdminClient, instanceId);
				}
				System.out.println("EVAL!!! " + description.toString());
				base.evaluate();
			}
		};
	}

	private void testSpannerConnection(DatabaseAdminClient databaseAdminClient, String instanceId) {
		try {
			databaseAdminClient.listDatabases(instanceId);
		}
		catch (Exception e) {
			System.out.println("WARNING: Could not create a valid connection to Google Cloud Spanner! "
					+ "Skipping integration tests. \nThis is not a failed test, if you want integration "
					+ "tests to fail, use -DforceIntegrationTests=true!");
			throw new AssumptionViolatedException(
					"Couldn't connect to Spanner! Skipping integration tests.",
					e);
		}
	}
}
