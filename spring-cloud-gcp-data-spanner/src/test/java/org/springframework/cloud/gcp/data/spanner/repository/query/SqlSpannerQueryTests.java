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

import java.util.Map;
import java.util.Optional;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Value;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.cloud.gcp.data.spanner.core.SpannerMutationFactory;
import org.springframework.cloud.gcp.data.spanner.core.SpannerQueryOptions;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerEntityProcessor;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
public class SqlSpannerQueryTests {

	private SpannerTemplate spannerTemplate;

	private SpannerQueryMethod queryMethod;

	private QueryMethodEvaluationContextProvider evaluationContextProvider;

	private SpelExpressionParser expressionParser;

	private SpannerMappingContext spannerMappingContext = new SpannerMappingContext();

	private final Sort sort = Sort.by(Order.asc("COLA"), Order.desc("COLB"));

	private final Pageable pageable = PageRequest.of(3, 10, this.sort);

	private final SpannerEntityProcessor spannerEntityProcessor = mock(
			SpannerEntityProcessor.class);

	@Before
	public void initMocks() {
		this.queryMethod = mock(SpannerQueryMethod.class);
		this.spannerTemplate = spy(new SpannerTemplate(mock(DatabaseClient.class),
				this.spannerMappingContext, this.spannerEntityProcessor,
				mock(SpannerMutationFactory.class), new SpannerSchemaUtils(
						this.spannerMappingContext, this.spannerEntityProcessor, true)));
		this.expressionParser = new SpelExpressionParser();
		this.evaluationContextProvider = mock(QueryMethodEvaluationContextProvider.class);
	}

	private SqlSpannerQuery<Trade> createQuery(String sql) {
		return new SqlSpannerQuery<Trade>(Trade.class, this.queryMethod,
				this.spannerTemplate,
				sql, this.evaluationContextProvider, this.expressionParser,
				new SpannerMappingContext());
	}

	@Test
	public void compoundNameConventionTest() {

		String sql = "SELECT DISTINCT * FROM "
				+ ":org.springframework.cloud.gcp.data.spanner.repository.query.SqlSpannerQueryTests$Trade:"
				+ "@{index=fakeindex}"
				+ " WHERE price=#{#tag3 * -1} AND price<>#{#tag3 * -1} OR "
				+ "price<>#{#tag4 * -1} AND " + "( action=@tag0 AND ticker=@tag1 ) OR "
				+ "( trader_id=@tag2 AND price<@tag3 ) OR ( price>=@tag4 AND id<>NULL AND "
				+ "trader_id=NULL AND trader_id LIKE %@tag5 AND price=TRUE AND price=FALSE AND "
				+ "struct_val = @tag8 AND struct_val = @tag9 "
				+ "price>@tag6 AND price<=@tag7 )ORDER BY id DESC LIMIT 3;";

		String entityResolvedSql = "SELECT * FROM (SELECT DISTINCT * FROM " + "trades@{index=fakeindex}"
				+ " WHERE price=@SpELtag1 AND price<>@SpELtag1 OR price<>@SpELtag2 AND "
				+ "( action=@tag0 AND ticker=@tag1 ) OR "
				+ "( trader_id=@tag2 AND price<@tag3 ) OR ( price>=@tag4 AND id<>NULL AND "
				+ "trader_id=NULL AND trader_id LIKE %@tag5 AND price=TRUE AND price=FALSE AND "
				+ "struct_val = @tag8 AND struct_val = @tag9 "
				+ "price>@tag6 AND price<=@tag7 )ORDER BY id DESC LIMIT 3) "
				+ "ORDER BY COLA ASC , COLB DESC LIMIT 10 OFFSET 30";

		Object[] params = new Object[] { "BUY", this.pageable, "abcd", "abc123", 8.88,
				3.33, "blahblah",
				1.11, 2.22, Struct.newBuilder().set("symbol").to("ABCD").set("action")
						.to("BUY").build(),
				new SymbolAction("ABCD", "BUY") };

		String[] paramNames = new String[] { "tag0", "ignoredPageable", "tag1", "tag2",
				"tag3", "tag4",
				"tag5", "tag6", "tag7", "tag8", "tag9" };

		Parameters parameters = mock(Parameters.class);

		// @formatter:off
		Mockito.<Parameters>when(this.queryMethod.getParameters())
				.thenReturn(parameters);
		// @formatter:on

		when(parameters.getNumberOfParameters()).thenReturn(paramNames.length);
		when(parameters.getParameter(anyInt())).thenAnswer(invocation -> {
			int index = invocation.getArgument(0);
			Parameter param = mock(Parameter.class);
			when(param.getName()).thenReturn(Optional.of(paramNames[index]));

			// @formatter:off
			Mockito.<Class>when(param.getType()).thenReturn(params[index].getClass());
			// @formatter:on;

			return param;
		});

		EvaluationContext evaluationContext = new StandardEvaluationContext();
		for (int i = 0; i < params.length; i++) {
			evaluationContext.setVariable(paramNames[i], params[i]);
		}
		when(this.evaluationContextProvider.getEvaluationContext(any(), any()))
				.thenReturn(evaluationContext);

		SqlSpannerQuery sqlSpannerQuery = createQuery(sql);

		doAnswer(invocation -> {
			Statement statement = invocation.getArgument(0);
			SpannerQueryOptions queryOptions = invocation.getArgument(1);
			assertTrue(queryOptions.isAllowPartialRead());

			assertEquals(entityResolvedSql, statement.getSql());

			Map<String, Value> paramMap = statement.getParameters();

			assertEquals(params[0], paramMap.get("tag0").getString());
			//params[1] is this.pageable that is ignored, hence no synthetic tag is created for it
			assertEquals(params[2], paramMap.get("tag1").getString());
			assertEquals(params[3], paramMap.get("tag2").getString());
			assertEquals(params[4], paramMap.get("tag3").getFloat64());
			assertEquals(params[5], paramMap.get("tag4").getFloat64());
			assertEquals(params[6], paramMap.get("tag5").getString());
			assertEquals(params[7], paramMap.get("tag6").getFloat64());
			assertEquals(params[8], paramMap.get("tag7").getFloat64());
			assertEquals(params[9], paramMap.get("tag8").getStruct());
			verify(this.spannerEntityProcessor, times(1)).write(same(params[10]), any());
			assertEquals(-8.88, paramMap.get("SpELtag1").getFloat64(), 0.00001);
			assertEquals(-3.33, paramMap.get("SpELtag2").getFloat64(), 0.00001);

			return null;
		}).when(this.spannerTemplate).executeQuery(any(), any());

		sqlSpannerQuery.execute(params);

		verify(this.spannerTemplate, times(1)).executeQuery(any(), any());
	}

