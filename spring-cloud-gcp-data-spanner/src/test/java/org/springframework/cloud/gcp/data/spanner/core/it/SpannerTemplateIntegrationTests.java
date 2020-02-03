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

package org.springframework.cloud.gcp.data.spanner.core.it;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.spanner.core.SpannerPageableQueryOptions;
import org.springframework.cloud.gcp.data.spanner.core.SpannerReadOptions;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.test.AbstractSpannerIntegrationTest;
import org.springframework.cloud.gcp.data.spanner.test.domain.Trade;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that use many features of the Spanner Template.
 *
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
public class SpannerTemplateIntegrationTests extends AbstractSpannerIntegrationTest {

	/**
	 * used for checking exception messages and types.
	 */
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Autowired
	TemplateTransactionalService transactionalService;

	@Test
	public void testReadOnlyOperation() {
		// Integration tests are configured with 10 sessions max. This will hang and fail if there
		// is a leak.
		for (int i = 0; i < 20; i++) {
			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
				this.transactionalService.testReadOnlyOperation();
				return true;
			});
		}
	}

	@Test
	public void insertAndDeleteSequence() {

		this.spannerOperations.delete(Trade.class, KeySet.all());

		assertThat(this.spannerOperations.count(Trade.class)).isEqualTo(0L);

		Trade trade = Trade.aTrade(null, true);
		this.spannerOperations.insert(trade);
		assertThat(this.spannerOperations.count(Trade.class)).isEqualTo(1L);

		List<Trade> trades = this.spannerOperations.queryAll(Trade.class, new SpannerPageableQueryOptions());

		assertThat(trades).containsExactly(trade);

		Trade retrievedTrade = this.spannerOperations.read(Trade.class,
				Key.of(trade.getId(), trade.getTraderId()));
		assertThat(retrievedTrade).isEqualTo(trade);

		trades = this.spannerOperations.readAll(Trade.class);

		assertThat(trades).containsExactly(trade);

		Trade trade2 = Trade.aTrade(null, true);
		this.spannerOperations.insert(trade2);

		trades = this.spannerOperations.read(Trade.class, KeySet.newBuilder().addKey(Key.of(trade.getId(), trade.getTraderId()))
				.addKey(Key.of(trade2.getId(), trade2.getTraderId())).build());

		assertThat(trades).containsExactlyInAnyOrder(trade, trade2);

		this.spannerOperations.deleteAll(Arrays.asList(trade, trade2));
		assertThat(this.spannerOperations.count(Trade.class)).isEqualTo(0L);
	}

	@Test
	public void readWriteTransactionTest() {
		Trade trade = Trade.aTrade();
		this.spannerOperations.performReadWriteTransaction((transactionOperations) -> {

			long beforeCount = transactionOperations.count(Trade.class);
			transactionOperations.insert(trade);

			// because the insert happens within the same transaction, this count is unchanged.
			assertThat(transactionOperations.count(Trade.class)).isEqualTo(beforeCount);
			return null;
		});

		assertThat(this.spannerOperations.count(Trade.class)).isEqualTo(1L);
		this.transactionalService.testTransactionalAnnotation();
		assertThat(this.spannerOperations.count(Trade.class)).isEqualTo(2L);
	}

	@Test
	public void readOnlyTransactionTest() {

		this.expectedEx.expect(SpannerDataException.class);
		this.expectedEx.expectMessage("A read-only transaction template cannot perform mutations.");

		Trade trade = Trade.aTrade();
		this.spannerOperations.performReadOnlyTransaction((transactionOperations) -> {
			// cannot do mutate in a read-only transaction
			transactionOperations.insert(trade);
			return null;
		}, new SpannerReadOptions());
	}

	/**
	 * a transactional service for testing annotated transaction methods.
	 */
	public static class TemplateTransactionalService {

		@Autowired
		SpannerTemplate spannerTemplate;

		@Transactional
		public void testTransactionalAnnotation() {
			long beforeCount = this.spannerTemplate.count(Trade.class);
			Trade trade = Trade.aTrade();
			this.spannerTemplate.insert(trade);

			// because the insert happens within the same transaction, this count is unchanged.
			assertThat(this.spannerTemplate.count(Trade.class)).isEqualTo(beforeCount);
		}

		@Transactional(readOnly = true)
		public void testReadOnlyOperation() {
			this.spannerTemplate.count(Trade.class);
		}
	}
}
