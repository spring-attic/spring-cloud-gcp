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

import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Value;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
public class SqlSpannerQueryTests {

	private SpannerOperations spannerOperations;

	private QueryMethod queryMethod;

	private EvaluationContextProvider evaluationContextProvider;

	private SpelExpressionParser expressionParser;

	@Before
	public void initMocks() {
		this.queryMethod = mock(QueryMethod.class);
		this.spannerOperations = mock(SpannerOperations.class);
		this.expressionParser = new SpelExpressionParser();
	}

	private SqlSpannerQuery createQuery(String sql) {
		return new SqlSpannerQuery(Trade.class, this.queryMethod, this.spannerOperations,
				sql, this.evaluationContextProvider, this.expressionParser);
	}

	@Test
	public void filterOutSpelParamsTest() {
		Object[] params = new Object[] { "a", "b", "c", "d" };
		Optional<String>[] spelParamNames = new Optional[] { Optional.of("something"),
				Optional.empty(), Optional.of("something"), Optional.empty() };

		String sql = "SELECT DISTINCT * FROM trades WHERE action=@tag0 AND ticker=@tag1;";

		SqlSpannerQuery sqlSpannerQuery = spy(createQuery(sql));
		doReturn(sql).when(sqlSpannerQuery).getSql(eq(params));

		Parameters mockParameters = mock(Parameters.class);
		when(this.queryMethod.getParameters()).thenReturn(mockParameters);
		when(mockParameters.getNumberOfParameters()).thenReturn(params.length);

		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return spelParamNames[(int) invocation.getArgument(1)];
			}
		}).when(sqlSpannerQuery).getSpelParamName(same(mockParameters), anyInt());

		when(this.spannerOperations.find(any(), (Statement) any(), any()))
				.thenAnswer(invocation -> {
					Statement statement = invocation.getArgument(1);

					assertEquals(sql, statement.getSql());

					Map<String, Value> paramMap = statement.getParameters();

					assertEquals(params[1], paramMap.get("tag0").getString());
					assertEquals(params[3], paramMap.get("tag1").getString());

					return null;
				});
		sqlSpannerQuery.execute(params);
	}

	@Test
	public void compoundNameConventionTest() {

		String sql = "SELECT DISTINCT * FROM trades WHERE ( action=@tag0 AND ticker=@tag1 ) OR "
				+ "( trader_id=@tag2 AND price<@tag3 ) OR ( price>=@tag4 AND id<>NULL AND "
				+ "trader_id=NULL AND trader_id LIKE %@tag5 AND price=TRUE AND price=FALSE AND "
				+ "price>@tag6 AND price<=@tag7 )ORDER BY id DESC LIMIT 3;";

		Object[] params = new Object[] { "BUY", "abcd", "abc123", 8.88, 3.33, "blahblah",
				1.11, 2.22, };

		SqlSpannerQuery sqlSpannerQuery = spy(createQuery(sql));
		doReturn(sql).when(sqlSpannerQuery).getSql(eq(params));
		doReturn(params).when(sqlSpannerQuery).filterOutSpelParameters(eq(params));

		when(this.spannerOperations.find(any(), (Statement) any(), any()))
				.thenAnswer(invocation -> {
					Statement statement = invocation.getArgument(1);

					assertEquals(sql, statement.getSql());

					Map<String, Value> paramMap = statement.getParameters();

					assertEquals(params[0], paramMap.get("tag0").getString());
					assertEquals(params[1], paramMap.get("tag1").getString());
					assertEquals(params[2], paramMap.get("tag2").getString());
					assertEquals(params[3], paramMap.get("tag3").getFloat64());
					assertEquals(params[4], paramMap.get("tag4").getFloat64());
					assertEquals(params[5], paramMap.get("tag5").getString());
					assertEquals(params[6], paramMap.get("tag6").getFloat64());
					assertEquals(params[7], paramMap.get("tag7").getFloat64());

					return null;
				});

		sqlSpannerQuery.execute(params);
	}

	@Table(name = "trades")
	private static class Trade {
		@Id
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
