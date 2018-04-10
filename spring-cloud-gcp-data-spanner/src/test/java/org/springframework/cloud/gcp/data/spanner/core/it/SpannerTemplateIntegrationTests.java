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

package org.springframework.cloud.gcp.data.spanner.core.it;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import com.google.cloud.spanner.Key;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.spanner.core.SpannerReadOptions;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.test.AbstractSpannerIntegrationTest;
import org.springframework.cloud.gcp.data.spanner.test.domain.SharesTransaction;
import org.springframework.cloud.gcp.data.spanner.test.domain.SubTrade;
import org.springframework.cloud.gcp.data.spanner.test.domain.Trade;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */

@RunWith(SpringRunner.class)
public class SpannerTemplateIntegrationTests extends AbstractSpannerIntegrationTest {

	@Autowired
	SpannerMappingContext spannerMappingContext;

	@Test
	public void insertAndDeleteSequence() {
		assertThat(this.spannerOperations.count(Trade.class), is(0L));

		Trade trade = Trade.aTrade();
		this.spannerOperations.insert(trade);
		assertThat(this.spannerOperations.count(Trade.class), is(1L));
		assertThat(this.spannerOperations.count(SubTrade.class), is(0L));
		assertThat(this.spannerOperations.count(SharesTransaction.class), is(0L));

		SubTrade subTrade = new SubTrade();
		subTrade.setTradeId(trade.getId());
		subTrade.setTraderId(trade.getTraderId());
		subTrade.setSubtradeId("a");
		trade.setSubTrades(Arrays.asList(subTrade));

		SharesTransaction sharesTransaction = new SharesTransaction();
		sharesTransaction.setTradeId(subTrade.getTradeId());
		sharesTransaction.setSubtradeId(subTrade.getSubtradeId());
		sharesTransaction.setTraderId(trade.getTraderId());
		sharesTransaction.setTransId("b");
		sharesTransaction.setSize(3);
		subTrade.setTransactionList(Arrays.asList(sharesTransaction));

		this.spannerOperations.upsert(trade);

		assertThat(this.spannerOperations.count(Trade.class), is(1L));
		assertThat(this.spannerOperations.count(SubTrade.class), is(1L));
		assertThat(this.spannerOperations.count(SharesTransaction.class), is(1L));

		Map<String, Set<String>> relationships = this.spannerDatabaseAdminTemplate
				.getParentChildTablesMap();
		String tradeTableName = this.spannerMappingContext
				.getPersistentEntity(Trade.class).tableName();
		String subTradeTableName = this.spannerMappingContext
				.getPersistentEntity(SubTrade.class).tableName();
		String sharesTransactionTableName = this.spannerMappingContext
				.getPersistentEntity(SharesTransaction.class).tableName();
		assertThat(relationships.get(tradeTableName),
				containsInAnyOrder(subTradeTableName));
		assertThat(relationships.get(subTradeTableName),
				containsInAnyOrder(sharesTransactionTableName));

		Trade retrievedTrade = this.spannerOperations.find(Trade.class,
				Key.of(trade.getId(), trade.getTraderId()));
		assertThat(retrievedTrade, is(trade));
		assertEquals("a", retrievedTrade.getSubTrades().get(0).getSubtradeId());
		assertEquals(1, retrievedTrade.getSubTrades().size());
		assertEquals("b", retrievedTrade.getSubTrades().get(0).getTransactionList().get(0)
				.getTransId());
		assertEquals(1, retrievedTrade.getSubTrades().get(0).getTransactionList().size());

		// cascading delete should delete all children
		this.spannerOperations.delete(trade);
		assertThat(this.spannerOperations.count(Trade.class), is(0L));
		assertThat(this.spannerOperations.count(SubTrade.class), is(0L));
		assertThat(this.spannerOperations.count(SharesTransaction.class), is(0L));
	}

	@Test
	public void readWriteTransactionTest() {
		Trade trade = Trade.aTrade();
		this.spannerOperations.performReadWriteTransaction(transactionOperations -> {
			transactionOperations.insert(trade);

			// because the insert happens within the same transaction, this count is still
			// 0
			assertThat(transactionOperations.count(Trade.class), is(0L));
			return null;
		});

		// now that the transaction has completed, the count should be 1
		assertThat(this.spannerOperations.count(Trade.class), is(1L));
	}

	@Test(expected = SpannerDataException.class)
	public void readOnlyTransactionTest() {
		Trade trade = Trade.aTrade();
		this.spannerOperations.performReadOnlyTransaction(transactionOperations -> {
			// cannot do mutate in a read-only transaction
			transactionOperations.insert(trade);
			return null;
		}, new SpannerReadOptions());
	}
}
