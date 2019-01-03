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

package org.springframework.cloud.gcp.data.datastore.repository.query;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.KeyQuery;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreCustomConversions;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreEntityConverter;
import org.springframework.cloud.gcp.data.datastore.core.convert.ReadWriteConversions;
import org.springframework.cloud.gcp.data.datastore.core.convert.TwoStepsConversions;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Field;
import org.springframework.cloud.gcp.data.datastore.it.TestEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.DefaultParameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for Part-Tree Datastore Query Methods.
 *
 * @author Chengyuan Zhao
 * @author Dmitry Solomakha
 */
public class PartTreeDatastoreQueryTests {

	private static final Object[] EMPTY_PARAMETERS = new Object[0];

	private DatastoreTemplate datastoreTemplate;

	private DatastoreQueryMethod queryMethod;

	private DatastoreMappingContext datastoreMappingContext;

	private PartTreeDatastoreQuery partTreeDatastoreQuery;

	private DatastoreEntityConverter datastoreEntityConverter;

	private ReadWriteConversions readWriteConversions;

	/**
	 * used to check exception messages and types.
	 */
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void initMocks() {
		this.queryMethod = mock(DatastoreQueryMethod.class);
		when(this.queryMethod.getReturnedObjectType()).thenReturn((Class) TestEntity.class);
		this.datastoreTemplate = mock(DatastoreTemplate.class);
		this.datastoreMappingContext = new DatastoreMappingContext();
		this.datastoreEntityConverter = mock(DatastoreEntityConverter.class);
		this.readWriteConversions = new TwoStepsConversions(new DatastoreCustomConversions(), null);
		when(this.datastoreTemplate.getDatastoreEntityConverter())
				.thenReturn(this.datastoreEntityConverter);
		when(this.datastoreEntityConverter.getConversions())
				.thenReturn(this.readWriteConversions);

	}

	private PartTreeDatastoreQuery<Trade> createQuery(boolean isPageQuery, boolean isSliceQuery) {
		PartTreeDatastoreQuery<Trade> tradePartTreeDatastoreQuery = new PartTreeDatastoreQuery<>(this.queryMethod,
				this.datastoreTemplate,
				this.datastoreMappingContext, Trade.class);
		PartTreeDatastoreQuery<Trade> spy = spy(tradePartTreeDatastoreQuery);
		doReturn(isPageQuery).when(spy).isPageQuery();
		doReturn(isSliceQuery).when(spy).isSliceQuery();
		doAnswer((invocation) -> invocation.getArguments()[0]).when(spy).processRawObjectForProjection(any());
		doAnswer((invocation) -> invocation.getArguments()[0]).when(spy).convertResultCollection(any(), any());

		return spy;
	}

	@Test
	public void compoundNameConventionTest() throws NoSuchMethodException {
		queryWithMockResult("findTop333ByActionAndSymbolAndPriceLessThanAndPriceGreater"
						+ "ThanEqualAndIdIsNullOrderByIdDesc", null,
				getClass().getMethod("tradeMethod", String.class, String.class, double.class, double.class));

		Object[] params = new Object[] { "BUY", "abcd",
				// this int param requires custom conversion
				8, 3.33 };

		when(this.datastoreTemplate.queryKeysOrEntities(any(), any())).thenAnswer((invocation) -> {
			EntityQuery statement = invocation.getArgument(0);

			EntityQuery expected = StructuredQuery.newEntityQueryBuilder()
					.setFilter(CompositeFilter.and(PropertyFilter.eq("action", "BUY"),
							PropertyFilter.eq("ticker", "abcd"),
							PropertyFilter.lt("price", 8L),
							PropertyFilter.ge("price", 3.33),
							PropertyFilter.isNull("id")))
					.setKind("trades")
					.setOrderBy(OrderBy.desc("id")).setLimit(333).build();

			assertThat(statement).isEqualTo(expected);

			return Collections.emptyList();
		});

		when(this.queryMethod.getCollectionReturnType()).thenReturn(List.class);

		this.partTreeDatastoreQuery.execute(params);
		verify(this.datastoreTemplate, times(1))
				.queryKeysOrEntities(any(), any());
	}

