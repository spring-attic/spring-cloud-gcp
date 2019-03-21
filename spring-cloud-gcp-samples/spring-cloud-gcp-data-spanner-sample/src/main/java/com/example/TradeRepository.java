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

import java.util.List;

import com.google.cloud.spanner.Key;

import org.springframework.cloud.gcp.data.spanner.repository.SpannerRepository;
import org.springframework.cloud.gcp.data.spanner.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * A sample repository.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 */
@RepositoryRestResource(collectionResourceRel = "trades", path = "trades")
public interface TradeRepository extends SpannerRepository<Trade, Key> {

	List<Trade> findTop3DistinctByActionAndSymbolOrTraderIdOrderBySymbolDesc(
			String action, String symbol, String traderId);

	List<Trade> findByAction(String action);

	@Query("SELECT * FROM trades limit 1")
	Trade getAnyOneTrade();

	@Query("SELECT trade_id from trades where action = @action")
	List<String> getTradeIds(@Param("action") String action);

	int countByAction(String action);

	// This method uses the query from the properties file instead of one generated based
	// on name.
	List<Trade> fetchByActionNamedQuery(@Param("tag0") String action);

}
