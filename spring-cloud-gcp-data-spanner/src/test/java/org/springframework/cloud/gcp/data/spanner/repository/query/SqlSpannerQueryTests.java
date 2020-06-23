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
import java.util.List;
import java.util.Map;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionRunner;
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
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerWriteConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Interleaved;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Where;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.repository.query.DefaultParameters;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

	private final DatabaseClient databaseClient = mock(DatabaseClient.class);

	/**
	 * checks messages and types for exceptions.
	 */
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Before
	public void initMocks() throws NoSuchMethodException {
		this.queryMethod = mock(SpannerQueryMethod.class);
		// this is a dummy object. it is not mockable otherwise.
		Method method = Object.class.getMethod("toString");
		when(this.queryMethod.getMethod()).thenReturn(method);
		when(this.spannerEntityProcessor.getWriteConverter()).thenReturn(new SpannerWriteConverter());
		this.spannerTemplate = spy(new SpannerTemplate(() -> this.databaseClient,
				this.spannerMappingContext, this.spannerEntityProcessor,
				mock(SpannerMutationFactory.class), new SpannerSchemaUtils(
						this.spannerMappingContext, this.spannerEntityProcessor, true)));
		this.expressionParser = new SpelExpressionParser();
		this.evaluationContextProvider = mock(QueryMethodEvaluationContextProvider.class);
	}

	private <T> SqlSpannerQuery<T> createQuery(String sql, Class<T> theClass,  boolean isDml) {
		return new SqlSpannerQuery<T>(theClass, this.queryMethod,
				this.spannerTemplate,
				sql, this.evaluationContextProvider, this.expressionParser,
				new SpannerMappingContext(), isDml);
	}

	@Test
	public void noPageableParamQueryTest() throws NoSuchMethodException {
		String sql = "SELECT DISTINCT * FROM "
				+ ":org.springframework.cloud.gcp.data.spanner.repository.query.SqlSpannerQueryTests$Trade:";
		// @formatter:off
		String entityResolvedSql = "SELECT *, " +
				"ARRAY (SELECT AS STRUCT disabled, id, childId, value, " +
					"ARRAY (SELECT AS STRUCT canceled, documentId, id, childId, content " +
						"FROM documents WHERE (documents.id = children.id AND documents.childId = children.childId) " +
							"AND (canceled = false)) AS documents " +
					"FROM children WHERE (children.id = trades.id) AND (disabled = false)) AS children " +
				"FROM (SELECT DISTINCT * FROM trades) trades";
		// @formatter:on

		final Class toReturn = Trade.class;
		when(queryMethod.isCollectionQuery()).thenReturn(false);
		when(queryMethod.getReturnedObjectType()).thenReturn(toReturn);

		EvaluationContext evaluationContext = new StandardEvaluationContext();
		when(this.evaluationContextProvider.getEvaluationContext(any(), any()))
				.thenReturn(evaluationContext);

		SqlSpannerQuery sqlSpannerQuery = createQuery(sql, toReturn, false);

		doAnswer((invocation) -> {
			Statement statement = invocation.getArgument(0);
			SpannerQueryOptions queryOptions = invocation.getArgument(1);
			assertThat(queryOptions.isAllowPartialRead()).isTrue();

			assertThat(statement.getSql()).isEqualTo(entityResolvedSql);

			return null;
		}).when(this.spannerTemplate).executeQuery(any(), any());

		// This dummy method was created so the metadata for the ARRAY param inner type is
		// provided.
		Method method = QueryHolder.class.getMethod("dummyMethod2");
		when(this.queryMethod.getMethod()).thenReturn(method);
		Mockito.<Parameters>when(this.queryMethod.getParameters()).thenReturn(new DefaultParameters(method));

		sqlSpannerQuery.execute(new Object[] {});

		verify(this.spannerTemplate, times(1)).executeQuery(any(), any());
	}

	@Test
	public void pageableParamQueryTest() throws NoSuchMethodException {

		String sql = "SELECT * FROM :org.springframework.cloud.gcp.data.spanner.repository.query.SqlSpannerQueryTests$Child:"
				+ " WHERE id = @id AND trader_id = @trader_id";
		// @formatter:off
		String entityResolvedSql = "SELECT *, " +
				"ARRAY (SELECT AS STRUCT canceled, documentId, id, childId, content " +
					"FROM documents WHERE (documents.id = children.id AND documents.childId = children.childId) " +
						"AND (canceled = false)) AS documents " +
				"FROM (SELECT * FROM children WHERE id = @id AND trader_id = @trader_id) children " +
				"WHERE disabled = false ORDER BY trader_id ASC LIMIT 10 OFFSET 30";
		// @formatter:on

		Object[] params = new Object[] { "ID", "TRADER_ID", PageRequest.of(3, 10, Sort.by(Order.asc("trader_id")))};
		String[] paramNames = new String[] { "id", "trader_id", "ignoredPageable" };

		when(queryMethod.isCollectionQuery()).thenReturn(false);
		when(queryMethod.getReturnedObjectType()).thenReturn((Class) Child.class);

		EvaluationContext evaluationContext = new StandardEvaluationContext();
		for (int i = 0; i < params.length; i++) {
			evaluationContext.setVariable(paramNames[i], params[i]);
		}
		when(this.evaluationContextProvider.getEvaluationContext(any(), any()))
				.thenReturn(evaluationContext);

		SqlSpannerQuery sqlSpannerQuery = createQuery(sql, Child.class, false);

		doAnswer((invocation) -> {
			Statement statement = invocation.getArgument(0);
			SpannerQueryOptions queryOptions = invocation.getArgument(1);
			assertThat(queryOptions.isAllowPartialRead()).isTrue();

			assertThat(statement.getSql()).isEqualTo(entityResolvedSql);

			Map<String, Value> paramMap = statement.getParameters();

			assertThat(paramMap.get("id").getString()).isEqualTo(params[0]);
			assertThat(paramMap.get("trader_id").getString()).isEqualTo(params[1]);
			assertThat(paramMap.get("ignoredPageable")).isNull();

			return null;
		}).when(this.spannerTemplate).executeQuery(any(), any());

		// This dummy method was created so the metadata for the ARRAY param inner type is
		// provided.
		Method method = QueryHolder.class.getMethod("dummyMethod4", String.class, String.class, Pageable.class);
		when(this.queryMethod.getMethod()).thenReturn(method);
		Mockito.<Parameters>when(this.queryMethod.getParameters()).thenReturn(new DefaultParameters(method));

		sqlSpannerQuery.execute(params);

		verify(this.spannerTemplate, times(1)).executeQuery(any(), any());
	}

	@Test
	public void sortParamQueryTest() throws NoSuchMethodException {

		String sql = "SELECT * FROM :org.springframework.cloud.gcp.data.spanner.repository.query.SqlSpannerQueryTests$Child:"
				+ " WHERE id = @id AND trader_id = @trader_id";
		// @formatter:off
		String entityResolvedSql = "SELECT *, " +
				"ARRAY (SELECT AS STRUCT canceled, documentId, id, childId, content " +
					"FROM documents WHERE (documents.id = children.id AND documents.childId = children.childId) " +
						"AND (canceled = false)) AS documents " +
				"FROM (SELECT * FROM children WHERE id = @id AND trader_id = @trader_id) children " +
				"WHERE disabled = false ORDER BY trader_id ASC";
		// @formatter:on

		Object[] params = new Object[] { "ID", "TRADER_ID", Sort.by(Order.asc("trader_id"))};
		String[] paramNames = new String[] { "id", "trader_id", "ignoredSort" };

		when(queryMethod.isCollectionQuery()).thenReturn(false);
		when(queryMethod.getReturnedObjectType()).thenReturn((Class) Child.class);

		EvaluationContext evaluationContext = new StandardEvaluationContext();
		for (int i = 0; i < params.length; i++) {
			evaluationContext.setVariable(paramNames[i], params[i]);
		}
		when(this.evaluationContextProvider.getEvaluationContext(any(), any()))
				.thenReturn(evaluationContext);

		SqlSpannerQuery sqlSpannerQuery = createQuery(sql, Child.class, false);

		doAnswer((invocation) -> {
			Statement statement = invocation.getArgument(0);
			SpannerQueryOptions queryOptions = invocation.getArgument(1);
			assertThat(queryOptions.isAllowPartialRead()).isTrue();

			assertThat(statement.getSql()).isEqualTo(entityResolvedSql);

			Map<String, Value> paramMap = statement.getParameters();

			assertThat(paramMap.get("id").getString()).isEqualTo(params[0]);
			assertThat(paramMap.get("trader_id").getString()).isEqualTo(params[1]);
			assertThat(paramMap.get("ignoredSort")).isNull();

			return null;
		}).when(this.spannerTemplate).executeQuery(any(), any());

		// This dummy method was created so the metadata for the ARRAY param inner type is
		// provided.
		Method method = QueryHolder.class.getMethod("dummyMethod5", String.class, String.class, Sort.class);
		when(this.queryMethod.getMethod()).thenReturn(method);
		Mockito.<Parameters>when(this.queryMethod.getParameters()).thenReturn(new DefaultParameters(method));

		sqlSpannerQuery.execute(params);

		verify(this.spannerTemplate, times(1)).executeQuery(any(), any());
	}


	@Test
	public void sortAndPageableQueryTest() throws NoSuchMethodException {

		String sql = "SELECT * FROM :org.springframework.cloud.gcp.data.spanner.repository.query.SqlSpannerQueryTests$Child:"
						+ " WHERE id = @id AND trader_id = @trader_id";
		// @formatter:off
		String entityResolvedSql = "SELECT *, " +
						"ARRAY (SELECT AS STRUCT canceled, documentId, id, childId, content " +
						"FROM documents WHERE (documents.id = children.id AND documents.childId = children.childId) " +
						"AND (canceled = false)) AS documents " +
						"FROM (SELECT * FROM children WHERE id = @id AND trader_id = @trader_id) children " +
						"WHERE disabled = false ORDER BY trader_id ASC LIMIT 2 OFFSET 2";
		// @formatter:on

		Object[] params = new Object[] { "ID", "TRADER_ID", Sort.by(Order.asc("trader_id")), PageRequest.of(1, 2)};
		String[] paramNames = new String[] { "id", "trader_id", "ignoredSort", "pageable" };

		when(queryMethod.isCollectionQuery()).thenReturn(false);
		when(queryMethod.getReturnedObjectType()).thenReturn((Class) Child.class);

		EvaluationContext evaluationContext = new StandardEvaluationContext();
		for (int i = 0; i < params.length; i++) {
			evaluationContext.setVariable(paramNames[i], params[i]);
		}
		when(this.evaluationContextProvider.getEvaluationContext(any(), any()))
						.thenReturn(evaluationContext);

		SqlSpannerQuery sqlSpannerQuery = createQuery(sql, Child.class, false);

		doAnswer((invocation) -> {
			Statement statement = invocation.getArgument(0);
			SpannerQueryOptions queryOptions = invocation.getArgument(1);
			assertThat(queryOptions.isAllowPartialRead()).isTrue();

			assertThat(statement.getSql()).isEqualTo(entityResolvedSql);

			Map<String, Value> paramMap = statement.getParameters();

			assertThat(paramMap.get("id").getString()).isEqualTo(params[0]);
			assertThat(paramMap.get("trader_id").getString()).isEqualTo(params[1]);
			assertThat(paramMap.get("ignoredSort")).isNull();
			assertThat(paramMap.get("pageable")).isNull();

			return null;
		}).when(this.spannerTemplate).executeQuery(any(), any());


		Method method = QueryHolder.class.getMethod("sortAndPageable", String.class, String.class, Sort.class, Pageable.class);
		when(this.queryMethod.getMethod()).thenReturn(method);
		Mockito.<Parameters>when(this.queryMethod.getParameters()).thenReturn(new DefaultParameters(method));

		sqlSpannerQuery.execute(params);

		verify(this.spannerTemplate, times(1)).executeQuery(any(), any());
	}

	@Test
	public void compoundNameConventionTest() throws NoSuchMethodException {

		String sql = "SELECT DISTINCT * FROM "
				+ ":org.springframework.cloud.gcp.data.spanner.repository.query.SqlSpannerQueryTests$Trade:"
				+ "@{index=fakeindex}"
				+ " WHERE price=#{#tag3 * -1} AND price<>#{#tag3 * -1} OR "
				+ "price<>#{#tag4 * -1} AND " + "( action=@tag0 AND ticker=@tag1 ) OR "
				+ "( trader_id=@tag2 AND price<@tag3 ) OR ( price>=@tag4 AND id<>NULL AND "
				+ "trader_id=NULL AND trader_id LIKE %@tag5 AND price=TRUE AND price=FALSE AND "
				+ "struct_val = @tag8 AND struct_val = @tag9 "
				+ "price>@tag6 AND price<=@tag7 and price in unnest(@tag10)) ORDER BY id DESC LIMIT 3;";

		// @formatter:off
		String entityResolvedSql = "SELECT *, " +
				"ARRAY (SELECT AS STRUCT disabled, id, childId, value, " +
					"ARRAY (SELECT AS STRUCT canceled, documentId, id, childId, content " +
						"FROM documents WHERE (documents.id = children.id AND documents.childId = children.childId) " +
							"AND (canceled = false)) AS documents " +
					"FROM children WHERE (children.id = trades.id) AND (disabled = false)) AS children FROM "
				+ "(SELECT DISTINCT * FROM trades@{index=fakeindex}"
				+ " WHERE price=@SpELtag1 AND price<>@SpELtag1 OR price<>@SpELtag2 AND "
				+ "( action=@tag0 AND ticker=@tag1 ) OR "
				+ "( trader_id=@tag2 AND price<@tag3 ) OR ( price>=@tag4 AND id<>NULL AND "
				+ "trader_id=NULL AND trader_id LIKE %@tag5 AND price=TRUE AND price=FALSE AND "
				+ "struct_val = @tag8 AND struct_val = @tag9 "
				+ "price>@tag6 AND price<=@tag7 and price in unnest(@tag10)) ORDER BY id DESC LIMIT 3) trades "
				+ "ORDER BY COLA ASC , COLB DESC LIMIT 10 OFFSET 30";
		// @formatter:on

		Object[] params = new Object[] { "BUY", this.pageable, "abcd", "abc123", 8.88,
				3.33, "blahblah",
				1.11, 2.22, Struct.newBuilder().set("symbol").to("ABCD").set("action")
						.to("BUY").build(),
				new SymbolAction("ABCD", "BUY"), Arrays.asList("a", "b") };

		String[] paramNames = new String[] { "tag0", "ignoredPageable", "tag1", "tag2",
				"tag3", "tag4",
				"tag5", "tag6", "tag7", "tag8", "tag9", "tag10" };

		when(queryMethod.isCollectionQuery()).thenReturn(false);
		when(queryMethod.getReturnedObjectType()).thenReturn((Class) Trade.class);

		EvaluationContext evaluationContext = new StandardEvaluationContext();
		for (int i = 0; i < params.length; i++) {
			evaluationContext.setVariable(paramNames[i], params[i]);
		}
		when(this.evaluationContextProvider.getEvaluationContext(any(), any()))
				.thenReturn(evaluationContext);

		SqlSpannerQuery sqlSpannerQuery = createQuery(sql, Trade.class, false);

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
			assertThat(paramMap.get("tag10").getStringArray()).isEqualTo(params[11]);
			verify(this.spannerEntityProcessor, times(1)).write(same(params[10]), any());

			assertThat(paramMap.get("SpELtag1").getFloat64()).isEqualTo(-8.88, DELTA);
			assertThat(paramMap.get("SpELtag2").getFloat64()).isEqualTo(-3.33, DELTA);

			return null;
		}).when(this.spannerTemplate).executeQuery(any(), any());

		// This dummy method was created so the metadata for the ARRAY param inner type is
		// provided.
		Method method = QueryHolder.class.getMethod("dummyMethod", Object.class, Pageable.class, Object.class,
				Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class,
				Object.class, List.class);
		when(this.queryMethod.getMethod()).thenReturn(method);
		Mockito.<Parameters>when(this.queryMethod.getParameters()).thenReturn(new DefaultParameters(method));
		sqlSpannerQuery.execute(params);

		verify(this.spannerTemplate, times(1)).executeQuery(any(), any());
	}

	@Test
	public void dmlTest() throws NoSuchMethodException {
		String sql = "dml statement here";

		TransactionContext context = mock(TransactionContext.class);
		TransactionRunner transactionRunner = mock(TransactionRunner.class);
		when(this.databaseClient.readWriteTransaction()).thenReturn(transactionRunner);

		when(transactionRunner.run(any())).thenAnswer((invocation) -> {
			TransactionRunner.TransactionCallable transactionCallable = invocation.getArgument(0);
			return transactionCallable.run(context);
		});

		Method method = QueryHolder.class.getMethod("noParamMethod");

		Mockito.<Parameters>when(this.queryMethod.getParameters())
				.thenReturn(new DefaultParameters(method));


		SqlSpannerQuery sqlSpannerQuery = spy(createQuery(sql, Trade.class, true));

		doReturn(long.class).when(sqlSpannerQuery)
				.getReturnedSimpleConvertableItemType();
		doReturn(null).when(sqlSpannerQuery).convertToSimpleReturnType(any(),
				any());

		sqlSpannerQuery.execute(new Object[] {});

		verify(this.spannerTemplate, times(1)).executeDmlStatement(any());
	}

	@Test
	public void sqlCountWithWhereTest() throws NoSuchMethodException {
		String sql = "SELECT count(1) FROM :org.springframework.cloud.gcp.data.spanner.repository.query.SqlSpannerQueryTests$Child:"
				+ " WHERE id = @id AND trader_id = @trader_id";

		String entityResolvedSql = "SELECT count(1) FROM children WHERE id = @id AND trader_id = @trader_id";

		Object[] params = new Object[] { "ID", "TRADER_ID" };
		String[] paramNames = new String[] { "id", "trader_id" };

		when(queryMethod.isCollectionQuery()).thenReturn(false);
		when(queryMethod.getReturnedObjectType()).thenReturn((Class) long.class);

		EvaluationContext evaluationContext = new StandardEvaluationContext();
		for (int i = 0; i < params.length; i++) {
			evaluationContext.setVariable(paramNames[i], params[i]);
		}
		when(this.evaluationContextProvider.getEvaluationContext(any(), any()))
				.thenReturn(evaluationContext);

		SqlSpannerQuery sqlSpannerQuery = createQuery(sql, long.class, false);

		doAnswer((invocation) -> {
			Statement statement = invocation.getArgument(0);
			SpannerQueryOptions queryOptions = invocation.getArgument(1);
			assertThat(queryOptions.isAllowPartialRead()).isTrue();

			assertThat(statement.getSql()).isEqualTo(entityResolvedSql);

			Map<String, Value> paramMap = statement.getParameters();

			assertThat(paramMap.get("id").getString()).isEqualTo(params[0]);
			assertThat(paramMap.get("trader_id").getString()).isEqualTo(params[1]);

			return null;
		}).when(this.spannerTemplate).executeQuery(any(), any());

		// This dummy method was created so the metadata for the ARRAY param inner type is
		// provided.
		Method method = QueryHolder.class.getMethod("dummyMethod3", String.class, String.class);
		when(this.queryMethod.getMethod()).thenReturn(method);
		Mockito.<Parameters>when(this.queryMethod.getParameters()).thenReturn(new DefaultParameters(method));

		sqlSpannerQuery.execute(params);

		verify(this.spannerTemplate, times(1)).executeQuery(any(), any());
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

		@Interleaved
		List<Child> children;
	}


	@Table(name = "children")
	@Where("disabled = false")
	private static class Child {
		@PrimaryKey(keyOrder = 1)
		String id;

		@PrimaryKey(keyOrder = 2)
		String childId;

		String value;

		boolean disabled;

		@Interleaved
		List<Document> documents;
	}

	@Table(name = "documents")
	@Where("canceled = false")
	private static class Document {
		@PrimaryKey(keyOrder = 1)
		String id;

		@PrimaryKey(keyOrder = 2)
		String childId;

		@PrimaryKey(keyOrder = 3)
		String documentId;

		String content;

		boolean canceled;
	}

	private static class QueryHolder {
		public long dummyMethod(Object tag0, Pageable pageable, Object tag1, Object tag2, Object tag3, Object tag4,
				Object tag5, Object tag6, Object tag7, Object tag8, Object tag9,
				@Param("tag10") List<String> blahblah) {
			// tag10 is intentionally named via annotation.
			return 0;
		}

		public long dummyMethod2() {
			return 0;
		}

		public long dummyMethod3(String id, String trader_id) {
			return 0;
		}

		public List<Child> dummyMethod4(String id, String trader_id, Pageable param3) {
			return null;
		}

		public List<Child> dummyMethod5(String id, String trader_id, Sort param3) {
			return null;
		}

		public List<Child> pageableAndSort(Pageable param1, Sort param2) {
			return null;
		}

		public List<Child> doublePageable(Pageable param1, Pageable param2) {
			return null;
		}

		public void noParamMethod() {

		}

		public List<Child> sortAndPageable(String id, String trader_id, Sort sort, Pageable pageable) {
			return null;
		}
	}
}
