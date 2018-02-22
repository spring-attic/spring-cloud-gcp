/*
 *  Copyright 2017 original author or authors.
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

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.repository.config.EnableSpannerRepositories;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Chengyuan Zhao
 */
@RestController
@EnableSpannerRepositories
public class WebController {

	@Autowired
	SpannerOperations spannerOperations;

	@Autowired
	TradeRepository tradeRepository;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String demoSpanner() {
		this.tradeRepository.deleteAll();

		String traderId = UUID.randomUUID().toString();
		for (int i = 0; i < 5; i++) {
			String tradeId = UUID.randomUUID().toString();

			Trade t = new Trade();
			t.id = tradeId;
			t.symbol = "ABCD";
			t.action = "BUY";
			t.traderId = traderId;
			t.price = 100.0;
			t.shares = 12345.6;

			this.spannerOperations.insert(t);
		}

		StringBuilder reply = new StringBuilder();
		reply.append("The table for trades has been cleared and "
				+ this.tradeRepository.count() + " new trades have been inserted:<br />");

		this.spannerOperations.findAll(Trade.class).stream()
				.forEach(trade -> reply.append(trade.toString() + "<br />"));

		return reply.toString();
	}
}
