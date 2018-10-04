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

package org.springframework.cloud.gcp.data.spanner.test.domain;

import com.google.spanner.v1.TypeCode;

import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Embedded;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;

/**
 * A child interleaved table of {@link SubTrade} and a grand-child of {@link Trade}
 *
 * @author Chengyuan Zhao
 */
@Table(name = "#{'sub_trade_component'.concat(tableNameSuffix)}")
public class SubTradeComponent {

	@PrimaryKey
	@Embedded
	SubTradeIdentifier subTradeIdentifier;

	@PrimaryKey(keyOrder = 2)
	String componentIdPartA;

	@PrimaryKey(keyOrder = 3)
	String componentIdPartB;

	@Column(spannerType = TypeCode.STRING)
	boolean booleanValue;

	public SubTradeComponent() {

	}

	public SubTradeComponent(String id, String traderId, String subTradeId,
			String componentIdPartA, String componentIdPartB) {
		TradeIdentifier tradeIdentifier = new TradeIdentifier();
		tradeIdentifier.identifier = id;
		tradeIdentifier.trader_id = traderId;
		SubTradeIdentifier subTradeIdentifier = new SubTradeIdentifier();
		subTradeIdentifier.sub_trade_id = subTradeId;
		subTradeIdentifier.tradeIdentifier = tradeIdentifier;
		this.subTradeIdentifier = subTradeIdentifier;
		this.componentIdPartA = componentIdPartA;
		this.componentIdPartB = componentIdPartB;
	}
}
