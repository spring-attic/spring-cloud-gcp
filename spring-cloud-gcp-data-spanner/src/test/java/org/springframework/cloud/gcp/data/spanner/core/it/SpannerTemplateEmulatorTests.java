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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.cloud.gcp.data.spanner.test.AbstractSpannerIntegrationTest;
import org.springframework.cloud.gcp.data.spanner.test.domain.Trade;
import org.springframework.cloud.gcp.test.spanner.SpannerEmulatorSpringConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that use many features of the Spanner Template.
 *
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {SpannerEmulatorSpringConfiguration.class})
public class SpannerTemplateEmulatorTests extends AbstractSpannerIntegrationTest {

	@Test
	public void insertSingleRow() {

		Trade trade = Trade.aTrade(null, 1);
		this.spannerOperations.insert(trade);
		assertThat(this.spannerOperations.count(Trade.class)).isEqualTo(1L);
	}

}