	@Test
	public void ambiguousSortPageableParam() throws NoSuchMethodException {
		queryWithMockResult("findTop333ByActionAndSymbolAndPriceLessThanAndPriceGreater"
						+ "ThanEqualAndIdIsNullOrderByIdDesc", null,
				getClass().getMethod("tradeMethod", String.class, String.class, double.class, double.class,
						Pageable.class));

		Object[] params = new Object[] { "BUY", "abcd", 8.88, 3.33, PageRequest.of(1, 444, Sort.Direction.ASC, "price") };

		when(this.datastoreTemplate.queryKeysOrEntities(any(), any())).thenAnswer((invocation) -> {
			EntityQuery statement = invocation.getArgument(0);

			EntityQuery expected = StructuredQuery.newEntityQueryBuilder()
					.setFilter(CompositeFilter.and(PropertyFilter.eq("action", "BUY"),
							PropertyFilter.eq("ticker", "abcd"),
							PropertyFilter.lt("price", 8.88),
							PropertyFilter.ge("price", 3.33),
							PropertyFilter.isNull("id")))
					.setKind("trades")
					.setOffset(444)
					.setLimit(444)
					.setOrderBy(OrderBy.desc("id"), OrderBy.asc("price")).build();

			assertThat(statement).isEqualTo(expected);

			return Collections.emptyList();
		});

		when(this.queryMethod.getCollectionReturnType()).thenReturn(List.class);

		this.partTreeDatastoreQuery.execute(params);
		verify(this.datastoreTemplate, times(1))
				.queryKeysOrEntities(any(), any());
	}

	@Test
	public void nullPageable() throws NoSuchMethodException {
		queryWithMockResult("findTop333ByActionAndSymbolAndPriceLessThanAndPriceGreater"
						+ "ThanEqualAndIdIsNullOrderByIdDesc", null,
				getClass().getMethod("tradeMethod", String.class, String.class, double.class, double.class,
						Pageable.class));

		Object[] params = new Object[] { "BUY", "abcd", 8.88, 3.33, null};

		when(this.datastoreTemplate.queryKeysOrEntities(any(), any())).thenAnswer((invocation) -> {
			EntityQuery statement = invocation.getArgument(0);

			EntityQuery expected = StructuredQuery.newEntityQueryBuilder()
					.setFilter(CompositeFilter.and(PropertyFilter.eq("action", "BUY"),
							PropertyFilter.eq("ticker", "abcd"),
							PropertyFilter.lt("price", 8.88),
							PropertyFilter.ge("price", 3.33),
							PropertyFilter.isNull("id")))
					.setKind("trades")
					.setLimit(333)
					.setOrderBy(OrderBy.desc("id")).build();

			assertThat(statement).isEqualTo(expected);

			return Collections.emptyList();
		});

		when(this.queryMethod.getCollectionReturnType()).thenReturn(List.class);

		this.partTreeDatastoreQuery.execute(params);
		verify(this.datastoreTemplate, times(1))
				.queryKeysOrEntities(any(), any());
	}

	@Test
	public void ambiguousSort() throws NoSuchMethodException {
		queryWithMockResult("findByActionAndSymbolAndPriceLessThanAndPriceGreater"
				+ "ThanEqualAndIdIsNullOrderByIdDesc", null,
				getClass().getMethod("tradeMethod", String.class, String.class, double.class, double.class,
						Sort.class));

		Object[] params = new Object[] { "BUY", "abcd", 8.88, 3.33, Sort.by(Sort.Direction.ASC, "price") };

		when(this.datastoreTemplate.queryKeysOrEntities(any(), any())).thenAnswer((invocation) -> {
			EntityQuery statement = invocation.getArgument(0);

			EntityQuery expected = StructuredQuery.newEntityQueryBuilder()
					.setFilter(CompositeFilter.and(PropertyFilter.eq("action", "BUY"),
							PropertyFilter.eq("ticker", "abcd"),
							PropertyFilter.lt("price", 8.88),
							PropertyFilter.ge("price", 3.33),
							PropertyFilter.isNull("id")))
					.setKind("trades")
					.setOrderBy(OrderBy.desc("id"), OrderBy.asc("price")).build();

			assertThat(statement).isEqualTo(expected);

			return Collections.emptyList();
		});

		when(this.queryMethod.getCollectionReturnType()).thenReturn(List.class);

		this.partTreeDatastoreQuery.execute(params);
		verify(this.datastoreTemplate, times(1))
				.queryKeysOrEntities(any(), any());
	}

