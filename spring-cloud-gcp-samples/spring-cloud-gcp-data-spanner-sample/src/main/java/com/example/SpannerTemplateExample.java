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

import java.util.List;

import com.google.cloud.spanner.KeySet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * @author Balint Pato
 */
@SpringBootApplication
public class SpannerTemplateExample {

	@Autowired
	SpannerOperations spannerOperations;

	public static void main(String[] args) {
		new SpringApplicationBuilder(SpannerTemplateExample.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {
			this.spannerOperations.delete(Trade.class, KeySet.all());

			Trade t = new Trade();
			t.symbol = "ST1";
			t.action = "BUY";
			t.traderId = "1234";
			t.price = 100.0;
			t.shares = 12345.6;

			this.spannerOperations.insert(t);
			t.action = "SELL";

			this.spannerOperations.insert(t);

			t.symbol = "ST2";
			this.spannerOperations.insert(t);

			List<Trade> tradesByAction = this.spannerOperations.findAll(Trade.class);

			tradesByAction.forEach(System.out::println);

			System.exit(0);
		};
	}

}
