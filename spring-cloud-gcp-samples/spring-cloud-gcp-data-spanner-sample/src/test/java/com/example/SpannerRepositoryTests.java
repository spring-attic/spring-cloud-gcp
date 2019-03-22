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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerDatabaseAdminTemplate;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Tests for the Spanner repository example.
 *
 * @author Daniel Zou
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@TestPropertySource("classpath:application.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { SpannerExampleDriver.class })
public class SpannerRepositoryTests {
	@LocalServerPort
	private int port;

	@Autowired
	private TraderRepository traderRepository;

	@Autowired
	private TradeRepository tradeRepository;

	@Autowired
	private SpannerSchemaUtils spannerSchemaUtils;

	@Autowired
	private SpannerDatabaseAdminTemplate spannerDatabaseAdminTemplate;

	@Autowired
	private SpannerRepositoryExample spannerRepositoryExample;

	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"Spanner integration tests are disabled. "
						+ "Please use '-Dit.spanner=true' to enable them. ",
				System.getProperty("it.spanner"), is("true"));
	}

	@Before
	@After
	public void cleanupAndSetupTables() {
		this.spannerRepositoryExample.createTablesIfNotExists();
		this.tradeRepository.deleteAll();
		this.traderRepository.deleteAll();
	}

	@Test
	public void testRestEndpoint() {
		this.spannerRepositoryExample.runExample();

		TestRestTemplate testRestTemplate = new TestRestTemplate();
		ResponseEntity<PagedModel<Trade>> tradesResponse = testRestTemplate.exchange(
				String.format("http://localhost:%s/trades/", this.port),
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<PagedModel<Trade>>() {
				});
		assertThat(tradesResponse.getBody().getMetadata().getTotalElements()).isEqualTo(8);
	}

	@Test
	public void testLoadsCorrectData() {
		assertThat(this.traderRepository.count()).isEqualTo(0);
		assertThat(this.tradeRepository.count()).isEqualTo(0);

		this.spannerRepositoryExample.runExample();
		List<String> traderIds = new ArrayList<>();
		this.traderRepository.findAll().forEach((t) -> traderIds.add(t.getTraderId()));
		assertThat(traderIds).containsExactlyInAnyOrder("demo_trader1", "demo_trader2", "demo_trader3");

		assertThat(this.tradeRepository.findAll()).hasSize(8);

		Set<String> tradeSpannerKeys = new HashSet<>();
		this.tradeRepository.findAll().forEach((t) -> tradeSpannerKeys.add(this.spannerSchemaUtils.getKey(t).toString()));

		assertThat(tradeSpannerKeys).containsExactlyInAnyOrder(
				"[demo_trader1,1]",
				"[demo_trader1,2]",
				"[demo_trader1,3]",
				"[demo_trader2,1]",
				"[demo_trader2,2]",
				"[demo_trader2,3]",
				"[demo_trader3,1]",
				"[demo_trader3,2]");

		List<String> buyTradeIds = this.tradeRepository.getTradeIds("BUY");
		assertThat(buyTradeIds).hasSize(5);
	}
}
