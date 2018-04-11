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
import java.util.List;

import com.google.cloud.spanner.KeySet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.context.annotation.Bean;

/**
 * @author Balint Pato
 * @author Mike Eltsufin
 */
@SpringBootApplication
public class SpannerTemplateExample {

	@Autowired
	private SpannerOperations spannerOperations;

	public static void main(String[] args) {
		new SpringApplicationBuilder(SpannerTemplateExample.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}

	@Bean
	public CommandLineRunner commandLineRunner() {
		return args -> {
			this.spannerOperations.delete(Trader.class, KeySet.all());
			this.spannerOperations.delete(Trade.class, KeySet.all());

			Trader trader = new Trader("template_trader1", "John", "Doe");

			this.spannerOperations.insert(trader);

			Trade t = new Trade("1", "BUY", 100.0, 50.0, "STOCK1", "template_trader1", Arrays.asList(99.0, 101.00));

			this.spannerOperations.insert(t);

			t.setTradeId("2");
			t.setTraderId("template_trader1");
			t.setAction("SELL");
			this.spannerOperations.insert(t);

			t.setTradeId("1");
			t.setTraderId("template_trader2");
			this.spannerOperations.insert(t);

			List<Trade> tradesByAction = this.spannerOperations.readAll(Trade.class);

			tradesByAction.forEach(System.out::println);

			System.exit(0);
		};
	}

}
