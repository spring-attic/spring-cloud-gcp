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

import java.util.List;

import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.ColumnInnerType;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;

/**
 * @author Ray Tsang
 * @author Chengyuan Zhao
 */
@Table(name = "trades")
public class Trade {

	@PrimaryKey(keyOrder = 2)
	String action;

	Double price;

	Double shares;

	@PrimaryKey(keyOrder = 1)
	String symbol;

	@Column(name = "trader_id")
	String traderId;

	@ColumnInnerType(innerType = Double.class)
	List<Double> curve;

	Person person;

	@Override
	public String toString() {
		return "Trade{" +
				"action='" + this.action + '\'' +
				", price=" + this.price +
				", shares=" + this.shares +
				", symbol='" + this.symbol + '\'' +
				", traderId='" + this.traderId +
				", person='" + this.person +
				'}';
	}
}
