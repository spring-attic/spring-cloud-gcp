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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.context.annotation.Bean;

/**
 * @author Chengyuan Zhao
 * @author Balint Pato
 * @author Mike Eltsufin
 */
@SpringBootApplication
public class SpannerRepositoryExample {

	@Autowired
	TraderRepository traderRepository;

	@Autowired
	TradeRepository tradeRepository;

	@Autowired
	SpannerSchemaUtils spannerSchemaUtils;

	public static void main(String[] args) {
		SpringApplication.run(SpannerRepositoryExample.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner() {
		return args -> {

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

			System.out.println("The table for trades has been cleared and "
					+ this.tradeRepository.count() + " new trades have been inserted:");

			Iterable<Trade> allTrades = this.tradeRepository.findAll();
			allTrades.forEach(System.out::println);

			System.out.println("There are " + this.tradeRepository.countByAction("BUY")
					+ " BUY trades: ");

			this.tradeRepository.findByAction("BUY").forEach(System.out::println);

			System.out.println("These are the Spanner primary keys for the trades:");

			allTrades.forEach(t -> {
				Key key = this.spannerSchemaUtils.getKey(t);
				System.out.println(key);
			});

			System.out.println("Try http://localhost:8080/trades in the browser to see all trades.");
		};
	}

}
