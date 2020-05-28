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

package org.springframework.cloud.gcp.data.spanner.repository.query;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Value;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerEntityProcessor;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerWriteConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.repository.query.DefaultParameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests Spanner statement queries.
 *
 * @author Chengyuan Zhao
 * @author Balint Pato
 */
public class SpannerStatementQueryTests {

	private static final Object[] EMPTY_PARAMETERS = new Object[0];

	private SpannerTemplate spannerTemplate;

	private SpannerQueryMethod queryMethod;

	private SpannerMappingContext spannerMappingContext;

	private PartTreeSpannerQuery partTreeSpannerQuery;

	/**
	 * Checks exceptions for messages and types.
	 */
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Before
	public void initMocks() throws NoSuchMethodException {
		this.queryMethod = mock(SpannerQueryMethod.class);
		// this is a dummy object. it is not mockable otherwise.
		Method method = Object.class.getMethod("toString");
		when(this.queryMethod.getMethod()).thenReturn(method);
		this.spannerTemplate = mock(SpannerTemplate.class);
		SpannerEntityProcessor spannerEntityProcessor = mock(SpannerEntityProcessor.class);
		when(this.spannerTemplate.getSpannerEntityProcessor()).thenReturn(spannerEntityProcessor);
		when(spannerEntityProcessor.getWriteConverter()).thenReturn(new SpannerWriteConverter());
		this.spannerMappingContext = new SpannerMappingContext();
	}

	private PartTreeSpannerQuery<Trade> createQuery() {
		return new PartTreeSpannerQuery<>(Trade.class, this.queryMethod,
				this.spannerTemplate, this.spannerMappingContext);
	}

	@Test
	public void compoundNameConventionTest() throws NoSuchMethodException {
		when(this.queryMethod.getName()).thenReturn(
				"findTop3DistinctByActionIgnoreCaseAndSymbolOrTraderIdAndPriceLessThanOrPriceGreater"
						+ "ThanEqualAndIdIsNotNullAndTraderIdIsNullAndTraderIdLikeAndPriceTrueAndPriceFalse"
						+ "AndPriceGreaterThanAndPriceLessThanEqualAndPriceInOrderByIdDesc");
		this.partTreeSpannerQuery = spy(createQuery());

		Object[] params = new Object[] { Trade.Action.BUY, "abcd", "abc123",
				8, // an int is not a natively supported type, and is intentionally used to use custom
					// converters
				3.33, "ignored",
				"ignored", "blahblah", "ignored", "ignored", 1.11, 2.22, Arrays.asList(1, 2) };

		when(this.spannerTemplate.query((Class<Object>) any(), any(), any()))
				.thenAnswer((invocation) -> {
					Statement statement = invocation.getArgument(1);

					String expectedQuery =
							"SELECT DISTINCT shares, trader_id, ticker, price, action, id "
									+ "FROM trades WHERE ( LOWER(action)=LOWER(@tag0) "
									+ "AND ticker=@tag1 ) OR "
									+ "( trader_id=@tag2 AND price<@tag3 ) OR ( price>=@tag4 AND id<>NULL AND "
									+ "trader_id=NULL AND trader_id LIKE @tag7 AND price=TRUE AND price=FALSE AND "
									+ "price>@tag10 AND price<=@tag11 AND price IN UNNEST(@tag12) ) ORDER BY id DESC LIMIT 3";

					assertThat(statement.getSql()).isEqualTo(expectedQuery);

					Map<String, Value> paramMap = statement.getParameters();

					assertThat(paramMap.get("tag0").getString()).isEqualTo(params[0].toString());
					assertThat(paramMap.get("tag1").getString()).isEqualTo(params[1]);
					assertThat(paramMap.get("tag2").getString()).isEqualTo(params[2]);
					assertThat(paramMap.get("tag3").getInt64()).isEqualTo(8L);
					assertThat(paramMap.get("tag4").getFloat64()).isEqualTo(params[4]);
					assertThat(paramMap.get("tag5").getString()).isEqualTo(params[5]);
					assertThat(paramMap.get("tag6").getString()).isEqualTo(params[6]);
					assertThat(paramMap.get("tag7").getString()).isEqualTo(params[7]);
					assertThat(paramMap.get("tag8").getString()).isEqualTo(params[8]);
					assertThat(paramMap.get("tag9").getString()).isEqualTo(params[9]);
					assertThat(paramMap.get("tag10").getFloat64()).isEqualTo(params[10]);
					assertThat(paramMap.get("tag11").getFloat64()).isEqualTo(params[11]);
					assertThat(paramMap.get("tag12").getInt64Array()).isEqualTo(Arrays.asList(1L, 2L));

					return null;
				});

		doReturn(Object.class).when(this.partTreeSpannerQuery)
				.getReturnedSimpleConvertableItemType();
		doReturn(null).when(this.partTreeSpannerQuery).convertToSimpleReturnType(any(),
				any());

		// This dummy method was created so the metadata for the ARRAY param inner type is
		// provided.
		Method method = QueryHolder.class.getMethod("repositoryMethod1", Object.class, Object.class, Object.class,
				Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class,
				Object.class, Object.class, List.class);
		when(this.queryMethod.getMethod()).thenReturn(method);
		doReturn(new DefaultParameters(method)).when(this.queryMethod).getParameters();

		this.partTreeSpannerQuery.execute(params);
		verify(this.spannerTemplate, times(1)).query((Class<Object>) any(), any(), any());
	}

