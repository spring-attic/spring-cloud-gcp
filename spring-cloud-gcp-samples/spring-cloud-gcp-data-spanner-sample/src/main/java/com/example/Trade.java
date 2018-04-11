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

	@PrimaryKey(keyOrder = 1)
	private Long sequence;

	private String action;

	private Double price;

	private Double shares;

	private String symbol;

	@PrimaryKey(keyOrder = 2)
	@Column(name = "trader_id")
	private String traderId;

	@ColumnInnerType(innerType = Double.class)
	private List<Double> curve;

	public Trade() {
	}

	public Trade(Long sequence, String action, Double price, Double shares, String symbol, String traderId,
			List<Double> curve) {
		this.sequence = sequence;
		this.action = action;
		this.price = price;
		this.shares = shares;
		this.symbol = symbol;
		this.traderId = traderId;
		this.curve = curve;
	}

	public Long getSequence() {
		return sequence;
	}

	public void setSequence(Long sequence) {
		this.sequence = sequence;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public Double getShares() {
		return shares;
	}

	public void setShares(Double shares) {
		this.shares = shares;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getTraderId() {
		return traderId;
	}

	public void setTraderId(String traderId) {
		this.traderId = traderId;
	}

	public List<Double> getCurve() {
		return curve;
	}

	public void setCurve(List<Double> curve) {
		this.curve = curve;
	}

	@Override public String toString() {
		return "Trade{" +
				"sequence=" + sequence +
				", action='" + action + '\'' +
				", price=" + price +
				", shares=" + shares +
				", symbol='" + symbol + '\'' +
				", traderId='" + traderId + '\'' +
				", curve=" + curve +
				'}';
	}
}
