/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.spanner.repository.it;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import com.google.cloud.spanner.KeySet;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.spanner.test.AbstractSpannerIntegrationTest;
import org.springframework.cloud.gcp.data.spanner.test.domain.Trade;
import org.springframework.cloud.gcp.data.spanner.test.domain.TradeRepository;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests multiple threads using a single repository instance.
 *
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
public class ParallelSpannerRepositoryIntegrationTests extends AbstractSpannerIntegrationTest {

	private final static int PARALLEL_OPERATIONS = 2;

	@Autowired
	TradeRepository tradeRepository;

	@Test
	public void testParallelOperations() {

		this.spannerOperations.delete(Trade.class, KeySet.all());

		this.tradeRepository.performReadWriteTransaction(repo -> {

			executeInParallel(unused -> {
				repo.save(Trade.aTrade());

				// all of the threads are using the same transaction at the same time, so they all still
				// see empty table
				assertThat(repo.count()).isZero();
			});
			assertThat(repo.count()).isZero();
			return null;
		});

		executeInParallel(unused -> assertThat(this.tradeRepository.countByAction("BUY")).isEqualTo(PARALLEL_OPERATIONS));

		executeInParallel(index -> this.tradeRepository.updateActionTradeById(
				((List<Trade>) this.tradeRepository.findAll()).get(index).getId(),
				"SELL"));

		executeInParallel(unused -> assertThat(this.tradeRepository.countByAction("SELL")).isEqualTo(PARALLEL_OPERATIONS));

		executeInParallel(unused -> assertThat(this.tradeRepository.countByActionQuery("SELL")).isEqualTo(PARALLEL_OPERATIONS));
	}

	private void executeInParallel(IntConsumer function) {
		IntStream.range(0, PARALLEL_OPERATIONS).parallel().forEach(function);
	}
}