	@Test
	public void compoundNameConventionCountTest() throws NoSuchMethodException {
		when(this.queryMethod.getName()).thenReturn(
				"existsDistinctByActionIgnoreCaseAndSymbolOrTraderIdAndPriceLessThanOrPriceGreater"
						+ "ThanEqualAndIdIsNotNullAndTraderIdIsNullAndTraderIdLikeAndPriceTrueAndPriceFalse"
						+ "AndPriceGreaterThanAndPriceLessThanEqualOrderByIdDesc");
		this.partTreeSpannerQuery = spy(createQuery());

		when(this.spannerTemplate.query((Function<Struct, Object>) any(), any(), any()))
				.thenReturn(Collections.singletonList(1L));

		Object[] params = new Object[] { Trade.Action.BUY, "abcd", "abc123", 8.88, 3.33, "ignored",
				"ignored", "blahblah", "ignored", "ignored", 1.11, 2.22, };
		Method method = QueryHolder.class.getMethod("repositoryMethod3",
				Object.class, Object.class, Object.class,
				Object.class, Object.class, Object.class,
				Object.class, Object.class, Object.class,
				Object.class, Object.class, Object.class);
		doReturn(new DefaultParameters(method)).when(this.queryMethod).getParameters();

		when(this.spannerTemplate.query((Function<Struct, Object>) any(), any(), any()))
				.thenAnswer((invocation) -> {
					Statement statement = invocation.getArgument(1);

					String expectedSql = "SELECT EXISTS"
							+ "(SELECT DISTINCT shares, trader_id, ticker, price, action, id "
							+ "FROM trades WHERE ( LOWER(action)=LOWER(@tag0) "
							+ "AND ticker=@tag1 ) OR "
							+ "( trader_id=@tag2 AND price<@tag3 ) OR ( price>=@tag4 AND id<>NULL AND "
							+ "trader_id=NULL AND trader_id LIKE @tag7 AND price=TRUE AND price=FALSE AND "
							+ "price>@tag10 AND price<=@tag11 ) ORDER BY id DESC LIMIT 1)";
					assertThat(statement.getSql()).isEqualTo(expectedSql);

					Map<String, Value> paramMap = statement.getParameters();

					assertThat(paramMap.get("tag0").getString()).isEqualTo(params[0].toString());
					assertThat(paramMap.get("tag1").getString()).isEqualTo(params[1]);
					assertThat(paramMap.get("tag2").getString()).isEqualTo(params[2]);
					assertThat(paramMap.get("tag3").getFloat64()).isEqualTo(params[3]);
					assertThat(paramMap.get("tag4").getFloat64()).isEqualTo(params[4]);
					assertThat(paramMap.get("tag5").getString()).isEqualTo(params[5]);
					assertThat(paramMap.get("tag6").getString()).isEqualTo(params[6]);
					assertThat(paramMap.get("tag7").getString()).isEqualTo(params[7]);
					assertThat(paramMap.get("tag8").getString()).isEqualTo(params[8]);
					assertThat(paramMap.get("tag9").getString()).isEqualTo(params[9]);
					assertThat(paramMap.get("tag10").getFloat64()).isEqualTo(params[10]);
					assertThat(paramMap.get("tag11").getFloat64()).isEqualTo(params[11]);

					return null;
				});

		doReturn(Object.class).when(this.partTreeSpannerQuery)
				.getReturnedSimpleConvertableItemType();
		doReturn(null).when(this.partTreeSpannerQuery).convertToSimpleReturnType(any(),
				any());

		this.partTreeSpannerQuery.execute(params);
		verify(this.spannerTemplate, times(1)).query((Function<Struct, Object>) any(),
				any(), any());
	}

