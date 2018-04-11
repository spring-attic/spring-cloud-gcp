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

import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.ColumnInnerType;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;

/**
 * @author Chengyuan Zhao
 */
@Table(name = "#{'subtrades_'.concat(tableNameSuffix)}")
public class SubTrade {

	@PrimaryKey(keyOrder = 1)
	@Column(name = "id")
	private String tradeId;

	@PrimaryKey(keyOrder = 2)
	@Column(name = "trader_id")
	private String traderId;

	@PrimaryKey(keyOrder = 3)
	private String subtradeId;

	@ColumnInnerType(innerType = SharesTransaction.class)
	private List<SharesTransaction> transactionList;

	public String getTradeId() {
		return this.tradeId;
	}

	public void setTradeId(String tradeId) {
		this.tradeId = tradeId;
	}

	public String getSubtradeId() {
		return this.subtradeId;
	}

	public void setSubtradeId(String subtradeId) {
		this.subtradeId = subtradeId;
	}

	public List<SharesTransaction> getTransactionList() {
		return this.transactionList;
	}

	public void setTransactionList(List<SharesTransaction> transactionList) {
		this.transactionList = transactionList;
	}

	public String getTraderId() {
		return this.traderId;
	}

	public void setTraderId(String traderId) {
		this.traderId = traderId;
	}
}
