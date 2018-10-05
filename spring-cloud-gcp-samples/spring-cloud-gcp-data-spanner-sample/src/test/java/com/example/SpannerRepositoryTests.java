package com.example;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
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
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

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
		spannerRepositoryExample.createTablesIfNotExists();
		this.tradeRepository.deleteAll();
		this.traderRepository.deleteAll();
	}

	@Test
	public void testRestEndpoint() {
		spannerRepositoryExample.runExample();

		TestRestTemplate testRestTemplate = new TestRestTemplate();
		ResponseEntity<PagedResources<Trade>> tradesResponse = testRestTemplate.exchange(
				String.format("http://localhost:%s/trades/", port),
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<PagedResources<Trade>>() {
				});
		assertThat(tradesResponse.getBody().getMetadata().getTotalElements()).isEqualTo(8);
	}

	@Test
	public void testLoadsCorrectData() {
		assertThat(traderRepository.count()).isEqualTo(0);
		assertThat(tradeRepository.count()).isEqualTo(0);

		spannerRepositoryExample.runExample();
		List<String> traderIds = ImmutableList.copyOf(traderRepository.findAll())
				.stream()
				.map(Trader::getTraderId)
				.collect(Collectors.toList());
		assertThat(traderIds).containsExactlyInAnyOrder("demo_trader1", "demo_trader2", "demo_trader3");

		List<Trade> actualTrades = ImmutableList.copyOf(tradeRepository.findAll());
		assertThat(actualTrades).hasSize(8);

		Set<String> tradeSpannerKeys = actualTrades.stream()
				.map(t -> spannerSchemaUtils.getKey(t).toString())
				.collect(Collectors.toSet());
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
