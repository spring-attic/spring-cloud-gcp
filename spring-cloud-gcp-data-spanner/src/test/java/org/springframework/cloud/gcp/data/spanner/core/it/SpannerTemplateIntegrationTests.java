/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.spanner.core.it;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.spanner.core.SpannerReadOptions;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.test.AbstractSpannerIntegrationTest;
import org.springframework.cloud.gcp.data.spanner.test.domain.Trade;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
public class SpannerTemplateIntegrationTests extends AbstractSpannerIntegrationTest {

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Autowired
	TemplateTransactionalService transactionalService;

	@Test
	public void insertAndDeleteSequence() {

		this.spannerOperations.delete(Trade.class, KeySet.all());

		assertThat(this.spannerOperations.count(Trade.class), is(0L));

		Trade trade = Trade.aTrade();
		this.spannerOperations.insert(trade);
		assertThat(this.spannerOperations.count(Trade.class), is(1L));

		Trade retrievedTrade = this.spannerOperations.read(Trade.class,
				Key.of(trade.getId(), trade.getTraderId()));
		assertThat(retrievedTrade, is(trade));

		this.spannerOperations.delete(trade);
		assertThat(this.spannerOperations.count(Trade.class), is(0L));
	}

	@Test
	public void readWriteTransactionTest() {
		Trade trade = Trade.aTrade();
		this.spannerOperations.performReadWriteTransaction(transactionOperations -> {
			transactionOperations.insert(trade);

			// because the insert happens within the same transaction, this count is still
			// 0
			assertThat(transactionOperations.count(Trade.class), is(0L));
			return null;
		});

		// now that the transaction has completed, the count should be 1
		assertThat(this.spannerOperations.count(Trade.class), is(1L));
		this.transactionalService.testTransactionalAnnotation();
		assertThat(this.spannerOperations.count(Trade.class), is(2L));
	}

	@Test
	public void readOnlyTransactionTest() {

		this.expectedEx.expect(SpannerDataException.class);
		// The expectMessage calls below operate as `contains` and seperate calls are used to
		// because
		// the printed order of some components is can change randomly.
		this.expectedEx.expectMessage("Invalid (null or empty) field name returned for property " +
				"@org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey");
		this.expectedEx.expectMessage("value=1");
		this.expectedEx.expectMessage("keyOrder=1");
		this.expectedEx.expectMessage(
				"java.lang.String org.springframework.cloud.gcp.data.spanner.core.mapping." +
				"SpannerPersistentPropertyImplTests$TestEntity.id by class " +
				"org.springframework.data.mapping.model.FieldNamingStrategy$MockitoMock$");

		Trade trade = Trade.aTrade();
		this.spannerOperations.performReadOnlyTransaction(transactionOperations -> {
			// cannot do mutate in a read-only transaction
			transactionOperations.insert(trade);
			return null;
		}, new SpannerReadOptions());
	}

	public static class TemplateTransactionalService {

		@Autowired
		SpannerTemplate spannerTemplate;

		@Transactional
		public void testTransactionalAnnotation() {
			Trade trade = Trade.aTrade();
			this.spannerTemplate.insert(trade);

			// because the insert happens within the same transaction, this count is still
			// 1
			assertThat(this.spannerTemplate.count(Trade.class), is(1L));
		}
	}
}
