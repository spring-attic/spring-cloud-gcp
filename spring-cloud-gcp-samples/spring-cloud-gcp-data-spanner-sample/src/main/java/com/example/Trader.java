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

package com.example;

import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;

/**
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

	public Trader() {
	}

	public Trader(String id, String firstName, String lastName) {
		this.traderId = id;
		this.firstName = firstName;
		this.lastName = lastName;
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
				'}';
	}
}
