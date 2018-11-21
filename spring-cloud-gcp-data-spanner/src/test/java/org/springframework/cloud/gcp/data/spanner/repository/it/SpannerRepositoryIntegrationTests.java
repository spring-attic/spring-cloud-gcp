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

package org.springframework.cloud.gcp.data.spanner.repository.it;

import java.util.ArrayList;
import java.util.List;

import com.google.cloud.spanner.Struct;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.spanner.test.AbstractSpannerIntegrationTest;
import org.springframework.cloud.gcp.data.spanner.test.domain.SubTrade;
import org.springframework.cloud.gcp.data.spanner.test.domain.SubTradeComponent;
import org.springframework.cloud.gcp.data.spanner.test.domain.SubTradeComponentRepository;
import org.springframework.cloud.gcp.data.spanner.test.domain.SubTradeRepository;
import org.springframework.cloud.gcp.data.spanner.test.domain.SymbolAction;
import org.springframework.cloud.gcp.data.spanner.test.domain.Trade;
import org.springframework.cloud.gcp.data.spanner.test.domain.TradeProjection;
import org.springframework.cloud.gcp.data.spanner.test.domain.TradeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
public class SpannerRepositoryIntegrationTests extends AbstractSpannerIntegrationTest {

	@Autowired
	TradeRepository tradeRepository;

	@Autowired
	SubTradeRepository subTradeRepository;

	@Autowired
	SubTradeComponentRepository subTradeComponentRepository;

	@Autowired
	TradeRepositoryTransactionalService tradeRepositoryTransactionalService;