	@Test
	public void nullSort() throws NoSuchMethodException {
		queryWithMockResult("findByActionAndSymbolAndPriceLessThanAndPriceGreater"
						+ "ThanEqualAndIdIsNullOrderByIdDesc", null,
				getClass().getMethod("tradeMethod", String.class, String.class, double.class, double.class,
						Sort.class));

		Object[] params = new Object[] { "BUY", "abcd", 8.88, 3.33, null };

		when(this.datastoreTemplate.queryKeysOrEntities(any(), any())).thenAnswer((invocation) -> {
			EntityQuery statement = invocation.getArgument(0);

			EntityQuery expected = StructuredQuery.newEntityQueryBuilder()
					.setFilter(CompositeFilter.and(PropertyFilter.eq("action", "BUY"),
							PropertyFilter.eq("ticker", "abcd"),
							PropertyFilter.lt("price", 8.88),
							PropertyFilter.ge("price", 3.33),
							PropertyFilter.isNull("id")))
					.setKind("trades")
					.setOrderBy(OrderBy.desc("id")).build();

			assertThat(statement).isEqualTo(expected);

			return Collections.emptyList();
		});

		when(this.queryMethod.getCollectionReturnType()).thenReturn(List.class);

		this.partTreeDatastoreQuery.execute(params);
		verify(this.datastoreTemplate, times(1))
				.queryKeysOrEntities(any(), any());
	}

	@Test
	public void caseInsensitiveSort() throws NoSuchMethodException {
		this.expectedException.expectMessage("Datastore doesn't support sorting ignoring case");
		queryWithMockResult("findByActionAndSymbolAndPriceLessThanAndPriceGreater"
						+ "ThanEqualAndIdIsNullOrderByIdDesc", null,
				getClass().getMethod("tradeMethod", String.class, String.class, double.class, double.class,
						Sort.class));

		Object[] params = new Object[] { "BUY", "abcd", 8.88, 3.33, Sort.by(Sort.Order.by("price").ignoreCase()) };

		this.partTreeDatastoreQuery.execute(params);
	}

	@Test
	public void caseNullHandlingSort() throws NoSuchMethodException {
		this.expectedException.expectMessage("Datastore supports only NullHandling.NATIVE null handling");
		queryWithMockResult("findByActionAndSymbolAndPriceLessThanAndPriceGreater"
						+ "ThanEqualAndIdIsNullOrderByIdDesc", null,
				getClass().getMethod("tradeMethod", String.class, String.class, double.class, double.class,
						Sort.class));

		Object[] params = new Object[] { "BUY", "abcd", 8.88, 3.33, Sort.by(Sort.Order.by("price").nullsFirst()) };

		this.partTreeDatastoreQuery.execute(params);
	}

	@Test
	public void pageableParam() throws NoSuchMethodException {
		queryWithMockResult("findByActionAndSymbolAndPriceLessThanAndPriceGreater"
				+ "ThanEqualAndIdIsNull", null,
				getClass().getMethod("tradeMethod", String.class, String.class, double.class, double.class,
						Pageable.class));

		Object[] params = new Object[] { "BUY", "abcd", 8.88, 3.33, PageRequest.of(1, 444, Sort.Direction.DESC, "id") };

		when(this.datastoreTemplate.queryKeysOrEntities(any(), any())).thenAnswer((invocation) -> {
			EntityQuery statement = invocation.getArgument(0);

			EntityQuery expected = StructuredQuery.newEntityQueryBuilder()
					.setFilter(CompositeFilter.and(PropertyFilter.eq("action", "BUY"),
							PropertyFilter.eq("ticker", "abcd"),
							PropertyFilter.lt("price", 8.88),
							PropertyFilter.ge("price", 3.33),
							PropertyFilter.isNull("id")))
					.setKind("trades")
					.setOffset(444)
					.setOrderBy(OrderBy.desc("id")).setLimit(444).build();

			assertThat(statement).isEqualTo(expected);

			return Collections.emptyList();
		});

		when(this.queryMethod.getCollectionReturnType()).thenReturn(List.class);

		this.partTreeDatastoreQuery.execute(params);
		verify(this.datastoreTemplate, times(1))
				.queryKeysOrEntities(any(), any());
	}