	@Test
	public void pageableTest() throws NoSuchMethodException {
		Object[] params = new Object[] { 8.88, PageRequest.of(1, 10, Sort.by("traderId")) };
		Method method = QueryHolder.class.getMethod("repositoryMethod5",
				Double.class, Pageable.class);
		String expectedSql = "SELECT shares, trader_id, ticker, price, action, id "
				+ "FROM trades WHERE ( price<@tag0 ) "
				+ "ORDER BY trader_id ASC LIMIT 10 OFFSET 10";

		runPageableOrSortTest(params, method, expectedSql);
	}


	@Test
	public void sortTest() throws NoSuchMethodException {
		Object[] params = new Object[] { 8.88, Sort.by(Order.desc("traderId"), Order.asc("price"), Order.desc("action")) };
		Method method = QueryHolder.class.getMethod("repositoryMethod6",
				Double.class, Sort.class);
		String expectedSql = "SELECT shares, trader_id, ticker, price, action, id "
				+ "FROM trades WHERE ( price<@tag0 ) "
				+ "ORDER BY trader_id DESC , price ASC , action DESC";

		runPageableOrSortTest(params, method, expectedSql);
	}

	private void runPageableOrSortTest(Object[] params, Method method, String expectedSql) {
		when(this.queryMethod.getName()).thenReturn(
				"findByPriceLessThan");
		this.partTreeSpannerQuery = spy(createQuery());

		when(this.spannerTemplate.query((Function<Struct, Object>) any(), any(), any()))
				.thenReturn(Collections.singletonList(1L));

		doReturn(new DefaultParameters(method)).when(this.queryMethod).getParameters();

		when(this.spannerTemplate.query((Class) any(), any(), any()))
				.thenAnswer((invocation) -> {
					Statement statement = invocation.getArgument(1);

					assertThat(statement.getSql()).isEqualTo(expectedSql);

					Map<String, Value> paramMap = statement.getParameters();

					assertThat(paramMap.get("tag0").getFloat64()).isEqualTo(params[0]);
					assertThat(paramMap).hasSize(1);

					return null;
				});

		doReturn(Object.class).when(this.partTreeSpannerQuery)
				.getReturnedSimpleConvertableItemType();
		doReturn(null).when(this.partTreeSpannerQuery).convertToSimpleReturnType(any(),
				any());

		this.partTreeSpannerQuery.execute(params);
		verify(this.spannerTemplate, times(1)).query((Class) any(),
				any(), any());
	}

