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
import java.util.List;
import java.util.Map;

import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Value;
import org.junit.Test;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerColumn;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerTable;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.query.QueryMethod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
public class SpannerStatementQueryTests {

	@Test
	public void compoundNameConventionTest() {

		QueryMethod queryMethod = mock(QueryMethod.class);

		when(queryMethod.getName()).thenReturn(
				"findTop3DistinctByActionAndSymbolOrTraderIdAndPriceLessThanOrPriceGreater"
						+ "ThanEqualAndIdIsNotNullAndTraderIdIsNullAndTraderIdLikeAndPriceTrueAndPriceFalse"
						+ "AndPriceGreaterThanAndPriceLessThanEqualOrderByIdDesc");

		SpannerOperations spannerOperations = mock(SpannerOperations.class);

		PartTreeSpannerQuery partTreeSpannerQuery = new PartTreeSpannerQuery(Trade.class,
				queryMethod, spannerOperations, new SpannerMappingContext());

		Object[] params = new Object[]{
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

		when(spannerOperations.find(any(), (Statement) any(), any()))
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

		partTreeSpannerQuery.execute(params);
	}

	@Test(expected = IllegalArgumentException.class)
	public void unspecifiedParametersTest() {

		QueryMethod queryMethod = mock(QueryMethod.class);

		when(queryMethod.getName()).thenReturn(
				"findTop3DistinctIdActionPriceByActionAndSymbolOrTraderIdAndPriceLessThanOrPriceGreater"
						+ "ThanEqualAndIdIsNotNullAndTraderIdIsNullOrderByIdDesc");

		SpannerOperations spannerOperations = mock(SpannerOperations.class);

		PartTreeSpannerQuery partTreeSpannerQuery = new PartTreeSpannerQuery(Trade.class,
				queryMethod, spannerOperations, new SpannerMappingContext());

		// There are too few params specified, so the exception will occur.
		Object[] params = new Object[] {
				"BUY",
				"abcd",
				"abc123",
		};

		partTreeSpannerQuery.execute(params);
	}

	@Test(expected = IllegalArgumentException.class)
	public void unsupportedParamTypeTest() {

		QueryMethod queryMethod = mock(QueryMethod.class);

		when(queryMethod.getName()).thenReturn(
				"findTop3DistinctIdActionPriceByActionAndSymbolOrTraderIdAndPriceLessThanOrPriceGreater"
						+ "ThanEqualAndIdIsNotNullAndTraderIdIsNullOrderByIdDesc");

		SpannerOperations spannerOperations = mock(SpannerOperations.class);

		PartTreeSpannerQuery partTreeSpannerQuery = new PartTreeSpannerQuery(Trade.class,
				queryMethod, spannerOperations, new SpannerMappingContext());

		Object[] params = new Object[] {
				"BUY",
				"abcd",
				"abc123",
				8.88,
				3.33,
				new Trade(), // This parameter is an unsupported type for Spanner SQL.
				"ignored",
		};

		partTreeSpannerQuery.execute(params);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void deleteTest() {

		QueryMethod queryMethod = mock(QueryMethod.class);

		when(queryMethod.getName()).thenReturn(
				"deleteTop3DistinctIdActionPriceByActionAndSymbolOrTraderIdAndPriceLessThanOrPriceGreater"
						+ "ThanEqualAndIdIsNotNullAndTraderIdIsNullOrderByIdDesc");

		SpannerOperations spannerOperations = mock(SpannerOperations.class);

		PartTreeSpannerQuery partTreeSpannerQuery = new PartTreeSpannerQuery(Trade.class,
				queryMethod, spannerOperations, new SpannerMappingContext());

		Object[] params = new Object[0];

		partTreeSpannerQuery.execute(params);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unSupportedPredicateTest() {

		QueryMethod queryMethod = mock(QueryMethod.class);

		when(queryMethod.getName()).thenReturn("countByTraderIdBetween");

		SpannerOperations spannerOperations = mock(SpannerOperations.class);

		PartTreeSpannerQuery partTreeSpannerQuery = new PartTreeSpannerQuery(Trade.class,
				queryMethod, spannerOperations, new SpannerMappingContext());

		Object[] params = new Object[0];

		partTreeSpannerQuery.execute(params);
	}

	@Test
	public void countTest() {

		QueryMethod queryMethod = mock(QueryMethod.class);

		when(queryMethod.getName()).thenReturn("countByAction");

		SpannerOperations spannerOperations = mock(SpannerOperations.class);

		PartTreeSpannerQuery partTreeSpannerQuery = new PartTreeSpannerQuery(Trade.class,
				queryMethod, spannerOperations, new SpannerMappingContext());

		Object[] params = new Object[]{
				"BUY",
		};

		List<Trade> results = new ArrayList<>();
		results.add(new Trade());

		when(spannerOperations.find(any(), (Statement) any(), any()))
				.thenReturn((List) results);

		assertEquals(1, partTreeSpannerQuery.execute(params));
	}

	@Test
	public void existsTest() {

		QueryMethod queryMethod = mock(QueryMethod.class);

		when(queryMethod.getName()).thenReturn("existsByAction");

		SpannerOperations spannerOperations = mock(SpannerOperations.class);

		PartTreeSpannerQuery partTreeSpannerQuery = new PartTreeSpannerQuery(Trade.class,
				queryMethod, spannerOperations, new SpannerMappingContext());

		Object[] params = new Object[]{
				"BUY",
		};

		List<Trade> results = new ArrayList<>();
		results.add(new Trade());

		when(spannerOperations.find(any(), (Statement) any(), any()))
				.thenReturn((List) results);

		assertTrue((boolean) partTreeSpannerQuery.execute(params));

		results.clear();

		assertFalse((boolean) partTreeSpannerQuery.execute(params));
	}

	@Test
	public void getQueryMethodTest() {

		QueryMethod queryMethod = mock(QueryMethod.class);

		when(queryMethod.getName()).thenReturn("existsByAction");

		SpannerOperations spannerOperations = mock(SpannerOperations.class);

		PartTreeSpannerQuery partTreeSpannerQuery = new PartTreeSpannerQuery(Trade.class,
				queryMethod, spannerOperations, new SpannerMappingContext());

		assertSame(queryMethod, partTreeSpannerQuery.getQueryMethod());
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
