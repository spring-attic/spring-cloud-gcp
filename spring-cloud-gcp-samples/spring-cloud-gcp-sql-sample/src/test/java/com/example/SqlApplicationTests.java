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

import java.util.List;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Simple integration test to verify the SQL sample application.
 *
 * Run with: mvn -Dit.cloudsql -Dspring.cloud.gcp.sql.database-name=[...]
 * -Dspring.cloud.gcp.sql.instance-connection-name=[...]
 * -Dspring.datasource.password=[...] test
 *
 * @author Daniel Zou
 */
@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application-test.properties")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = { SqlApplication.class })
public class SqlApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"SQL sample integration tests are disabled. Please use '-Dit.cloudsql=true' "
						+ "to enable them. ",
				System.getProperty("it.cloudsql"), is("true"));
	}

	@After
	public void clearTable() {
		jdbcTemplate.execute("DROP TABLE IF EXISTS users");
	}

	@Test
	public void testSqlRowsAccess() {
		String url = String.format("http://localhost:%s/getTuples", port);
		ResponseEntity<List<String>> result = testRestTemplate.exchange(
				url, HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>() {
				});

		assertThat(result.getBody()).containsExactlyInAnyOrder(
				"[luisao@example.com, Anderson, Silva]",
				"[jonas@example.com, Jonas, Goncalves]",
				"[fejsa@example.com, Ljubomir, Fejsa]");
	}
}
