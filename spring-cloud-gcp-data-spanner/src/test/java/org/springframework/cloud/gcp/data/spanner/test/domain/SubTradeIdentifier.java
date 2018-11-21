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

import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Embedded;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;

/**
 * An object that holds the components that identify a {@link SubTrade}. This is
 * intentionally not used in {@link SubTrade} but is used in {@link SubTradeComponent} to
 * test that nested embedded key properties are resolved properly.
 *
 * @author Chengyuan Zhao
 */
public class SubTradeIdentifier {

	@PrimaryKey
	@Embedded
	TradeIdentifier tradeIdentifier;

	@PrimaryKey(keyOrder = 2)
	@Column(name = "subTradeId")
	String sub_trade_id;
}
