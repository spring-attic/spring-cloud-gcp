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
		spannerTemplateExample.createTablesIfNotExists();
		this.spannerOperations.delete(Trader.class, KeySet.all());
		this.spannerOperations.delete(Trade.class, KeySet.all());
	}

	@Test
	public void testSpannerTemplateLoadsData() {
		assertThat(this.spannerOperations.readAll(Trade.class)).isEmpty();

		spannerTemplateExample.runExample();

		Set<String> tradeSpannerKeys = this.spannerOperations.readAll(Trade.class)
				.stream()
				.map(t -> spannerSchemaUtils.getKey(t).toString())
				.collect(Collectors.toSet());

		assertThat(tradeSpannerKeys).containsExactlyInAnyOrder(
				"[template_trader1,1]",
				"[template_trader1,2]",
				"[template_trader2,1]");
	}
}