	@Test
	public void pageableQuery() throws NoSuchMethodException {
		queryWithMockResult("findByActionAndSymbolAndPriceLessThanAndPriceGreater"
						+ "ThanEqualAndIdIsNull", null,
				getClass().getMethod("tradeMethod", String.class, String.class, double.class, double.class,
						Pageable.class));

		this.partTreeDatastoreQuery = createQuery(true, false);

		Object[] params = new Object[] { "BUY", "abcd", 8.88, 3.33, PageRequest.of(1, 2, Sort.Direction.DESC, "id") };

		preparePageResults(2, 2);

		when(this.queryMethod.getCollectionReturnType()).thenReturn(List.class);

		Page result = (Page) this.partTreeDatastoreQuery.execute(params);
		assertThat(result.getTotalElements()).isEqualTo(4);
		assertThat(result.getTotalPages()).isEqualTo(2);
		assertThat(result.getNumberOfElements()).isEqualTo(2);

		verify(this.datastoreTemplate, times(1))
				.queryKeysOrEntities(isA(EntityQuery.class), any());

		verify(this.datastoreTemplate, times(1))
				.queryKeysOrEntities(isA(KeyQuery.class), any());
	}

	@Test
	public void pageableQueryNoPageableParam() throws NoSuchMethodException {
		queryWithMockResult("findByActionAndSymbolAndPriceLessThanAndPriceGreater"
						+ "ThanEqualAndIdIsNullOrderByIdDesc", null,
				getClass().getMethod("tradeMethod", String.class, String.class, double.class, double.class));

		this.partTreeDatastoreQuery = createQuery(true, false);

		Object[] params = new Object[] { "BUY", "abcd", 8.88, 3.33 };

		preparePageResults(0, null);

		when(this.queryMethod.getCollectionReturnType()).thenReturn(List.class);

		Page result = (Page) this.partTreeDatastoreQuery.execute(params);
		assertThat(result.getTotalElements()).isEqualTo(4);
		assertThat(result.getTotalPages()).isEqualTo(1);

		verify(this.datastoreTemplate, times(1))
				.queryKeysOrEntities(isA(EntityQuery.class), any());

		verify(this.datastoreTemplate, times(1))
				.queryKeysOrEntities(isA(KeyQuery.class), any());
	}

	@Test
	public void sliceQueryLast() throws NoSuchMethodException {
		queryWithMockResult("findByActionAndSymbolAndPriceLessThanAndPriceGreater"
						+ "ThanEqualAndIdIsNull", null,
				getClass().getMethod("tradeMethod", String.class, String.class, double.class, double.class,
						Pageable.class));

		this.partTreeDatastoreQuery = createQuery(false, true);

		Object[] params = new Object[] { "BUY", "abcd", 8.88, 3.33, PageRequest.of(1, 2, Sort.Direction.DESC, "id") };

		prepareSliceResults(2, 3, 2);

		when(this.queryMethod.getCollectionReturnType()).thenReturn(List.class);

		Slice result = (Slice) this.partTreeDatastoreQuery.execute(params);
		assertThat(result.hasNext()).isEqualTo(false);
		assertThat(result.getNumberOfElements()).isEqualTo(2);


		verify(this.datastoreTemplate, times(1))
				.query(isA(EntityQuery.class), (Function) any());

		verify(this.datastoreTemplate, times(0))
				.queryKeysOrEntities(isA(KeyQuery.class), any());
	}

	@Test
	public void sliceQueryNoPageableParam() throws NoSuchMethodException {
		queryWithMockResult("findByActionAndSymbolAndPriceLessThanAndPriceGreater"
						+ "ThanEqualAndIdIsNullOrderByIdDesc", null,
				getClass().getMethod("tradeMethod", String.class, String.class, double.class, double.class));

		this.partTreeDatastoreQuery = createQuery(false, true);

		Object[] params = new Object[] { "BUY", "abcd", 8.88, 3.33 };

		prepareSliceResults(0, null, null);

		when(this.queryMethod.getCollectionReturnType()).thenReturn(List.class);

		Slice result = (Slice) this.partTreeDatastoreQuery.execute(params);
		assertThat(result.hasNext()).isEqualTo(false);


		verify(this.datastoreTemplate, times(1))
				.query(isA(EntityQuery.class), (Function) any());

		verify(this.datastoreTemplate, times(0))
				.queryKeysOrEntities(isA(KeyQuery.class), any());
	}

