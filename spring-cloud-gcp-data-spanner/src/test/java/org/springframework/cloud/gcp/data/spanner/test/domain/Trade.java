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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKeyColumn;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;

/**
 * @author Ray Tsang
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
@Table(name = "#{'trades_'.concat(tableNameSuffix)}")
public class Trade {
	@PrimaryKeyColumn(keyOrder = 1)
	private String id;

	private int age;

	private Instant tradeTime;

	private String action;

	private Double price;

	private Double shares;

	private String symbol;

	@PrimaryKeyColumn(keyOrder = 2)
	@Column(name = "trader_id")
	private String traderId;

	public static Trade aTrade() {
		Trade t = new Trade();
		String tradeId = UUID.randomUUID().toString();
		String traderId = UUID.randomUUID().toString();

		t.id = tradeId;
		t.age = 8;
		t.symbol = "ABCD";
		t.action = "BUY";
		t.traderId = traderId;
		t.tradeTime = Instant.now();
		t.price = 100.0;
		t.shares = 12345.6;
		return t;
	}

	public static String createDDL(String tableName) {
		return "CREATE TABLE " + tableName + "(" + "\tid STRING(128) NOT NULL,\n"
				+ "\tage INT64,\n" + "\taction STRING(15),\n" + "\tprice FLOAT64,\n"
				+ "\tshares FLOAT64,\n" + "\ttradeTime TIMESTAMP,\n"
				+ "\tsymbol STRING(5),\n" + "\ttrader_id STRING(128),\n"
				+ ") PRIMARY KEY (id, trader_id)";
	}

	public static String dropDDL(String tableName) {
		return "DROP table " + tableName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Trade trade = (Trade) o;
		return Objects.equals(this.id, trade.id)
				&& Objects.equals(this.age, trade.age)
				&& Objects.equals(this.action, trade.action)
				&& Objects.equals(this.price, trade.price)
				&& Objects.equals(this.shares, trade.shares)
				&& Objects.equals(this.symbol, trade.symbol)
				&& Objects.equals(this.tradeTime, trade.tradeTime)
				&& Objects.equals(this.traderId, trade.traderId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id, this.age, this.action, this.price, this.shares,
				this.symbol, this.tradeTime,
				this.traderId);
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getAge() {
		return this.age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getAction() {
		return this.action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public Instant getTradeTime() {
		return this.tradeTime;
	}

	public void setTradeTime(Instant tradeTime) {
		this.tradeTime = tradeTime;
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

	public String getTraderId() {
		return this.traderId;
	}

	public void setTraderId(String traderId) {
		this.traderId = traderId;
	}

	@Override
	public String toString() {
		return "Trade{" + "id='" + this.id + '\'' + ", action='" + this.action + '\''
				+ ", age=" + this.age + ", price=" + this.price + ", shares="
				+ this.shares + ", symbol='" + this.symbol + ", tradeTime="
				+ this.tradeTime + '\'' + ", traderId='" + this.traderId + '\'' + '}';
	}
}