	@Test(expected = SpannerDataException.class)
	public void multiplePageableSortTest() {
		String sql = "SELECT * FROM table;";

		Parameters parameters = mock(Parameters.class);
		// @formatter:off
		Mockito.<Parameters>when(this.queryMethod.getParameters())
				.thenReturn(parameters);
		// @formatter:on
		when(parameters.getNumberOfParameters()).thenReturn(0);

		SqlSpannerQuery sqlSpannerQuery = createQuery(sql);

		sqlSpannerQuery.execute(new Object[] { this.pageable, this.sort });
	}

	@Test(expected = SpannerDataException.class)
	public void mutliplePageableTest() {
		String sql = "SELECT * FROM table;";

		Parameters parameters = mock(Parameters.class);
		// @formatter:off
		Mockito.<Parameters>when(this.queryMethod.getParameters())
				.thenReturn(parameters);
		// @formatter:on
		when(parameters.getNumberOfParameters()).thenReturn(0);

		SqlSpannerQuery sqlSpannerQuery = createQuery(sql);

		sqlSpannerQuery.execute(new Object[] { this.pageable, this.pageable });
	}

	@Test(expected = SpannerDataException.class)
	public void mutlipleSortTest() {
		String sql = "SELECT * FROM table;";

		Parameters parameters = mock(Parameters.class);
		// @formatter:off
		Mockito.<Parameters>when(this.queryMethod.getParameters())
				.thenReturn(parameters);
		// @formatter:on
		when(parameters.getNumberOfParameters()).thenReturn(0);

		SqlSpannerQuery sqlSpannerQuery = createQuery(sql);

		sqlSpannerQuery.execute(new Object[] { this.sort, this.sort });
	}

	private static class SymbolAction {
		String symbol;

		String action;

		SymbolAction(String s, String a) {
			this.symbol = s;
			this.action = a;
		}
	}

	@Table(name = "trades")
	private static class Trade {
		@PrimaryKey
		String id;

		String action;

		Double price;

		Double shares;

		@Column(name = "ticker")
		String symbol;

		@Column(name = "trader_id")
		String traderId;
	}
}