	@Test
	public void sliceQuery() throws NoSuchMethodException {
		queryWithMockResult("findByActionAndSymbolAndPriceLessThanAndPriceGreater"
						+ "ThanEqualAndIdIsNull", null,
				getClass().getMethod("tradeMethod", String.class, String.class, double.class, double.class,
						Pageable.class));

		this.partTreeDatastoreQuery = createQuery(false, true);

		Object[] params = new Object[] { "BUY", "abcd", 8.88, 3.33, PageRequest.of(0, 2, Sort.Direction.DESC, "id") };

		prepareSliceResults(0, 3, 3);

		when(this.queryMethod.getCollectionReturnType()).thenReturn(List.class);

		Slice result = (Slice) this.partTreeDatastoreQuery.execute(params);
		assertThat(result.hasNext()).isEqualTo(true);
		assertThat(result.getNumberOfElements()).isEqualTo(2);


		verify(this.datastoreTemplate, times(1))
				.query(isA(EntityQuery.class), (Function) any());

		verify(this.datastoreTemplate, times(0))
				.queryKeysOrEntities(isA(KeyQuery.class), any());
	}

	private void preparePageResults(int offset, Integer limit) {
		when(this.datastoreTemplate.queryKeysOrEntities(isA(EntityQuery.class), any())).thenAnswer((invocation) -> {
			EntityQuery statement = invocation.getArgument(0);
			EntityQuery expected = StructuredQuery.newEntityQueryBuilder()
					.setFilter(CompositeFilter.and(PropertyFilter.eq("action", "BUY"),
							PropertyFilter.eq("ticker", "abcd"),
							PropertyFilter.lt("price", 8.88),
							PropertyFilter.ge("price", 3.33),
							PropertyFilter.isNull("id")))
					.setKind("trades")
					.setOffset(offset)
					.setOrderBy(OrderBy.desc("id")).setLimit(limit).build();

			assertThat(statement).isEqualTo(expected);
			return Arrays.asList(3, 4);
		});

		when(this.datastoreTemplate.queryKeysOrEntities(isA(KeyQuery.class), any())).thenAnswer((invocation) -> {
			KeyQuery statement = invocation.getArgument(0);
			KeyQuery expected = StructuredQuery.newKeyQueryBuilder()
					.setFilter(CompositeFilter.and(PropertyFilter.eq("action", "BUY"),
							PropertyFilter.eq("ticker", "abcd"),
							PropertyFilter.lt("price", 8.88),
							PropertyFilter.ge("price", 3.33),
							PropertyFilter.isNull("id")))
					.setKind("trades")
					.setOrderBy(OrderBy.desc("id")).build();

			assertThat(statement).isEqualTo(expected);
			return Arrays.asList(1, 2, 3, 4);
		});
	}

	private void prepareSliceResults(int offset, Integer queryLimit, Integer resultLimit) {
		when(this.datastoreTemplate.query(isA(EntityQuery.class), (Function) any())).thenAnswer((invocation) -> {
			EntityQuery statement = invocation.getArgument(0);
			EntityQuery expected = StructuredQuery.newEntityQueryBuilder()
					.setFilter(CompositeFilter.and(PropertyFilter.eq("action", "BUY"),
							PropertyFilter.eq("ticker", "abcd"),
							PropertyFilter.lt("price", 8.88),
							PropertyFilter.ge("price", 3.33),
							PropertyFilter.isNull("id")))
					.setKind("trades")
					.setOffset(offset)
					.setOrderBy(OrderBy.desc("id")).setLimit(queryLimit).build();

			assertThat(statement).isEqualTo(expected);
			List<Integer> results = Arrays.asList(3, 4, 5);
			return (resultLimit != null && resultLimit < results.size()) ? results.subList(0, resultLimit)
					: results;
		});
		when(this.datastoreTemplate.convertEntitiesForRead(any(), any())).then(
				(invocation) -> Lists.newArrayList(invocation.<Iterator>getArgument(0))
		);
	}

	@Test
	public void unspecifiedParametersTest() throws NoSuchMethodException {
		this.expectedException.expectMessage(
				"Too few parameters are provided for query method: " +
						"findByActionAndSymbolAndPriceLessThanAndPriceGreaterThanEqualAndIdIsNullOrderByIdDesc");
		queryWithMockResult("countByTraderIdBetween", null,
				getClass().getMethod("countByAction", String.class));

		when(this.queryMethod.getName())
				.thenReturn("findByActionAndSymbolAndPriceLessThanAndPriceGreater"
						+ "ThanEqualAndIdIsNullOrderByIdDesc");
		this.partTreeDatastoreQuery = createQuery(false, false);

		// There are too few params specified, so the exception will occur.
		Object[] params = new Object[] { "BUY" };

		this.partTreeDatastoreQuery.execute(params);
	}

