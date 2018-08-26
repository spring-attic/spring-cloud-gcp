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

package com.example;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

/**
 * @author Chengyuan Zhao
 */

/*
 * This tests verifies that the datastore-sample works. In order to run it, use the
 *
 * -Dit.datastore=true -Dspring.cloud.gcp.sql.database-name=[...]
 * -Dspring.cloud.gcp.datastore.namespace=[...]
 */
@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application-test.properties")
@SpringBootTest(classes = { DatastoreRepositoryExample.class })
public class ApplicationTests {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Autowired
	private CommandLineRunner commandLineRunner;

	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"Datastore-sample integration tests are disabled. Please use '-Dit.datastore=true' "
						+ "to enable them. ",
				System.getProperty("it.datastore"), is("true"));
	}

	@Test
	public void basicTest() throws Exception {
		// we need to run the command line runner again to capture output
		this.commandLineRunner.run();

		assertTrue(this.outputCapture.toString().contains("This concludes the sample."));
	}
}
