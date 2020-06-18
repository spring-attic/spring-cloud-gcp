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

package org.springframework.cloud.gcp.data.spanner.repository.it;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import org.assertj.core.util.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.gcp.data.spanner.core.SpannerQueryOptions;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.cloud.gcp.data.spanner.test.AbstractSpannerIntegrationTest;
import org.springframework.cloud.gcp.data.spanner.test.domain.SubTrade;
import org.springframework.cloud.gcp.data.spanner.test.domain.SubTradeComponent;
import org.springframework.cloud.gcp.data.spanner.test.domain.SubTradeComponentRepository;
import org.springframework.cloud.gcp.data.spanner.test.domain.SubTradeRepository;
import org.springframework.cloud.gcp.data.spanner.test.domain.SymbolAction;
import org.springframework.cloud.gcp.data.spanner.test.domain.Trade;
import org.springframework.cloud.gcp.data.spanner.test.domain.TradeProjection;
import org.springframework.cloud.gcp.data.spanner.test.domain.TradeRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

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

	@Autowired
	SpannerMappingContext spannerMappingContext;

	@SpyBean
	SpannerTemplate spannerTemplate;

	@Before
	@After
	public void cleanUpData() {
		this.tradeRepository.deleteAll();
	}

	@Test
	public void queryMethodsTest() {
		final int subTrades = 42;
		Trade trade = Trade.aTrade(null, subTrades);
		this.spannerOperations.insert(trade);

		final String identifier = trade.getTradeDetail().getId();
		final String traderId = trade.getTraderId();

		long count = subTradeRepository.countBy(identifier, traderId);
		assertThat(count)
				.isEqualTo(subTrades);

		List<SubTrade> list = subTradeRepository.getList(
				identifier, traderId, Sort.by(Order.desc("subTradeId")));
		assertThat(list)
				.hasSize(subTrades);
		assertThat(list.get(subTrades - 1))
				.satisfies(s -> assertThat(s.getSubTradeId()).isEqualTo("subTrade0"));

		List<SubTrade> page = subTradeRepository.getPage(
				identifier, traderId, PageRequest.of(0, 1024, Sort.by(Order.asc("subTradeId"))));
		assertThat(page)
				.hasSize(subTrades);
		assertThat(page.get(0))
				.satisfies(s -> assertThat(s.getSubTradeId()).isEqualTo("subTrade0"));

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

		arrayParameterBindTest();

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

		assertThat(this.tradeRepository
				.findByTraderId("trader2", PageRequest.of(0, 2, Sort.by("tradeTime"))))
				.containsExactlyInAnyOrder(trader2Trades.get(0), trader2Trades.get(1));

		assertThat(this.tradeRepository
				.findByTraderId("trader2", PageRequest.of(1, 2, Sort.by("tradeTime"))))
				.containsExactlyInAnyOrder(trader2Trades.get(2));

		assertThat(this.tradeRepository
				.findByTraderId("trader2", PageRequest.of(0, 2, Sort.by(Direction.DESC, "tradeTime"))))
				.containsExactlyInAnyOrder(trader2Trades.get(2), trader2Trades.get(1));

		assertThat(this.tradeRepository
				.findByTraderId("trader2", PageRequest.of(1, 2, Sort.by(Direction.DESC, "tradeTime"))))
				.containsExactlyInAnyOrder(trader2Trades.get(0));

		assertThat(this.tradeRepository
				.findTop2ByTraderIdOrderByTradeTimeAsc("trader2", Pageable.unpaged()))
				.containsExactlyInAnyOrder(trader2Trades.get(0), trader2Trades.get(1));

		assertThat(this.tradeRepository
				.findTop2ByTraderIdOrderByTradeTimeAsc("trader2", PageRequest.of(0, 1)))
				.containsExactlyInAnyOrder(trader2Trades.get(0));

		assertThat(this.tradeRepository
				.findTop2ByTraderIdOrderByTradeTimeAsc("trader2", PageRequest.of(0, 1, Sort.by(Direction.DESC, "tradeTime"))))
				.containsExactlyInAnyOrder(trader2Trades.get(2));

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
				.annotatedTradesByAction("BUY", PageRequest.of(0, 100, Sort.by(Order.desc("id"))));
		assertThat(buyTradesRetrieved).containsExactlyInAnyOrderElementsOf(trader1BuyTrades);
		assertThat(buyTradesRetrieved.get(0).getId()).isGreaterThan(buyTradesRetrieved.get(1).getId());
		assertThat(buyTradesRetrieved.get(1).getId()).isGreaterThan(buyTradesRetrieved.get(2).getId());

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

		subTrade1.setSubTradeComponentList(Arrays.asList(subTradeComponent11));
		subTrade2.setSubTradeComponentList(
				Arrays.asList(subTradeComponent21, subTradeComponent22));
		someTrade.setSubTrades(Arrays.asList(subTrade1, subTrade2));

		this.tradeRepository.save(someTrade);

		assertThat(this.subTradeRepository.count()).isEqualTo(2);
		assertThat(this.subTradeComponentRepository.count()).isEqualTo(3);

		// test eager-fetch in @Query
		Mockito.clearInvocations(spannerTemplate);
		final Trade aTrade = someTrade;
		assertThat(tradeRepository.fetchById(aTrade.getId()))
				.isNotEmpty()
				.hasValueSatisfying(t -> assertThat(t.getId()).isEqualTo(aTrade.getId()))
				.hasValueSatisfying(t -> assertThat(t.getTraderId()).isEqualTo(aTrade.getTraderId()))
				.hasValueSatisfying(t -> assertThat(t.getSymbol()).isEqualTo(aTrade.getSymbol()))
				.hasValueSatisfying(t -> assertThat(t.getSubTrades()).hasSize(aTrade.getSubTrades().size()));
		Mockito.verify(spannerTemplate, Mockito.times(1))
				.executeQuery(any(Statement.class), any());
		Mockito.verify(spannerTemplate, Mockito.times(1))
				.query(eq(Trade.class), any(Statement.class), any(SpannerQueryOptions.class));

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
		this.tradeRepositoryTransactionalService.testTransactionalAnnotation(3);
		assertThat(this.tradeRepository.count()).isEqualTo(1L);

		List<Trade> trades = (List) tradeRepository.findAll();
		someTrade = trades.get(0);
		assertThat(someTrade.getSubTrades()).hasSize(3);
		SubTrade someSubTrade = trades.get(0).getSubTrades().get(0);
		someSubTrade.setDisabled(true); // a soft-delete
		subTradeRepository.save(someSubTrade);

		someTrade = this.tradeRepository
				.findById(this.spannerSchemaUtils.getKey(someTrade)).get();
		assertThat(someTrade.getSubTrades())
				.doesNotContain(someSubTrade) // "someSubTrade" was soft-deleted
				.hasSize(2);

	}

	@Test
	public void existsTest() {
		Trade trade = Trade.aTrade();
		this.tradeRepository.save(trade);
		SpannerPersistentEntity<?> persistentEntity = this.spannerMappingContext.getPersistentEntity(Trade.class);
		PersistentPropertyAccessor accessor = persistentEntity.getPropertyAccessor(trade);
		PersistentProperty idProperty = persistentEntity.getIdProperty();
		Key key = (Key) accessor.getProperty(idProperty);
		assertThat(this.tradeRepository.existsById(key)).isTrue();
		this.tradeRepository.delete(trade);
		assertThat(this.tradeRepository.existsById(key)).isFalse();
	}

	@Test
	public void testNonNull() {
		assertThatThrownBy(() -> this.tradeRepository.getByAction("non-existing-action"))
				.isInstanceOf(EmptyResultDataAccessException.class)
				.hasMessageMatching("Result must not be null!");
	}

	@Test
	public void testTransactionRolledBack() {
		assertThat(this.tradeRepository.count()).isEqualTo(0L);
		try {
			this.tradeRepositoryTransactionalService.testTransactionRolledBack();
		}
		catch (RuntimeException re) {
			// expected exception that causes roll-back;
		}
		assertThat(this.tradeRepository.count()).isEqualTo(0L);
	}

	private void arrayParameterBindTest() {
		assertThat(this.tradeRepository.countByActionIn(Arrays.asList("BUY", "SELL"))).isEqualTo(8L);
		assertThat(this.tradeRepository.countByActionIn(Collections.singletonList("BUY"))).isEqualTo(3L);
		assertThat(this.tradeRepository.countByActionIn(Collections.singletonList("SELL"))).isEqualTo(5L);
		assertThat(this.tradeRepository.countWithInQuery(Arrays.asList("BUY", "SELL"))).isEqualTo(8L);
		assertThat(this.tradeRepository.countWithInQuery(Collections.singletonList("BUY"))).isEqualTo(3L);
		assertThat(this.tradeRepository.countWithInQuery(Collections.singletonList("SELL"))).isEqualTo(5L);
		assertThat(this.tradeRepository.findByActionIn(Sets.newHashSet(Arrays.asList("BUY", "SELL"))).size())
				.isEqualTo(8L);
		assertThat(this.tradeRepository.findByActionIn(Collections.singleton("BUY")).size()).isEqualTo(3L);
		assertThat(this.tradeRepository.findByActionIn(Collections.singleton("SELL")).size()).isEqualTo(5L);
	}

	private List<Trade> insertTrades(String traderId, String action, int numTrades) {
		List<Trade> trades = new ArrayList<>();
		for (int i = 0; i < numTrades; i++) {
			Trade t = Trade.aTrade(traderId, 0, i);
			t.setAction(action);
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
		public void testTransactionalAnnotation(int numSubTrades) {
			Trade trade = Trade.aTrade(null, numSubTrades);
			this.tradeRepository.save(trade);
			// because the insert happens within the same transaction, this count is still
			// 1
			assertThat(this.tradeRepository.count()).isEqualTo(0L);
		}

		@Transactional
		public void testTransactionRolledBack() {
			Trade trade = Trade.aTrade();
			this.tradeRepository.save(trade);
			throw new RuntimeException("Intentional error to rollback save.");
		}
	}
}
