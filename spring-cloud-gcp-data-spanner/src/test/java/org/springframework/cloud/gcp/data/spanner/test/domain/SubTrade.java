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

import java.util.List;

import org.springframework.cloud.gcp.data.spanner.core.mapping.Embedded;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Interleaved;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;

/**
 * An interleaved child of {@link Trade}.
 *
 * @author Chengyuan Zhao
 */
@Table(name = "#{'sub_trades_'.concat(tableNameSuffix)}")
public class SubTrade {

	@Embedded
	@PrimaryKey
	TradeIdentifier tradeIdentifier;

	@PrimaryKey(keyOrder = 2)
	String subTradeId;

	@Interleaved
	List<SubTradeComponent> subTradeComponentList;

	public SubTrade() {

	}

	public SubTrade(String id, String traderId, String subTradeId) {
		TradeIdentifier tradeIdentifier = new TradeIdentifier();
		tradeIdentifier.identifier = id;
		tradeIdentifier.trader_id = traderId;
		this.subTradeId = subTradeId;
		this.tradeIdentifier = tradeIdentifier;
	}

	public String getSubTradeId() {
		return this.subTradeId;
	}

	public void setSubTradeId(String subTradeId) {
		this.subTradeId = subTradeId;
	}

	public List<SubTradeComponent> getSubTradeComponentList() {
		return this.subTradeComponentList;
	}

	public void setSubTradeComponentList(List<SubTradeComponent> subTradeComponentList) {
		this.subTradeComponentList = subTradeComponentList;
	}
}
