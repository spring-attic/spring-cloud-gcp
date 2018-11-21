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

import java.util.List;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.Struct;

import org.springframework.cloud.gcp.data.spanner.repository.SpannerRepository;
import org.springframework.cloud.gcp.data.spanner.repository.query.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

/**
 * @author Chengyuan Zhao
 */
public interface TradeRepository extends SpannerRepository<Trade, Key> {

	List<Trade> findByTraderId(String traderId);

	int countByAction(String action);

	// deletes with the given action string, and returns the number of deleted items.
	int deleteByAction(String action);

	// deletes and returns the items that were deleted.
	List<Trade> deleteBySymbol(String symbol);

	void deleteBySymbolAndAction(String symbol, String action);

	@Query(" select count(1) from :org.springframework.cloud.gcp.data.spanner.test.domain.Trade: "
			+ "where action = @action")
	int countByActionQuery(@Param("action") String action);

	@Query("SELECT EXISTS(select * from "
			+ ":org.springframework.cloud.gcp.data.spanner.test.domain.Trade: "
			+ "where action = @action limit 1)")
	boolean existsByActionQuery(@Param("action") String action);

	@Query("select action from :org.springframework.cloud.gcp.data.spanner.test.domain.Trade: "
			+ "where action = @action limit 1")
	String getFirstString(@Param("action") String action);

	@Query("SELECT * FROM :org.springframework.cloud.gcp.data.spanner.test.domain.Trade:"
			+ " WHERE action=@action AND action=#{#action} ORDER BY action desc limit 1")
	Trade getOneTrade(@Param("action") String action);

	@Query("select action from :org.springframework.cloud.gcp.data.spanner.test.domain.Trade: "
			+ "where action = @action")
	List<String> getFirstStringList(@Param("action") String action);

	@Query("SELECT * FROM :org.springframework.cloud.gcp.data.spanner.test.domain.Trade:"
			+ " WHERE action=@action AND action=#{#action} ORDER BY action desc")
	List<Trade> annotatedTradesByAction(@Param("action") String action);

	List<TradeProjection> findByActionIgnoreCase(String action);

	// The sort is defined in the query string here, but can be overriden by the Pageable
	// param.
	@Query("SELECT * FROM :org.springframework.cloud.gcp.data.spanner.test.domain.Trade:"
			+ " ORDER BY LOWER(action) DESC")
	List<Trade> sortedTrades(Pageable pageable);

	List<Trade> findBySymbolLike(String symbolFragment);

	List<Trade> findBySymbolContains(String symbolFragment);

	List<Trade> findBySymbolNotLike(String symbolFragment);

	List<Trade> findBySymbolNotContains(String symbolFragment);

	@Query("SELECT * FROM :org.springframework.cloud.gcp.data.spanner.test.domain.Trade:"
			+ " WHERE STRUCT(symbol,action) = @pairTag ORDER BY LOWER(action) DESC")
	List<Trade> findBySymbolAndActionStruct(@Param("pairTag") Struct symbolAction);

	@Query("SELECT * FROM :org.springframework.cloud.gcp.data.spanner.test.domain.Trade:"
			+ " WHERE STRUCT(symbol,action) = @pairTag ORDER BY LOWER(action) DESC")
	List<Trade> findBySymbolAndActionPojo(@Param("pairTag") SymbolAction symbolAction);
}