	@Test
	public void queryMethodsTest() {
		this.tradeRepository.deleteAll();
		List<Trade> trader1BuyTrades = insertTrades("trader1", "BUY", 3);
		List<Trade> trader1SellTrades = insertTrades("trader1", "SELL", 2);
		List<Trade> trader2Trades = insertTrades("trader2", "SELL", 3);

		List<Trade> allTrades = new ArrayList<>();
		allTrades.addAll(trader1BuyTrades);
		allTrades.addAll(trader1SellTrades);
		allTrades.addAll(trader2Trades);

		assertThat(this.tradeRepository.count(), is(8L));

		assertThat(this.tradeRepository.deleteByAction("BUY"), is(3));

		assertThat(this.tradeRepository.count(), is(5L));

		List<Trade> deletedBySymbol = this.tradeRepository.deleteBySymbol("ABCD");

		assertThat(deletedBySymbol.size(), is(5));

		assertThat(this.tradeRepository.count(), is(0L));

		this.tradeRepository.saveAll(deletedBySymbol);

		assertThat(this.tradeRepository.count(), is(5L));

		this.tradeRepository.deleteBySymbolAndAction("ABCD", "SELL");

		assertThat(this.tradeRepository.count(), is(0L));

		this.tradeRepository.saveAll(allTrades);

		List<Trade> allTradesRetrieved = this.spannerOperations.readAll(Trade.class);
		assertThat(
				"size is not " + allTrades.size() + " in received records: \n"
						+ allTradesRetrieved,
				allTradesRetrieved.size(), is(allTrades.size()));
		assertThat(allTradesRetrieved, containsInAnyOrder(allTrades.toArray()));

		assertThat(this.tradeRepository.countByAction("BUY"), is(3));
		assertThat(this.tradeRepository.countByActionQuery("BUY"), is(3));
		assertTrue(this.tradeRepository.existsByActionQuery("BUY"));

		assertNotNull(this.tradeRepository.getOneTrade("BUY"));

		assertEquals("BUY", this.tradeRepository.getFirstString("BUY"));
		assertThat(this.tradeRepository.getFirstStringList("BUY"),
				containsInAnyOrder("BUY", "BUY", "BUY"));

		List<Trade> trader2TradesRetrieved = this.tradeRepository
				.findByTraderId("trader2");
		assertThat(
				"size is not " + allTrades.size() + " in received records: \n"
						+ trader2TradesRetrieved,
				trader2TradesRetrieved.size(), is(trader2Trades.size()));
		assertThat(trader2TradesRetrieved, containsInAnyOrder(trader2Trades.toArray()));

		List<TradeProjection> tradeProjectionsRetrieved = this.tradeRepository
				.findByActionIgnoreCase("bUy");
		assertEquals(3, tradeProjectionsRetrieved.size());
		for (TradeProjection tradeProjection : tradeProjectionsRetrieved) {
			assertEquals("BUY", tradeProjection.getAction());
			assertEquals("ABCD BUY", tradeProjection.getSymbolAndAction());
		}

		List<Trade> tradesReceivedPage0 = this.tradeRepository
				.findAll(PageRequest.of(0, 3, Sort.by(Order.asc("id"))))
				.getContent();
		assertEquals(3, tradesReceivedPage0.size());
		assertTrue(tradesReceivedPage0.get(0).getId()
				.compareTo(tradesReceivedPage0.get(1).getId()) < 0);
		assertTrue(tradesReceivedPage0.get(1).getId()
				.compareTo(tradesReceivedPage0.get(2).getId()) < 0);

		List<Trade> tradesReceivedPage1 = this.tradeRepository
				.findAll(PageRequest.of(1, 3, Sort.by(Order.asc("id"))))
				.getContent();
		assertEquals(3, tradesReceivedPage1.size());
		assertTrue(tradesReceivedPage0.get(2).getId()
				.compareTo(tradesReceivedPage1.get(0).getId()) < 0);
		assertTrue(tradesReceivedPage1.get(0).getId()
				.compareTo(tradesReceivedPage1.get(1).getId()) < 0);
		assertTrue(tradesReceivedPage1.get(1).getId()
				.compareTo(tradesReceivedPage1.get(2).getId()) < 0);

		List<Trade> tradesReceivedPage2 = this.tradeRepository
				.findAll(PageRequest.of(2, 3, Sort.by(Order.asc("id"))))
				.getContent();
		assertEquals(2, tradesReceivedPage2.size());
		assertTrue(tradesReceivedPage1.get(2).getId()
				.compareTo(tradesReceivedPage2.get(0).getId()) < 0);
		assertTrue(tradesReceivedPage2.get(0).getId()
				.compareTo(tradesReceivedPage2.get(1).getId()) < 0);

		List<Trade> buyTradesRetrieved = this.tradeRepository
				.annotatedTradesByAction("BUY");
		assertThat(
				"size is not " + buyTradesRetrieved.size() + " in received records: \n"
						+ buyTradesRetrieved,
				buyTradesRetrieved.size(), is(trader1BuyTrades.size()));
		assertThat(buyTradesRetrieved,
				containsInAnyOrder(trader1BuyTrades.toArray()));

		List<Trade> customSortedTrades = this.tradeRepository.sortedTrades(PageRequest
				.of(2, 2, org.springframework.data.domain.Sort.by(Order.asc("id"))));

		assertEquals(2, customSortedTrades.size());
		assertTrue(customSortedTrades.get(0).getId()
				.compareTo(customSortedTrades.get(1).getId()) < 0);

		this.tradeRepository.findBySymbolLike("%BCD")
				.forEach(x -> assertEquals("ABCD", x.getSymbol()));
		assertTrue(this.tradeRepository.findBySymbolNotLike("%BCD").isEmpty());

		this.tradeRepository.findBySymbolContains("BCD")
				.forEach(x -> assertEquals("ABCD", x.getSymbol()));
		assertTrue(this.tradeRepository.findBySymbolNotContains("BCD").isEmpty());

		assertEquals(3, this.tradeRepository
				.findBySymbolAndActionPojo(new SymbolAction("ABCD", "BUY")).size());
		assertEquals(3,
				this.tradeRepository.findBySymbolAndActionStruct(Struct.newBuilder()
						.set("symbol").to("ABCD").set("action").to("BUY").build())
						.size());

		// testing setting some columns to null.
		Trade someTrade = this.tradeRepository.findBySymbolContains("ABCD").get(0);
		assertNotNull(someTrade.getExecutionTimes());
		assertNotNull(someTrade.getSymbol());
		someTrade.setExecutionTimes(null);
		someTrade.setSymbol(null);
		this.tradeRepository.save(someTrade);
		someTrade = this.tradeRepository.findById(this.spannerSchemaUtils.getKey(someTrade)).get();
		assertNull(someTrade.getExecutionTimes());
		assertNull(someTrade.getSymbol());

		// testing parent-child relationships
		assertEquals(0, someTrade.getSubTrades().size());
		SubTrade subTrade1 = new SubTrade(someTrade.getTradeDetail().getId(),
				someTrade.getTraderId(), "subTrade1");
		SubTrade subTrade2 = new SubTrade(someTrade.getTradeDetail().getId(),
				someTrade.getTraderId(), "subTrade2");
		SubTradeComponent subTradeComponent11 = new SubTradeComponent(
				someTrade.getTradeDetail().getId(), someTrade.getTraderId(), "subTrade1",
				"11a", "11b");
		SubTradeComponent subTradeComponent21 = new SubTradeComponent(
				someTrade.getTradeDetail().getId(), someTrade.getTraderId(), "subTrade2",
				"21a", "21b");
		SubTradeComponent subTradeComponent22 = new SubTradeComponent(
				someTrade.getTradeDetail().getId(), someTrade.getTraderId(), "subTrade2",
				"22a", "22b");

		subTrade1.setSubTradeComponentList(ImmutableList.of(subTradeComponent11));
		subTrade2.setSubTradeComponentList(
				ImmutableList.of(subTradeComponent21, subTradeComponent22));
		someTrade.setSubTrades(ImmutableList.of(subTrade1, subTrade2));

		this.tradeRepository.save(someTrade);

		assertEquals(2, this.subTradeRepository.count());
		assertEquals(3, this.subTradeComponentRepository.count());

		this.subTradeRepository.deleteById(this.spannerSchemaUtils.getKey(subTrade1));
		assertEquals(2, this.subTradeComponentRepository.count());

		someTrade = this.tradeRepository
				.findById(this.spannerSchemaUtils.getKey(someTrade)).get();
		assertEquals(1, someTrade.getSubTrades().size());
		assertEquals("subTrade2", someTrade.getSubTrades().get(0).getSubTradeId());
		assertEquals(2,
				someTrade.getSubTrades().get(0).getSubTradeComponentList().size());

		this.tradeRepository.delete(someTrade);

		assertEquals(0, this.subTradeComponentRepository.count());
		assertEquals(0, this.subTradeRepository.count());

		this.tradeRepository.deleteAll();
		this.tradeRepositoryTransactionalService.testTransactionalAnnotation();
	}

	private List<Trade> insertTrades(String traderId1, String action, int numTrades) {
		List<Trade> trades = new ArrayList<>();
		for (int i = 0; i < numTrades; i++) {
			Trade t = Trade.aTrade();
			t.setAction(action);
			t.setTraderId(traderId1);
			t.setSymbol("ABCD");
			trades.add(t);
			this.spannerOperations.insert(t);
		}
		return trades;
	}

	public static class TradeRepositoryTransactionalService {

		@Autowired
		TradeRepository tradeRepository;

		@Transactional
		public void testTransactionalAnnotation() {
			Trade trade = Trade.aTrade();
			this.tradeRepository.save(trade);

			// because the insert happens within the same transaction, this count is still
			// 1
			assertThat(this.tradeRepository.count(), is(0L));
		}
	}
}
