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

import java.util.Set;
import java.util.stream.Collectors;

import com.google.cloud.spanner.KeySet;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@TestPropertySource("classpath:application.properties")
@SpringBootTest(classes = { SpannerExampleDriver.class })
public class SpannerTemplateTests {
	@Autowired
	private SpannerOperations spannerOperations;

	@Autowired
	private SpannerSchemaUtils spannerSchemaUtils;

	@Autowired
	private SpannerTemplateExample spannerTemplateExample;

	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"Spanner integration tests are disabled. "
						+ "Please use '-Dit.spanner=true' to enable them. ",
				System.getProperty("it.spanner"), is("true"));
	}

	@Before
	@After
	public void cleanupSpannerTables() {
		this.spannerTemplateExample.createTablesIfNotExists();
		this.spannerOperations.delete(Trader.class, KeySet.all());
		this.spannerOperations.delete(Trade.class, KeySet.all());
	}

	@Test
	public void testSpannerTemplateLoadsData() {
		assertThat(this.spannerOperations.readAll(Trade.class)).isEmpty();

		this.spannerTemplateExample.runExample();

		Set<String> tradeSpannerKeys = this.spannerOperations.readAll(Trade.class)
				.stream()
				.map(t -> this.spannerSchemaUtils.getKey(t).toString())
				.collect(Collectors.toSet());

		assertThat(tradeSpannerKeys).containsExactlyInAnyOrder(
				"[template_trader1,1]",
				"[template_trader1,2]",
				"[template_trader2,1]");
	}
}
