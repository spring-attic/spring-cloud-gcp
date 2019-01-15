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

package org.springframework.cloud.gcp.data.spanner.repository.it;

import java.util.ArrayList;
import java.util.List;

import com.google.cloud.Timestamp;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Spanner Repository that uses many features.
 *
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

		Trade buyTrade1 = trader1BuyTrades.get(0);
		this.tradeRepository.updateActionTradeById(buyTrade1.getId(), "invalid action");
		assertThat(this.tradeRepository.findById(this.spannerSchemaUtils.getKey(buyTrade1)).get().getAction())
				.isEqualTo("invalid action");
		this.tradeRepository.updateActionTradeById(buyTrade1.getId(), "BUY");
		assertThat(this.tradeRepository.findById(this.spannerSchemaUtils.getKey(buyTrade1)).get().getAction())
				.isEqualTo("BUY");

		assertThat(this.tradeRepository.count()).isEqualTo(8L);

		assertThat(this.tradeRepository.deleteByAction("BUY")).isEqualTo(3);

		assertThat(this.tradeRepository.count()).isEqualTo(5L);

		List<Trade> deletedBySymbol = this.tradeRepository.deleteBySymbol("ABCD");

		assertThat(deletedBySymbol).hasSize(5);

		assertThat(this.tradeRepository.count()).isEqualTo(0L);

		this.tradeRepository.saveAll(deletedBySymbol);

		assertThat(this.tradeRepository.count()).isEqualTo(5L);

		this.tradeRepository.deleteBySymbolAndAction("ABCD", "SELL");

		assertThat(this.tradeRepository.count()).isEqualTo(0L);

		this.tradeRepository.saveAll(allTrades);

		List<Trade> allTradesRetrieved = this.spannerOperations.readAll(Trade.class);
		assertThat(allTradesRetrieved).containsExactlyInAnyOrderElementsOf(allTrades);

		assertThat(this.tradeRepository.countByAction("BUY")).isEqualTo(3);
		assertThat(this.tradeRepository.countByActionQuery("BUY")).isEqualTo(3);
		assertThat(this.tradeRepository.existsByActionQuery("BUY")).isTrue();

		assertThat(this.tradeRepository.getOneTrade("BUY")).isNotNull();

		assertThat(this.tradeRepository.getFirstString("BUY")).isEqualTo("BUY");
		assertThat(this.tradeRepository.getFirstStringList("BUY"))
				.containsExactlyInAnyOrder("BUY", "BUY", "BUY");

		List<Trade> trader2TradesRetrieved = this.tradeRepository
				.findByTraderId("trader2");
		assertThat(trader2TradesRetrieved).containsExactlyInAnyOrderElementsOf(trader2Trades);

		List<TradeProjection> tradeProjectionsRetrieved = this.tradeRepository
				.findByActionIgnoreCase("bUy");
		assertThat(tradeProjectionsRetrieved).hasSize(3);
		for (TradeProjection tradeProjection : tradeProjectionsRetrieved) {
			assertThat(tradeProjection.getAction()).isEqualTo("BUY");
			assertThat(tradeProjection.getSymbolAndAction()).isEqualTo("ABCD BUY");
		}

		List<Trade> tradesReceivedPage0 = this.tradeRepository
				.findAll(PageRequest.of(0, 3, Sort.by(Order.asc("id"))))
				.getContent();
		assertThat(tradesReceivedPage0).hasSize(3);
		assertThat(tradesReceivedPage0.get(0).getId()
				.compareTo(tradesReceivedPage0.get(1).getId())).isNegative();
		assertThat(tradesReceivedPage0.get(1).getId()
				.compareTo(tradesReceivedPage0.get(2).getId())).isNegative();

		List<Trade> tradesReceivedPage1 = this.tradeRepository
				.findAll(PageRequest.of(1, 3, Sort.by(Order.asc("id"))))
				.getContent();
		assertThat(tradesReceivedPage1).hasSize(3);
		assertThat(tradesReceivedPage0.get(2).getId()
				.compareTo(tradesReceivedPage1.get(0).getId())).isNegative();
		assertThat(tradesReceivedPage1.get(0).getId()
				.compareTo(tradesReceivedPage1.get(1).getId())).isNegative();
		assertThat(tradesReceivedPage1.get(1).getId()
				.compareTo(tradesReceivedPage1.get(2).getId())).isNegative();

		List<Trade> tradesReceivedPage2 = this.tradeRepository
				.findAll(PageRequest.of(2, 3, Sort.by(Order.asc("id"))))
				.getContent();
		assertThat(tradesReceivedPage2).hasSize(2);
		assertThat(tradesReceivedPage1.get(2).getId()
				.compareTo(tradesReceivedPage2.get(0).getId())).isNegative();
		assertThat(tradesReceivedPage2.get(0).getId()
				.compareTo(tradesReceivedPage2.get(1).getId())).isNegative();

		List<Trade> buyTradesRetrieved = this.tradeRepository
				.annotatedTradesByAction("BUY");
		assertThat(buyTradesRetrieved).containsExactlyInAnyOrderElementsOf(trader1BuyTrades);

		List<Trade> customSortedTrades = this.tradeRepository.sortedTrades(PageRequest
				.of(2, 2, org.springframework.data.domain.Sort.by(Order.asc("id"))));

		assertThat(customSortedTrades).hasSize(2);
		assertThat(customSortedTrades.get(0).getId()
				.compareTo(customSortedTrades.get(1).getId())).isNegative();

		this.tradeRepository.findBySymbolLike("%BCD")
				.forEach((x) -> assertThat(x.getSymbol()).isEqualTo("ABCD"));
		assertThat(this.tradeRepository.findBySymbolNotLike("%BCD")).isEmpty();

		this.tradeRepository.findBySymbolContains("BCD")
				.forEach((x) -> assertThat(x.getSymbol()).isEqualTo("ABCD"));
		assertThat(this.tradeRepository.findBySymbolNotContains("BCD")).isEmpty();

		assertThat(this.tradeRepository
				.findBySymbolAndActionPojo(new SymbolAction("ABCD", "BUY"))).hasSize(3);
		assertThat(this.tradeRepository.findBySymbolAndActionStruct(Struct.newBuilder()
						.set("symbol").to("ABCD").set("action").to("BUY").build())
				).hasSize(3);

		// testing setting some columns to null.
		Trade someTrade = this.tradeRepository.findBySymbolContains("ABCD").get(0);
		assertThat(someTrade.getExecutionTimes()).isNotNull();
		assertThat(someTrade.getSymbol()).isNotNull();
		someTrade.setExecutionTimes(null);
		someTrade.setSymbol(null);
		this.tradeRepository.save(someTrade);
		someTrade = this.tradeRepository.findById(this.spannerSchemaUtils.getKey(someTrade)).get();
		assertThat(someTrade.getExecutionTimes()).isNull();
		assertThat(someTrade.getSymbol()).isNull();

		// testing parent-child relationships
		assertThat(someTrade.getSubTrades()).hasSize(0);
		SubTrade subTrade1 = new SubTrade(someTrade.getTradeDetail().getId(),
				someTrade.getTraderId(), "subTrade1");
		SubTrade subTrade2 = new SubTrade(someTrade.getTradeDetail().getId(),
				someTrade.getTraderId(), "subTrade2");
		SubTradeComponent subTradeComponent11 = new SubTradeComponent(
				someTrade.getTradeDetail().getId(), someTrade.getTraderId(), "subTrade1",
				"11a", "11b");
		subTradeComponent11.setCommitTimestamp(Timestamp.ofTimeMicroseconds(11));
		SubTradeComponent subTradeComponent21 = new SubTradeComponent(
				someTrade.getTradeDetail().getId(), someTrade.getTraderId(), "subTrade2",
				"21a", "21b");
		subTradeComponent21.setCommitTimestamp(Timestamp.ofTimeMicroseconds(21));
		SubTradeComponent subTradeComponent22 = new SubTradeComponent(
				someTrade.getTradeDetail().getId(), someTrade.getTraderId(), "subTrade2",
				"22a", "22b");
		subTradeComponent22.setCommitTimestamp(Timestamp.ofTimeMicroseconds(22));

		subTrade1.setSubTradeComponentList(ImmutableList.of(subTradeComponent11));
		subTrade2.setSubTradeComponentList(
				ImmutableList.of(subTradeComponent21, subTradeComponent22));
		someTrade.setSubTrades(ImmutableList.of(subTrade1, subTrade2));

		this.tradeRepository.save(someTrade);

		assertThat(this.subTradeRepository.count()).isEqualTo(2);
		assertThat(this.subTradeComponentRepository.count()).isEqualTo(3);

		List<SubTradeComponent> subTradeComponents = (List) this.subTradeComponentRepository.findAll();

		assertThat(subTradeComponents.get(0).getCommitTimestamp())
				.isEqualTo(subTradeComponents.get(1).getCommitTimestamp());
		assertThat(subTradeComponents.get(0).getCommitTimestamp())
				.isEqualTo(subTradeComponents.get(2).getCommitTimestamp());
		assertThat(subTradeComponents.get(0).getCommitTimestamp()).isGreaterThan(Timestamp.ofTimeMicroseconds(22));

		this.subTradeRepository.deleteById(this.spannerSchemaUtils.getKey(subTrade1));
		assertThat(this.subTradeComponentRepository.count()).isEqualTo(2);

		someTrade = this.tradeRepository
				.findById(this.spannerSchemaUtils.getKey(someTrade)).get();
		assertThat(someTrade.getSubTrades()).hasSize(1);
		assertThat(someTrade.getSubTrades().get(0).getSubTradeId()).isEqualTo("subTrade2");
		assertThat(someTrade.getSubTrades().get(0).getSubTradeComponentList()).hasSize(2);

		this.tradeRepository.delete(someTrade);

		assertThat(this.subTradeComponentRepository.count()).isEqualTo(0);
		assertThat(this.subTradeRepository.count()).isEqualTo(0);

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

	/**
	 * A service that executes methods annotated as transactional.
	 */
	public static class TradeRepositoryTransactionalService {

		@Autowired
		TradeRepository tradeRepository;

		@Transactional
		public void testTransactionalAnnotation() {
			Trade trade = Trade.aTrade();
			this.tradeRepository.save(trade);

			// because the insert happens within the same transaction, this count is still
			// 1
			assertThat(this.tradeRepository.count()).isEqualTo(0L);
		}
	}
}
