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

package org.springframework.cloud.gcp.data.spanner.repository.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Value;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerColumn;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerTable;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.query.QueryMethod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 * @author Balint Pato
 */
public class SpannerStatementQueryTests {

	private static final Object[] EMPTY_PARAMETERS = new Object[0];

	private SpannerOperations spannerOperations;

	private QueryMethod queryMethod;

	private SpannerMappingContext spannerMappingContext;

	private PartTreeSpannerQuery partTreeSpannerQuery;

	@Before
	public void initMocks() {
		this.queryMethod = mock(QueryMethod.class);
		this.spannerOperations = mock(SpannerOperations.class);
		this.spannerMappingContext = new SpannerMappingContext();
	}

	private PartTreeSpannerQuery createQuery() {
		return new PartTreeSpannerQuery(Trade.class,
				this.queryMethod, this.spannerOperations, this.spannerMappingContext);
	}

	@Test
	public void compoundNameConventionTest() {
		when(this.queryMethod.getName()).thenReturn(
				"findTop3DistinctByActionAndSymbolOrTraderIdAndPriceLessThanOrPriceGreater"
						+ "ThanEqualAndIdIsNotNullAndTraderIdIsNullAndTraderIdLikeAndPriceTrueAndPriceFalse"
						+ "AndPriceGreaterThanAndPriceLessThanEqualOrderByIdDesc");
		this.partTreeSpannerQuery = createQuery();

		Object[] params = new Object[] {
				"BUY",
				"abcd",
				"abc123",
				8.88,
				3.33,
				"ignored",
				"ignored",
				"blahblah",
				"ignored",
				"ignored",
				1.11,
				2.22,
		};

		when(this.spannerOperations.find(any(), (Statement) any(), any()))
				.thenAnswer(invocation -> {
					Statement statement = invocation.getArgument(1);

					assertEquals(
							"SELECT DISTINCT * FROM trades WHERE ( action=@tag0 AND ticker=@tag1 ) OR "
									+ "( trader_id=@tag2 AND price<@tag3 ) OR ( price>=@tag4 AND id<>NULL AND "
									+ "trader_id=NULL AND trader_id LIKE %@tag7 AND price=TRUE AND price=FALSE AND "
									+ "price>@tag10 AND price<=@tag11 )ORDER BY id DESC LIMIT 3;",
							statement.getSql());

					Map<String, Value> paramMap = statement.getParameters();

					assertEquals(params[0], paramMap.get("tag0").getString());
					assertEquals(params[1], paramMap.get("tag1").getString());
					assertEquals(params[2], paramMap.get("tag2").getString());
					assertEquals(params[3], paramMap.get("tag3").getFloat64());
					assertEquals(params[4], paramMap.get("tag4").getFloat64());
					assertEquals(params[5], paramMap.get("tag5").getString());
					assertEquals(params[6], paramMap.get("tag6").getString());
					assertEquals(params[7], paramMap.get("tag7").getString());
					assertEquals(params[8], paramMap.get("tag8").getString());
					assertEquals(params[9], paramMap.get("tag9").getString());
					assertEquals(params[10], paramMap.get("tag10").getFloat64());
					assertEquals(params[11], paramMap.get("tag11").getFloat64());

					return null;
				});

		this.partTreeSpannerQuery.execute(params);
	}

	@Test(expected = IllegalArgumentException.class)
	public void unspecifiedParametersTest() {
		when(this.queryMethod.getName()).thenReturn(
				"findTop3DistinctIdActionPriceByActionAndSymbolOrTraderIdAndPriceLessThanOrPriceGreater"
						+ "ThanEqualAndIdIsNotNullAndTraderIdIsNullOrderByIdDesc");
		this.partTreeSpannerQuery = createQuery();

		// There are too few params specified, so the exception will occur.
		Object[] params = new Object[] {
				"BUY",
				"abcd",
				"abc123",
		};

		this.partTreeSpannerQuery.execute(params);
	}

	@Test(expected = IllegalArgumentException.class)
	public void unsupportedParamTypeTest() {
		when(this.queryMethod.getName()).thenReturn(
				"findTop3DistinctIdActionPriceByActionAndSymbolOrTraderIdAndPriceLessThanOrPriceGreater"
						+ "ThanEqualAndIdIsNotNullAndTraderIdIsNullOrderByIdDesc");
		this.partTreeSpannerQuery = createQuery();

		Object[] params = new Object[] {
				"BUY",
				"abcd",
				"abc123",
				8.88,
				3.33,
				new Trade(), // This parameter is an unsupported type for Spanner SQL.
				"ignored",
		};

		this.partTreeSpannerQuery.execute(params);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void deleteTest() {
		//delete is not supported
		when(this.queryMethod.getName()).thenReturn(
				"deleteTop3DistinctIdActionPriceByActionAndSymbolOrTraderIdAndPriceLessThanOrPriceGreater"
						+ "ThanEqualAndIdIsNotNullAndTraderIdIsNullOrderByIdDesc");

		this.partTreeSpannerQuery = createQuery();

		this.partTreeSpannerQuery.execute(EMPTY_PARAMETERS);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unSupportedPredicateTest() {
		when(this.queryMethod.getName()).thenReturn("countByTraderIdBetween");
		this.partTreeSpannerQuery = createQuery();
		this.partTreeSpannerQuery.execute(EMPTY_PARAMETERS);
	}

	@Test
	public void countShouldReturnSizeOfResultSet() {
		List<Trade> results = new ArrayList<>();
		results.add(new Trade());

		queryWithMockResult("countByAction", results);

		Object[] params = new Object[]{
				"BUY",
		};
		assertEquals(1, this.partTreeSpannerQuery.execute(params));
	}

	@Test
	public void existShouldBeTrueWhenResultSetIsNotEmpty() {
		List<Trade> results = new ArrayList<>();
		results.add(new Trade());

		queryWithMockResult("existsByAction", results);

		Object[] params = new Object[]{
				"BUY",
		};
		assertTrue((boolean) this.partTreeSpannerQuery.execute(params));
	}

	@Test
	public void existShouldBeFalseWhenResultSetIsEmpty() {
		queryWithMockResult("existsByAction", Collections.emptyList());

		Object[] params = new Object[]{
				"BUY",
		};
		assertFalse((boolean) this.partTreeSpannerQuery.execute(params));
	}

	private void queryWithMockResult(String queryName, List results) {
		when(this.queryMethod.getName()).thenReturn(queryName);
		this.partTreeSpannerQuery = createQuery();
		when(this.spannerOperations.find(any(), (Statement) any(), any()))
				.thenReturn(results);
	}

	@SpannerTable(name = "trades")
	private static class Trade {
		@Id
		String id;

		String action;

		Double price;

		Double shares;

		@SpannerColumn(name = "ticker")
		String symbol;

		@SpannerColumn(name = "trader_id")
		String traderId;
	}
}