	@Test
	public void unsupportedParamTypeTest() throws NoSuchMethodException {
		this.expectedException.expectMessage(
				"Unable to convert class " +
						"org.springframework.cloud.gcp.data.datastore.repository.query." +
						"PartTreeDatastoreQueryTests$Trade to Datastore supported type.");
		queryWithMockResult("findByAction", null,
				getClass().getMethod("countByPrice", Integer.class));

		this.partTreeDatastoreQuery = createQuery(false, false);

		Object[] params = new Object[] { new Trade() };

		this.partTreeDatastoreQuery.execute(params);
	}

	@Test
	public void unSupportedPredicateTest() throws NoSuchMethodException {
		this.expectedException.expectMessage("Unsupported predicate keyword: BETWEEN");

		queryWithMockResult("countByTraderIdBetween", null, getClass().getMethod("traderAndPrice"));
		this.partTreeDatastoreQuery = createQuery(false, false);
		this.partTreeDatastoreQuery.execute(EMPTY_PARAMETERS);
	}

	@Test
	public void unSupportedOrTest() throws NoSuchMethodException {
		this.expectedException.expectMessage("Cloud Datastore only supports multiple filters combined with AND");

		queryWithMockResult("countByTraderIdOrPrice", null, getClass().getMethod("traderAndPrice"));

		//this.partTreeDatastoreQuery = createQuery();
		this.partTreeDatastoreQuery.execute(new Object[] { 123L, 45L});
	}

	@Test
	public void countTest() throws NoSuchMethodException {
		List<Trade> results = new ArrayList<>();
		results.add(new Trade());

		queryWithMockResult("countByAction", results, getClass().getMethod("countByAction", String.class));

		PartTreeDatastoreQuery spyQuery = this.partTreeDatastoreQuery;

		Object[] params = new Object[] { "BUY", };
		assertThat(spyQuery.execute(params)).isEqualTo(1L);
	}

	@Test
	public void existShouldBeTrueWhenResultSetIsNotEmpty() throws NoSuchMethodException {
		List<Trade> results = new ArrayList<>();
		results.add(new Trade());

		queryWithMockResult("existsByAction", results, getClass().getMethod("countByAction", String.class));

		PartTreeDatastoreQuery spyQuery = this.partTreeDatastoreQuery;

		doAnswer((invocation) -> invocation.getArgument(0)).when(spyQuery)
				.processRawObjectForProjection(any());

		Object[] params = new Object[] { "BUY", };
		assertThat((boolean) spyQuery.execute(params)).isTrue();
	}

	@Test
	public void existShouldBeFalseWhenResultSetIsEmpty() throws NoSuchMethodException {
		queryWithMockResult("existsByAction", Collections.emptyList(),
				getClass().getMethod("countByAction", String.class));

		PartTreeDatastoreQuery spyQuery = this.partTreeDatastoreQuery;

		doAnswer((invocation) -> invocation.getArgument(0)).when(spyQuery)
				.processRawObjectForProjection(any());

		Object[] params = new Object[] { "BUY", };
		assertThat((boolean) spyQuery.execute(params)).isFalse();
	}

	private void queryWithMockResult(String queryName, List results, Method m) {
		when(this.queryMethod.getName()).thenReturn(queryName);
		doReturn(new DefaultParameters(m))
				.when(this.queryMethod).getParameters();
		this.partTreeDatastoreQuery = createQuery(false, false);
		when(this.datastoreTemplate.queryKeysOrEntities(any(), Mockito.<Class<Trade>>any()))
				.thenReturn(results);
	}

	public List<Trade> tradeMethod(String action, String symbol, double pless, double pgreater) {
		return null;
	}

	public List<Trade> tradeMethod(String action, String symbol, double pless, double pgreater, Pageable pageable) {
		return null;
	}

	public List<Trade> tradeMethod(String action, String symbol, double pless, double pgreater, Sort sort) {
		return null;
	}

	public int traderAndPrice() {
		return 0;
	}

	public int countByAction(String action) {
		return 0;
	}

	public int countByPrice(Integer action) {
		return 0;
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
