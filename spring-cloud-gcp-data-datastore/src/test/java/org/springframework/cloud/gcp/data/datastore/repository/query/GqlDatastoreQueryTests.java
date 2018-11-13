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

package org.springframework.cloud.gcp.data.datastore.repository.query;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.cloud.datastore.DoubleValue;
import com.google.cloud.datastore.GqlQuery;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Field;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
public class GqlDatastoreQueryTests {

	private DatastoreTemplate datastoreTemplate;

	private DatastoreQueryMethod queryMethod;

	private QueryMethodEvaluationContextProvider evaluationContextProvider;

	@Before
	public void initMocks() {
		this.queryMethod = mock(DatastoreQueryMethod.class);
		this.datastoreTemplate = mock(DatastoreTemplate.class);
		this.evaluationContextProvider = mock(QueryMethodEvaluationContextProvider.class);
	}

	private GqlDatastoreQuery<Trade> createQuery(String gql) {
		return new GqlDatastoreQuery<>(Trade.class, this.queryMethod,
				this.datastoreTemplate, gql, this.evaluationContextProvider, new DatastoreMappingContext());
	}

	@Test
	public void compoundNameConventionTest() {

		String gql = "SELECT * FROM "
				+ "|org.springframework.cloud.gcp.data.datastore."
				+ "repository.query.GqlDatastoreQueryTests$Trade|"
				+ " WHERE price=:#{#tag6 * -1} AND price<>:#{#tag6 * -1} OR "
				+ "price<>:#{#tag7 * -1} AND " + "( action=@tag0 AND ticker=@tag1 ) OR "
				+ "( trader_id=@tag2 AND price<@tag3 ) OR ( price>=@tag4 AND id<>NULL AND "
				+ "trader_id=NULL AND trader_id LIKE %@tag5 AND price=TRUE AND price=FALSE AND "
				+ "price>@tag6 AND price<=@tag7 )ORDER BY id DESC LIMIT 3;";

		String entityResolvedGql = "SELECT * FROM trades"
				+ " WHERE price=@SpELtag1 AND price<>@SpELtag2 OR price<>@SpELtag3 AND "
				+ "( action=@tag0 AND ticker=@tag1 ) OR "
				+ "( trader_id=@tag2 AND price<@tag3 ) OR ( price>=@tag4 AND id<>NULL AND "
				+ "trader_id=NULL AND trader_id LIKE %@tag5 AND price=TRUE AND price=FALSE AND "
				+ "price>@tag6 AND price<=@tag7 )ORDER BY id DESC LIMIT 3";

		Object[] params = new Object[] { "BUY", "abcd",
				new String[] { "abc123", "abc321" }, new double[] { 8.88, 9.99 }, 3.33,
				"blahblah", 1.11, 2.22 };

		String[] paramNames = new String[] { "tag0", "tag1", "tag2", "tag3", "tag4",
				"tag5", "tag6", "tag7" };

		Parameters parameters = mock(Parameters.class);

		Mockito.<Parameters>when(this.queryMethod.getParameters())
				.thenReturn(parameters);

		when(parameters.getNumberOfParameters()).thenReturn(paramNames.length);
		when(parameters.getParameter(anyInt())).thenAnswer(invocation -> {
			int index = invocation.getArgument(0);
			Parameter param = mock(Parameter.class);
			when(param.getName()).thenReturn(Optional.of(paramNames[index]));

			Mockito.<Class>when(param.getType()).thenReturn(params[index].getClass());

			return param;
		});

		EvaluationContext evaluationContext = new StandardEvaluationContext();
		for (int i = 0; i < params.length; i++) {
			evaluationContext.setVariable(paramNames[i], params[i]);
		}
		when(this.evaluationContextProvider.getEvaluationContext(any(), any()))
				.thenReturn(evaluationContext);

		GqlDatastoreQuery gqlDatastoreQuery = spy(createQuery(gql));

		doAnswer(invocation -> {
			GqlQuery statement = invocation.getArgument(0);

			assertEquals(entityResolvedGql, statement.getQueryString());

			Map<String, Value> paramMap = statement.getNamedBindings();

			assertEquals(params[0], paramMap.get("tag0").get());
			assertEquals(params[1], paramMap.get("tag1").get());
			assertEquals(((String[]) params[2])[0],
					((StringValue) (((List) paramMap.get("tag2").get()).get(0))).get());
			assertEquals(((String[]) params[2])[1],
					((StringValue) (((List) paramMap.get("tag2").get()).get(1))).get());
			assertEquals(((double[]) params[3])[0],
					((DoubleValue) (((List) paramMap.get("tag3").get()).get(0))).get(),
					0.00001);
			assertEquals(((double[]) params[3])[1],
					((DoubleValue) (((List) paramMap.get("tag3").get()).get(1))).get(),
					0.00001);
			assertEquals(params[4], paramMap.get("tag4").get());
			assertEquals(params[5], paramMap.get("tag5").get());
			assertEquals(params[6], paramMap.get("tag6").get());
			assertEquals(params[7], paramMap.get("tag7").get());
			assertEquals(-1 * (double) params[6], (double) paramMap.get("SpELtag1").get(),
					0.00001);
			assertEquals(-1 * (double) params[6], (double) paramMap.get("SpELtag2").get(),
					0.00001);
			assertEquals(-1 * (double) params[7], (double) paramMap.get("SpELtag3").get(),
					0.00001);

			return null;
		}).when(this.datastoreTemplate).queryKeysOrEntities(any(), eq(Trade.class));

		doReturn(false).when(gqlDatastoreQuery).isNonEntityReturnedType(any());

		gqlDatastoreQuery.execute(params);

		verify(this.datastoreTemplate, times(1))
				.queryKeysOrEntities(any(), eq(Trade.class));
	}

	@Entity(name = "trades")
	private static class Trade {
		@Id
		String id;

		String action;

		Double price;

		Double shares;

		@Field(name = "ticker")
		String symbol;

		@Field(name = "trader_id")
		String traderId;
	}
}
