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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.assertj.core.util.DateUtil;

import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Embedded;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Interleaved;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;

/**
 * @author Ray Tsang
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
@Table(name = "#{'trades_'.concat(tableNameSuffix)}")
public class Trade {

	@Column(nullable = false)
	private int age;

	private Instant tradeTime;

	private Date tradeDate;

	private String action;

	private String symbol;

	@Embedded
	@PrimaryKey
	private TradeDetail tradeDetail;

	@PrimaryKey(keyOrder = 2)
	@Column(name = "trader_id")
	private String traderId;

	private List<Instant> executionTimes;

	@Interleaved
	private List<SubTrade> subTrades;

	public static Trade aTrade() {
		Trade t = new Trade();
		String tradeId = UUID.randomUUID().toString();
		String traderId = UUID.randomUUID().toString();

		t.tradeDetail = new TradeDetail();

		t.tradeDetail.id = tradeId;
		t.age = 8;
		t.symbol = "ABCD";
		t.action = "BUY";
		t.traderId = traderId;
		t.tradeTime = Instant.ofEpochSecond(333);
		t.tradeDate = Date.from(t.tradeTime);
		t.tradeDetail.price = 100.0;
		t.tradeDetail.shares = 12345.6;
		t.executionTimes = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			t.executionTimes.add(Instant.ofEpochSecond(i));
		}
		return t;
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
		return Objects.equals(this.tradeDetail.id, trade.tradeDetail.id)
				&& Objects.equals(this.age, trade.age)
				&& Objects.equals(this.action, trade.action)
				&& Objects.equals(this.tradeDetail.price, trade.tradeDetail.price)
				&& Objects.equals(this.tradeDetail.shares, trade.tradeDetail.shares)
				&& Objects.equals(this.symbol, trade.symbol)
				&& Objects.equals(this.tradeTime, trade.tradeTime)
				&& Objects.equals(this.traderId, trade.traderId)
				// java Date contains the time of day, but Cloud Spanner Date is only specific
				// to the day.
				&& Objects.equals(DateUtil.truncateTime(this.tradeDate),
						DateUtil.truncateTime(trade.tradeDate));
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.tradeDetail.id, this.age, this.action,
				this.tradeDetail.price, this.tradeDetail.shares, this.symbol,
				this.tradeTime, DateUtil.truncateTime(this.tradeDate),
				this.traderId, this.executionTimes);
	}

	public String getId() {
		return this.tradeDetail.id;
	}

	public void setId(String id) {
		this.tradeDetail.id = id;
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
		return this.tradeDetail.price;
	}

	public void setPrice(Double price) {
		this.tradeDetail.price = price;
	}

	public Double getShares() {
		return this.tradeDetail.shares;
	}

	public void setShares(Double shares) {
		this.tradeDetail.shares = shares;
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

	public List<SubTrade> getSubTrades() {
		return this.subTrades;
	}

	public void setSubTrades(List<SubTrade> subTrades) {
		this.subTrades = subTrades;
	}

	public TradeDetail getTradeDetail() {
		return this.tradeDetail;
	}

	public void setTradeDetail(TradeDetail tradeDetail) {
		this.tradeDetail = tradeDetail;
	}

	@Override
	public String toString() {
		return "Trade{" + "id='" + this.tradeDetail.id + '\'' + ", action='" + this.action
				+ '\'' + ", age=" + this.age + ", price=" + this.tradeDetail.price
				+ ", shares=" + this.tradeDetail.shares + ", symbol='"
				+ this.symbol + ", tradeTime="
				+ this.tradeTime + ", tradeDate='" + DateUtil.truncateTime(this.tradeDate)
				+ '\'' + ", traderId='" + this.traderId + '\'' + '}';
	}

	public Date getTradeDate() {
		return this.tradeDate;
	}

	public void setTradeDate(Date tradeDate) {
		this.tradeDate = tradeDate;
	}

	public List<Instant> getExecutionTimes() {
		return this.executionTimes;
	}

	public void setExecutionTimes(List<Instant> executionTimes) {
		this.executionTimes = executionTimes;
	}
}
