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

package com.example;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.output.TeeOutputStream;
import org.awaitility.Awaitility;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Tests for the Firestore sample application.
 *
 * @author Dmitry Solomakha
 *
 * @since 1.2
 */

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
public class FirestoreSampleAppTests {
	private static final int TIMEOUT = 10;
	private static PrintStream systemOut;

	private static ByteArrayOutputStream baos;

	@BeforeClass
	public static void prepare() {
		assumeThat(
				"Firestore-sample tests are disabled. Please use '-Dit.firestore=true' "
						+ "to enable them. ",
				System.getProperty("it.firestore"), is("true"));

		systemOut = System.out;
		baos = new ByteArrayOutputStream();
		TeeOutputStream out = new TeeOutputStream(systemOut, baos);
		System.setOut(new PrintStream(out));
	}

	@AfterClass
	public static void bringBack() {
		System.setOut(systemOut);
	}

	@Test
	public void testSample() {
		String expectedString =
				"read: {name=Ada, phones=[123, 456]}\n" +
				"read: User{name='Joe', phones=[Phone{number=12345, type=CELL}, Phone{number=54321, type=WORK}]}";

		Awaitility.await()
				.atMost(TIMEOUT, TimeUnit.SECONDS)
				.until(() -> baos.toString().contains(expectedString));

		//the following two lines appear in a non-deterministic order, so checking them independently
		Awaitility.await()
				.atMost(TIMEOUT, TimeUnit.SECONDS)
				.until(() -> baos.toString().contains("removing: ada"));
		Awaitility.await()
				.atMost(TIMEOUT, TimeUnit.SECONDS)
				.until(() -> baos.toString().contains("removing: joe"));
	}
}
