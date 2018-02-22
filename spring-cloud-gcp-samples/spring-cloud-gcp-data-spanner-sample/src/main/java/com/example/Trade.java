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

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerColumn;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerTable;
import org.springframework.data.annotation.Id;

/**
 * @author Ray Tsang
 * @author Chengyuan Zhao
 */
@SpannerTable(name = "trades")
public class Trade {
	@Id
	String id;

	String action;

	Double price;

	Double shares;

	String symbol;

	@SpannerColumn(name = "trader_id")
	String traderId;

	@Override
	public String toString() {
		return "id: " + this.id + " action: " + this.action + " price: "
				+ String.valueOf(this.price) + " shares: " + String.valueOf(this.shares)
				+ " symbol: " + this.symbol + " trader: " + this.traderId;
	}
}
