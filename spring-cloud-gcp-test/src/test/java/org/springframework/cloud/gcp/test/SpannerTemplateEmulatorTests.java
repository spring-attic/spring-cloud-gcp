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

package org.springframework.cloud.gcp.test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Assumptions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerDatabaseAdminTemplate;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;
import org.springframework.cloud.gcp.test.spanner.SpannerEmulatorSpringConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Spanner emulator tests.
 *
 * @author Dmitry Solomakha
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {SpannerEmulatorSpringConfiguration.class, SpannerTestConfiguration.class})
public class SpannerTemplateEmulatorTests {
	@Autowired
	private SpannerTemplate spannerTemplate;

	@Autowired
	private SpannerDatabaseAdminTemplate spannerDatabaseAdminTemplate;

	@Autowired
	private SpannerSchemaUtils spannerSchemaUtils;

	private static final Log LOGGER = LogFactory.getLog(SpannerTemplateEmulatorTests.class);

	@BeforeClass
	public static void checkToRun() {
		Assumptions.assumeThat(System.getProperty("it.emulator"))
				.as("Spanner emulator tests are disabled. "
						+ "Please use '-Dit.emulator=true' to enable them. ")
				.isEqualTo("true");
	}

	@Test
	public void insertSingleRow() {
		createDatabaseWithSchema();
		Trade trade = Trade.aTrade(null, 1);
		this.spannerTemplate.insert(trade);
		Assertions.assertThat(this.spannerTemplate.count(Trade.class)).isEqualTo(1L);
	}

	private void createDatabaseWithSchema() {
		List<String> createStatements = new ArrayList<>(this.spannerSchemaUtils
				.getCreateTableDdlStringsForInterleavedHierarchy(Trade.class));

		if (!this.spannerDatabaseAdminTemplate.databaseExists()) {
			LOGGER.debug(
					this.getClass() + " - Integration database created with schema: "
							+ createStatements);
			this.spannerDatabaseAdminTemplate.executeDdlStrings(createStatements, true);
		}
		else {
			LOGGER.debug(
					this.getClass() + " - schema created: " + createStatements);
			this.spannerDatabaseAdminTemplate.executeDdlStrings(createStatements, false);
		}
	}

	@Table
	public static class Trade {

		@Column(nullable = false)
		private int age;

		private Instant tradeTime;

		private Date tradeDate;

		private LocalDate tradeLocalDate;

		private LocalDateTime tradeLocalDateTime;

		private String action;

		private String symbol;


		Trade(String symbol, List<Instant> executionTimes) {
			this.symbol = symbol;
		}

		public static Trade aTrade() {
			return aTrade(null, 0);
		}

		static Trade aTrade(String customTraderId, int subTrades) {
			Trade t = new Trade("ABCD", new ArrayList<>());

			t.age = 8;
			t.action = "BUY";
			t.tradeTime = Instant.ofEpochSecond(333);
			t.tradeDate = Date.from(t.tradeTime);
			t.tradeLocalDate = LocalDate.of(2015, 1, 1);
			t.tradeLocalDateTime = LocalDateTime.of(2015, 1, 1, 2, 3, 4, 5);

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
			return age == trade.age &&
					Objects.equals(tradeTime, trade.tradeTime) &&
					Objects.equals(tradeDate, trade.tradeDate) &&
					Objects.equals(tradeLocalDate, trade.tradeLocalDate) &&
					Objects.equals(tradeLocalDateTime, trade.tradeLocalDateTime) &&
					Objects.equals(action, trade.action) &&
					Objects.equals(symbol, trade.symbol);
		}

		@Override
		public int hashCode() {
			return Objects.hash(age, tradeTime, tradeDate, tradeLocalDate, tradeLocalDateTime, action, symbol);
		}
	}
}