	@Test
	public void unspecifiedParametersTest() throws NoSuchMethodException {
		this.expectedEx.expect(IllegalArgumentException.class);
		this.expectedEx.expectMessage("The number of tags does not match the number of params.");
		when(this.queryMethod.getName()).thenReturn(
				"findTop3DistinctIdActionPriceByActionAndSymbolOrTraderIdAndPriceLessThanOrPriceGreater"
						+ "ThanEqualAndIdIsNotNullAndTraderIdIsNullOrderByIdDesc");
		this.partTreeSpannerQuery = createQuery();
		Method method = QueryHolder.class.getMethod("repositoryMethod4", Object.class, Object.class, Object.class);
		doReturn(new DefaultParameters(method)).when(this.queryMethod).getParameters();

		// There are too few params specified, so the exception will occur.
		Object[] params = new Object[] { "BUY", "abcd", "abc123", };

		this.partTreeSpannerQuery.execute(params);
	}

	@Test
	public void unsupportedParamTypeTest() throws NoSuchMethodException {
		this.expectedEx.expect(IllegalArgumentException.class);
		this.expectedEx.expectMessage("is not a supported type: class org.springframework." +
				"cloud.gcp.data.spanner.repository.query.SpannerStatementQueryTests$Trade");
		when(this.queryMethod.getName()).thenReturn(
				"findTop3DistinctIdActionPriceByActionAndSymbolOrTraderIdAndPriceLessThanOrPriceGreater"
						+ "ThanEqualAndIdIsNotNullAndTraderIdIsNullOrderByIdDesc");
		this.partTreeSpannerQuery = createQuery();
		Method method = QueryHolder.class.getMethod("repositoryMethod2", Object.class, Object.class, Object.class,
				Object.class, Object.class, Trade.class, Object.class);

		doReturn(new DefaultParameters(method)).when(this.queryMethod).getParameters();

		// This parameter is an unsupported type for Spanner SQL.
		Object[] params = new Object[] { "BUY", "abcd", "abc123", 8.88, 3.33, new Trade(), "ignored", };

		this.partTreeSpannerQuery.execute(params);
	}

	@Test
	public void unSupportedPredicateTest() throws NoSuchMethodException {
		this.expectedEx.expect(UnsupportedOperationException.class);
		this.expectedEx.expectMessage("The statement type: BETWEEN (2): [IsBetween, " +
				"Between] is not supported.");
		when(this.queryMethod.getName()).thenReturn("countByTraderIdBetween");
		Method method = Object.class.getMethod("toString");
		doReturn(new DefaultParameters(method)).when(this.queryMethod).getParameters();

		this.partTreeSpannerQuery = createQuery();
		this.partTreeSpannerQuery.execute(EMPTY_PARAMETERS);
	}

	@Table(name = "trades")
	private static class Trade {
		@PrimaryKey
		String id;

		Action action;

		Double price;

		Double shares;

		@Column(name = "ticker")
		String symbol;

		@Column(name = "trader_id")
		String traderId;

		enum Action {
			BUY,
			SELL
		}
	}

	//The methods in this class are used to emulate repository methods
	private static class QueryHolder {
		public long repositoryMethod1(Object tag0, Object tag1, Object tag2, Object tag3, Object tag4, Object tag5,
				Object tag6, Object tag7, Object tag8, Object tag9, Object tag10, Object tag11, List<Integer> tag12) {
			// tag12 is intentionally List<Integer> instead of List<Long> to trigger conversion.
			return 0;
		}

		public long repositoryMethod2(Object tag0, Object tag1, Object tag2, Object tag3, Object tag4, Trade tag5, Object tag6) {
			return 0;
		}

		public long repositoryMethod3(Object tag0, Object tag1, Object tag2, Object tag3, Object tag4, Object tag5,
				Object tag6, Object tag7, Object tag8, Object tag9, Object tag10, Object tag11) {
			return 0;
		}

		public long repositoryMethod4(Object tag0, Object tag1, Object tag2) {
			return 0;
		}

		public long repositoryMethod5(Double tag0, Pageable tag1) {
			return 0;
		}

		public long repositoryMethod6(Double tag0, Sort tag1) {
			return 0;
		}
	}
}
