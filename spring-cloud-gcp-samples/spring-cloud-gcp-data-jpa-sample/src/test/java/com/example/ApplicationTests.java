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
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

/**
 * This test verifies that the jpa-sample works.
 *
 * Run with: mvn -Dit.cloudsql test
 *
 * The test will inherit the properties set in resources/application.properties.
 *
 * @author Mike Eltsufin
 * @author Dmitry Solomakha
 * @author Daniel Zou
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApplication.class})
public class ApplicationTests {
	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"JPA-sample integration tests are disabled. Please use '-Dit.cloudsql=true' "
						+ "to enable them. ",
				System.getProperty("it.cloudsql"), is("true"));
	}

	@Autowired
	private CommandLineRunner commandLineRunner;

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void basicTest() throws Exception {
		// we need to run the command line runner again to capture output
		this.commandLineRunner.run();

		assertTrue(this.outputCapture.toString().contains("Number of houses is 4"));
		assertTrue(this.outputCapture.toString().contains("636 Avenue of the Americas, NYC"));
	}
}
