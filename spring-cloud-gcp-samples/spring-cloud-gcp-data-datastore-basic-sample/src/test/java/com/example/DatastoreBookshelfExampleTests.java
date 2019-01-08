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

package com.example;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Tests for the Book Shelf sample app.
 *
 * @author Dmitry Solomakha
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = DatastoreBookshelfExample.class,
		properties = { InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=" + false })
public class DatastoreBookshelfExampleTests {

	@Autowired
	private Shell shell;

	@Autowired
	private DatastoreTemplate datastoreTemplate;

	@After
	public void cleanUp() {
		this.datastoreTemplate.deleteAll(Book.class);
	}

	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"Datastore-sample integration tests are disabled. Please use '-Dit.datastore=true' "
						+ "to enable them. ",
				System.getProperty("it.datastore"), is("true"));
	}

	@Test
	public void testSaveBook() {
		String book1 = (String) this.shell.evaluate(() -> "save-book book1 author1 1984");
		String book2 = (String) this.shell.evaluate(() -> "save-book book2 author2 2000");

		String allBooks = (String) this.shell.evaluate(() -> "find-all-books");
		assertThat(allBooks).containsSequence(book1);
		assertThat(allBooks).containsSequence(book2);

		assertThat(this.shell.evaluate(() -> "find-by-author author1")).isEqualTo("[" + book1 + "]");

		assertThat(this.shell.evaluate(() -> "find-by-author-year author2 2000")).isEqualTo("[" + book2 + "]");

		assertThat(this.shell.evaluate(() -> "find-by-year-greater-than 1985")).isEqualTo("[" + book2 + "]");

		this.shell.evaluate(() -> "remove-all-books");

		assertThat(this.shell.evaluate(() -> "find-all-books")).isEqualTo("[]");
	}

}
