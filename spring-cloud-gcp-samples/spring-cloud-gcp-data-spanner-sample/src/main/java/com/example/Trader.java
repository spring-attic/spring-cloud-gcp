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

package com.example;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;

/**
 * A sample entity.
 *
 * @author Mike Eltsufin
 */
@Table(name = "traders")
public class Trader {
	@PrimaryKey
	@Column(name = "trader_id")
	private String traderId;

	@Column(name = "first_name")
	private String firstName;

	@Column(name = "last_name")
	private String lastName;

	@Column(name = "CREATED_ON")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MMM-dd HH:mm:ss z")
	private java.sql.Timestamp createdOn;

	@Column(name = "MODIFIED_ON")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MMM-dd HH:mm:ss z")
	private List<java.sql.Timestamp> modifiedOn;

	public Trader() {
	}

	public Trader(String traderId, String firstName, String lastName) {
		this.traderId = traderId;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public Trader(String traderId, String firstName, String lastName, Timestamp createdOn, List<Timestamp> modifiedOn) {
		this.traderId = traderId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.createdOn = createdOn;
		this.modifiedOn = modifiedOn;
	}

	public String getTraderId() {
		return this.traderId;
	}

	public void setTraderId(String traderId) {
		this.traderId = traderId;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return this.lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Override
	public String toString() {
		return "Trader{" +
				"traderId='" + this.traderId + '\'' +
				", firstName='" + this.firstName + '\'' +
				", lastName='" + this.lastName + '\'' +
				", createdOn=" + this.createdOn +
				", modifiedOn=" + this.modifiedOn +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Trader trader = (Trader) o;
		return Objects.equals(getTraderId(), trader.getTraderId()) &&
				Objects.equals(getFirstName(), trader.getFirstName()) &&
				Objects.equals(getLastName(), trader.getLastName()) &&
				Objects.equals(this.createdOn, trader.createdOn) &&
				Objects.equals(this.modifiedOn, trader.modifiedOn);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getTraderId(), getFirstName(), getLastName(), this.createdOn, this.modifiedOn);
	}
}
