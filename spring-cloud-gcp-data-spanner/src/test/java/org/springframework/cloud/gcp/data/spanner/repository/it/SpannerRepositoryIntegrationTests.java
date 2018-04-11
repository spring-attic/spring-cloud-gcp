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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.test.AbstractSpannerIntegrationTest;
import org.springframework.cloud.gcp.data.spanner.test.domain.Trade;
import org.springframework.cloud.gcp.data.spanner.test.domain.TradeRepository;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
public class SpannerRepositoryIntegrationTests extends AbstractSpannerIntegrationTest {

	@Autowired
	SpannerOperations spannerOperations;

	@Autowired
	TradeRepository tradeRepository;

	@Test
	public void declarativeQueryMethodTest() {
		List<Trade> trader1BuyTrades = insertTrades("trader1", "BUY", 3);
		List<Trade> trader1SellTrades = insertTrades("trader1", "SELL", 2);
		List<Trade> trader2Trades = insertTrades("trader2", "SELL", 3);

		List<Trade> allTrades = new ArrayList<>();
		allTrades.addAll(trader1BuyTrades);
		allTrades.addAll(trader1SellTrades);
		allTrades.addAll(trader2Trades);

		assertThat(this.tradeRepository.count(), is(8L));

		List<Trade> allTradesRetrieved = this.spannerOperations.readAll(Trade.class);
		assertThat(
				"size is not " + allTrades.size() + " in received records: \n"
						+ allTradesRetrieved,
				allTradesRetrieved.size(), is(allTrades.size()));
		assertThat(allTradesRetrieved, containsInAnyOrder(allTrades.toArray()));

		assertThat(this.tradeRepository.countByAction("BUY"), is(3));

		List<Trade> trader2TradesRetrieved = this.tradeRepository
				.findByTraderId("trader2");
		assertThat(
				"size is not " + allTrades.size() + " in received records: \n"
						+ trader2TradesRetrieved,
				trader2TradesRetrieved.size(), is(trader2Trades.size()));
		assertThat(trader2TradesRetrieved, containsInAnyOrder(trader2Trades.toArray()));

		List<Trade> buyTradesRetrieved = this.tradeRepository
				.annotatedTradesByAction("BUY");
		assertThat(
				"size is not " + buyTradesRetrieved.size() + " in received records: \n"
						+ buyTradesRetrieved,
				buyTradesRetrieved.size(), is(trader1BuyTrades.size()));
		assertThat(buyTradesRetrieved,
				containsInAnyOrder(trader1BuyTrades.toArray()));
	}

	protected List<Trade> insertTrades(String traderId1, String action, int numTrades) {
		List<Trade> trades = new ArrayList<>();
		for (int i = 0; i < numTrades; i++) {
			Trade t = Trade.aTrade();
			t.setAction(action);
			t.setTraderId(traderId1);
			trades.add(t);
			this.spannerOperations.insert(t);
		}
		return trades;
	}
}
