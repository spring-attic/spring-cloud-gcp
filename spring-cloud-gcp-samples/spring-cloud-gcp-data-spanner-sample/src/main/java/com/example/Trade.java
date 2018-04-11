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
 * @author Mike Eltsufin
 */
@Table(name = "trades")
public class Trade {

	@PrimaryKey(keyOrder = 2)
	@Column(name = "trade_id")
	private String tradeId;

	@PrimaryKey(keyOrder = 1)
	@Column(name = "trader_id")
	private String traderId;

	private String action;

	private Double price;

	private Double shares;

	private String symbol;

	@ColumnInnerType(innerType = Double.class)
	private List<Double> curve;

	public Trade() {
	}

	public Trade(String tradeId, String action, Double price, Double shares, String symbol, String traderId,
			List<Double> curve) {
		this.tradeId = tradeId;
		this.action = action;
		this.price = price;
		this.shares = shares;
		this.symbol = symbol;
		this.traderId = traderId;
		this.curve = curve;
	}

	public String getTradeId() {
		return this.tradeId;
	}

	public void setTradeId(String tradeId) {
		this.tradeId = tradeId;
	}

	public String getTraderId() {
		return this.traderId;
	}

	public void setTraderId(String traderId) {
		this.traderId = traderId;
	}

	public String getAction() {
		return this.action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public Double getPrice() {
		return this.price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public Double getShares() {
		return this.shares;
	}

	public void setShares(Double shares) {
		this.shares = shares;
	}

	public String getSymbol() {
		return this.symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public List<Double> getCurve() {
		return this.curve;
	}

	public void setCurve(List<Double> curve) {
		this.curve = curve;
	}

	@Override
	public String toString() {
		return "Trade{" +
				"tradeId=" + this.tradeId +
				", traderId='" + this.traderId + '\'' +
				", action='" + this.action + '\'' +
				", price=" + this.price +
				", shares=" + this.shares +
				", symbol='" + this.symbol + '\'' +
				", curve=" + this.curve +
				'}';
	}
}
