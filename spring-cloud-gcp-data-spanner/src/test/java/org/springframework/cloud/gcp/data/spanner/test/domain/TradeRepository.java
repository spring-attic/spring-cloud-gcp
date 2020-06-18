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

package org.springframework.cloud.gcp.data.spanner.test.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.Struct;

import org.springframework.cloud.gcp.data.spanner.repository.SpannerRepository;
import org.springframework.cloud.gcp.data.spanner.repository.query.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

/**
 * A repository for integration tests that holds many complex use cases.
 *
 * @author Chengyuan Zhao
 */

@Nonnull
public interface TradeRepository extends SpannerRepository<Trade, Key> {

	List<Trade> findByTraderId(String traderId);

	List<Trade> findByTraderId(String traderId, Pageable pageable);

	List<Trade> findTop2ByTraderIdOrderByTradeTimeAsc(String traderId, Pageable pageable);

	int countByAction(String action);

	// deletes with the given action string, and returns the number of deleted items.
	int deleteByAction(String action);

	// deletes and returns the items that were deleted.
	List<Trade> deleteBySymbol(String symbol);

	void deleteBySymbolAndAction(String symbol, String action);

	@Query("SELECT * FROM :org.springframework.cloud.gcp.data.spanner.test.domain.Trade: WHERE id = @id")
	Optional<Trade> fetchById(@Param("id") String id);

	@Query(dmlStatement = true, value = "UPDATE :org.springframework.cloud.gcp.data.spanner.test.domain.Trade:" +
			" set action = @action WHERE id = @id")
	long updateActionTradeById(@Param("id") String id, @Param("action") String act);

	@Query(" select count(1) from :org.springframework.cloud.gcp.data.spanner.test.domain.Trade: "
			+ "where action = @action")
	int countByActionQuery(@Param("action") String action);

	@Query("SELECT EXISTS(select * from "
			+ ":org.springframework.cloud.gcp.data.spanner.test.domain.Trade: "
			+ "where action = @action limit 1)")
	boolean existsByActionQuery(@Param("action") String action);

	@Query("select action from :org.springframework.cloud.gcp.data.spanner.test.domain.Trade: "
			+ "where action in (@action) limit 1")
	String getFirstString(@Param("action") String action);

	@Query("SELECT * FROM :org.springframework.cloud.gcp.data.spanner.test.domain.Trade:"
			+ " WHERE action=@action AND action=#{#action} ORDER BY action desc limit 1")
	Trade getOneTrade(@Param("action") String action);

	@Query("select action from :org.springframework.cloud.gcp.data.spanner.test.domain.Trade: "
			+ "where action = @action")
	List<String> getFirstStringList(@Param("action") String action);

	//The sort should be passed as a Pageable param - Spanner did not preserve the order
	//of a wrapped query that we will have at fetching of eager-interleaved fields of the Trade entity
	@Query("SELECT * FROM :org.springframework.cloud.gcp.data.spanner.test.domain.Trade:"
			+ " WHERE action=@action AND action=#{#action}")
	List<Trade> annotatedTradesByAction(@Param("action") String action, Pageable pageable);

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

	long countByActionIn(List<String> action);

	@Query("SELECT count(1) FROM :org.springframework.cloud.gcp.data.spanner.test.domain.Trade:" +
			" where action in unnest(@actions)")
	long countWithInQuery(@Param("actions") List<String> actions);

	List<Trade> findByActionIn(Set<String> action);

	@NonNull
	Trade getByAction(String s);
}
