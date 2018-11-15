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

import java.util.Arrays;

import com.google.cloud.spanner.Key;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerDatabaseAdminTemplate;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.stereotype.Component;

/**
 * @author Chengyuan Zhao
 * @author Balint Pato
 * @author Mike Eltsufin
 */
@Component
public class SpannerRepositoryExample {
	private static final Log LOGGER = LogFactory.getLog(SpannerRepositoryExample.class);

	@Autowired
	private TraderRepository traderRepository;

	@Autowired
	private TradeRepository tradeRepository;

	@Autowired
	private SpannerSchemaUtils spannerSchemaUtils;

	@Autowired
	private SpannerDatabaseAdminTemplate spannerDatabaseAdminTemplate;

	public void runExample() {
		createTablesIfNotExists();
		this.traderRepository.deleteAll();
		this.tradeRepository.deleteAll();

		this.traderRepository.save(new Trader("demo_trader1", "John", "Doe"));
		this.traderRepository.save(new Trader("demo_trader2", "Mary", "Jane"));
		this.traderRepository.save(new Trader("demo_trader3", "Scott", "Smith"));

		this.tradeRepository
				.save(new Trade("1", "BUY", 100.0, 50.0, "STOCK1", "demo_trader1", Arrays.asList(99.0, 101.00)));
		this.tradeRepository
				.save(new Trade("2", "BUY", 105.0, 60.0, "STOCK2", "demo_trader1", Arrays.asList(99.0, 101.00)));
		this.tradeRepository
				.save(new Trade("3", "BUY", 100.0, 50.0, "STOCK1", "demo_trader1", Arrays.asList(99.0, 101.00)));
		this.tradeRepository
				.save(new Trade("1", "BUY", 100.0, 70.0, "STOCK2", "demo_trader2", Arrays.asList(99.0, 101.00)));
		this.tradeRepository
				.save(new Trade("2", "BUY", 103.0, 50.0, "STOCK1", "demo_trader2", Arrays.asList(99.0, 101.00)));
		this.tradeRepository
				.save(new Trade("3", "SELL", 100.0, 52.0, "STOCK2", "demo_trader2", Arrays.asList(99.0, 101.00)));
		this.tradeRepository
				.save(new Trade("1", "SELL", 98.0, 50.0, "STOCK1", "demo_trader3", Arrays.asList(99.0, 101.00)));
		this.tradeRepository
				.save(new Trade("2", "SELL", 110.0, 50.0, "STOCK2", "demo_trader3", Arrays.asList(99.0, 101.00)));

		LOGGER.info("The table for trades has been cleared and "
				+ this.tradeRepository.count() + " new trades have been inserted:");

		Iterable<Trade> allTrades = this.tradeRepository.findAll();

		LOGGER.info("All trades:");
		for (Trade t : allTrades) {
			LOGGER.info(t);
		}

		LOGGER.info("These are the Cloud Spanner primary keys for the trades:");
		for (Trade t : allTrades) {
			Key key = this.spannerSchemaUtils.getKey(t);
			LOGGER.info(key);
		}

		LOGGER.info("There are " + this.tradeRepository.countByAction("BUY") + " BUY trades:");
		for (Trade t : this.tradeRepository.findByAction("BUY")) {
			LOGGER.info(t);
		}

		LOGGER.info("A query method can retrieve a single entity:");
		LOGGER.info(this.tradeRepository.getAnyOneTrade());

		LOGGER.info("A query method can also select properties in entities:");
		this.tradeRepository.getTradeIds("BUY").stream()
				.forEach(x -> LOGGER.info(x));

		LOGGER.info("Try http://localhost:8080/trades in the browser to see all trades.");
	}

	void createTablesIfNotExists() {
		if (!this.spannerDatabaseAdminTemplate.tableExists("trades")) {
			this.spannerDatabaseAdminTemplate.executeDdlStrings(
					Arrays.asList(
							this.spannerSchemaUtils.getCreateTableDdlString(Trade.class)),
					true);
		}

		if (!this.spannerDatabaseAdminTemplate.tableExists("traders")) {
			this.spannerDatabaseAdminTemplate.executeDdlStrings(Arrays.asList(
					this.spannerSchemaUtils.getCreateTableDdlString(Trader.class)), true);
		}
	}
}
