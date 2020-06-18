/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.spanner.test.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Embedded;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Interleaved;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Where;

/**
 * A test domain object using many features.
 *
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

	private LocalDate tradeLocalDate;

	private LocalDateTime tradeLocalDateTime;

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
	@Where("disabled = false")
	private List<SubTrade> subTrades = Collections.emptyList();

	/**
	 * Partial constructor. Intentionally tests a field that is left null sometimes.
	 * @param symbol the symbol.
	 * @param executionTimes the list of execution times.
	 */
	public Trade(String symbol, List<Instant> executionTimes) {
		this.symbol = symbol;
		this.executionTimes = executionTimes;
	}

	public static Trade aTrade() {
		return aTrade(null, 0);
	}

	public static Trade aTrade(String customTraderId, int subTrades) {
		return aTrade(customTraderId, subTrades, 0);
	}

	public static Trade aTrade(String customTraderId, int subTrades, int tradeTime) {
		Trade t = new Trade("ABCD", new ArrayList<>());
		String tradeId = UUID.randomUUID().toString();
		String traderId = customTraderId == null ? UUID.randomUUID().toString() : customTraderId;

		t.tradeDetail = new TradeDetail();

		t.tradeDetail.id = tradeId;
		t.age = 8;
		t.action = "BUY";
		t.traderId = traderId;
		t.tradeTime = Instant.ofEpochSecond(333 + tradeTime);
		t.tradeDate = Date.from(t.tradeTime);
		t.tradeLocalDate = LocalDate.of(2015, 1, 1);
		t.tradeLocalDateTime = LocalDateTime.of(2015, 1, 1, 2, 3, 4, 5);
		if (subTrades > 0) {
			t.setSubTrades(
					IntStream.range(0, subTrades)
							.mapToObj(i -> new SubTrade(t.getTradeDetail().getId(), t.getTraderId(), "subTrade" + i))
							.collect(Collectors.toList()));
		}
		t.tradeDetail.price = 100.0;
		t.tradeDetail.shares = 12345.6;
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
		return getAge() == trade.getAge() &&
				Objects.equals(getTradeTime(), trade.getTradeTime()) &&
				Objects.equals(getTradeDate(), trade.getTradeDate()) &&
				Objects.equals(this.tradeLocalDate, trade.tradeLocalDate) &&
				Objects.equals(this.tradeLocalDateTime, trade.tradeLocalDateTime) &&
				Objects.equals(getAction(), trade.getAction()) &&
				Objects.equals(getSymbol(), trade.getSymbol()) &&
				Objects.equals(getTradeDetail(), trade.getTradeDetail()) &&
				Objects.equals(getTraderId(), trade.getTraderId()) &&
				Objects.equals(getExecutionTimes(), trade.getExecutionTimes()) &&
				Objects.equals(getSubTrades(), trade.getSubTrades());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getAge(), getTradeTime(), getTradeDate(), this.tradeLocalDate, this.tradeLocalDateTime,
				getAction(), getSymbol(), getTradeDetail(), getTraderId(), getExecutionTimes(), getSubTrades());
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
		return "Trade{" +
				"age=" + this.age +
				", tradeTime=" + this.tradeTime +
				", tradeDate=" + this.tradeDate +
				", tradeLocalDate=" + this.tradeLocalDate +
				", tradeLocalDateTime=" + this.tradeLocalDateTime +
				", action='" + this.action + '\'' +
				", symbol='" + this.symbol + '\'' +
				", tradeDetail=" + this.tradeDetail +
				", traderId='" + this.traderId + '\'' +
				", executionTimes=" + this.executionTimes +
				", subTrades=" + this.subTrades +
				'}';
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
