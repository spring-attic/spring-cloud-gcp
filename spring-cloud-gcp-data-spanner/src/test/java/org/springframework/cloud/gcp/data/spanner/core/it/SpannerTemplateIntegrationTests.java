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

package org.springframework.cloud.gcp.data.spanner.core.it;

import com.google.cloud.spanner.Key;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.test.AbstractSpannerIntegrationTest;
import org.springframework.cloud.gcp.data.spanner.test.domain.Trade;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Balint Pato
 */

@RunWith(SpringRunner.class)
public class SpannerTemplateIntegrationTests extends AbstractSpannerIntegrationTest {

	@Autowired
	protected SpannerOperations spannerOperations;

	@Test
	public void insertAndDeleteSequence() {
		assertThat(this.spannerOperations.count(Trade.class), is(0L));

		Trade trade = Trade.aTrade();
		this.spannerOperations.insert(trade);
		assertThat(this.spannerOperations.count(Trade.class), is(1L));

		Trade retrievedTrade = this.spannerOperations.find(Trade.class, Key.of(trade.getId()));
		assertThat(retrievedTrade, is(trade));

		this.spannerOperations.delete(trade);
		assertThat(this.spannerOperations.count(Trade.class), is(0L));
	}

}
