/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.spanner.repository.query;

import java.util.Map;
import java.util.Optional;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Value;
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests Spanner SQL Query Methods.
 *
 * @author Chengyuan Zhao
 */
public class SqlSpannerQueryTests {

	private static final Offset<Double> DELTA = Offset.offset(0.00001);

	private SpannerTemplate spannerTemplate;

	private SpannerQueryMethod queryMethod;

	private QueryMethodEvaluationContextProvider evaluationContextProvider;

	private SpelExpressionParser expressionParser;

	private SpannerMappingContext spannerMappingContext = new SpannerMappingContext();

	private final Sort sort = Sort.by(Order.asc("COLA"), Order.desc("COLB"));

	private final Pageable pageable = PageRequest.of(3, 10, this.sort);

	private final SpannerEntityProcessor spannerEntityProcessor = mock(
			SpannerEntityProcessor.class);

	/**
	 * checks messages and types for exceptions.
	 */
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

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

	private SqlSpannerQuery<Trade> createQuery(String sql, boolean isDml) {
		return new SqlSpannerQuery<Trade>(Trade.class, this.queryMethod,
				this.spannerTemplate,
				sql, this.evaluationContextProvider, this.expressionParser,
				new SpannerMappingContext(), isDml);
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
		when(parameters.getParameter(anyInt())).thenAnswer((invocation) -> {
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

		SqlSpannerQuery sqlSpannerQuery = createQuery(sql, false);

		doAnswer((invocation) -> {
			Statement statement = invocation.getArgument(0);
			SpannerQueryOptions queryOptions = invocation.getArgument(1);
			assertThat(queryOptions.isAllowPartialRead()).isTrue();

			assertThat(statement.getSql()).isEqualTo(entityResolvedSql);

			Map<String, Value> paramMap = statement.getParameters();

			assertThat(paramMap.get("tag0").getString()).isEqualTo(params[0]);
			//params[1] is this.pageable that is ignored, hence no synthetic tag is created for it
			assertThat(paramMap.get("tag1").getString()).isEqualTo(params[2]);
			assertThat(paramMap.get("tag2").getString()).isEqualTo(params[3]);
			assertThat(paramMap.get("tag3").getFloat64()).isEqualTo(params[4]);
			assertThat(paramMap.get("tag4").getFloat64()).isEqualTo(params[5]);
			assertThat(paramMap.get("tag5").getString()).isEqualTo(params[6]);
			assertThat(paramMap.get("tag6").getFloat64()).isEqualTo(params[7]);
			assertThat(paramMap.get("tag7").getFloat64()).isEqualTo(params[8]);
			assertThat(paramMap.get("tag8").getStruct()).isEqualTo(params[9]);
			verify(this.spannerEntityProcessor, times(1)).write(same(params[10]), any());

			assertThat(paramMap.get("SpELtag1").getFloat64()).isEqualTo(-8.88, DELTA);
			assertThat(paramMap.get("SpELtag2").getFloat64()).isEqualTo(-3.33, DELTA);

			return null;
		}).when(this.spannerTemplate).executeQuery(any(), any());

		sqlSpannerQuery.execute(params);

		verify(this.spannerTemplate, times(1)).executeQuery(any(), any());
	}

	@Test
	public void multiplePageableSortTest() {
		this.expectedEx.expect(SpannerDataException.class);
		this.expectedEx.expectMessage("Only a single Pageable or Sort param is allowed.");
		String sql = "SELECT * FROM table;";

		Parameters parameters = mock(Parameters.class);
		// @formatter:off
		Mockito.<Parameters>when(this.queryMethod.getParameters())
				.thenReturn(parameters);
		// @formatter:on
		when(parameters.getNumberOfParameters()).thenReturn(0);

		SqlSpannerQuery sqlSpannerQuery = createQuery(sql, false);

		sqlSpannerQuery.execute(new Object[] { this.pageable, this.sort });
	}

	@Test
	public void mutliplePageableTest() {
		this.expectedEx.expect(SpannerDataException.class);
		this.expectedEx.expectMessage("Only a single Pageable or Sort param is allowed.");
		String sql = "SELECT * FROM table;";

		Parameters parameters = mock(Parameters.class);
		// @formatter:off
		Mockito.<Parameters>when(this.queryMethod.getParameters())
				.thenReturn(parameters);
		// @formatter:on
		when(parameters.getNumberOfParameters()).thenReturn(0);

		SqlSpannerQuery sqlSpannerQuery = createQuery(sql, false);

		sqlSpannerQuery.execute(new Object[] { this.pageable, this.pageable });
	}

	@Test
	public void mutlipleSortTest() {
		this.expectedEx.expect(SpannerDataException.class);
		this.expectedEx.expectMessage("Only a single Pageable or Sort param is allowed.");
		String sql = "SELECT * FROM table;";

		Parameters parameters = mock(Parameters.class);
		// @formatter:off
		Mockito.<Parameters>when(this.queryMethod.getParameters())
				.thenReturn(parameters);
		// @formatter:on
		when(parameters.getNumberOfParameters()).thenReturn(0);

		SqlSpannerQuery sqlSpannerQuery = createQuery(sql, false);

		sqlSpannerQuery.execute(new Object[] { this.sort, this.sort });
	}

	@Test
	public void dmlTest() {
		String sql = "dml statement here";

		Parameters parameters = mock(Parameters.class);
		// @formatter:off
		Mockito.<Parameters>when(this.queryMethod.getParameters())
				.thenReturn(parameters);
		// @formatter:on
		when(parameters.getNumberOfParameters()).thenReturn(0);

		SqlSpannerQuery sqlSpannerQuery = spy(createQuery(sql, true));

		doReturn(long.class).when(sqlSpannerQuery)
				.getReturnedSimpleConvertableItemType();
		doReturn(null).when(sqlSpannerQuery).convertToSimpleReturnType(any(),
				any());

		sqlSpannerQuery.execute(new Object[] {});

		verify(this.spannerTemplate, times(1)).executeDmlStatement(any());
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
