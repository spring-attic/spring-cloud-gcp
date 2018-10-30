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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreEntityConverter;
import org.springframework.cloud.gcp.data.datastore.core.convert.ReadWriteConversions;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Field;
import org.springframework.cloud.gcp.data.datastore.it.TestEntity;
import org.springframework.data.annotation.Id;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
public class PartTreeDatastoreQueryTests {

	private static final Object[] EMPTY_PARAMETERS = new Object[0];

	private DatastoreTemplate spannerTemplate;

	private DatastoreQueryMethod queryMethod;

	private DatastoreMappingContext spannerMappingContext;

	private PartTreeDatastoreQuery partTreeSpannerQuery;

	private DatastoreEntityConverter datastoreEntityConverter;

	private ReadWriteConversions readWriteConversions;

	@Before
	public void initMocks() {
		this.queryMethod = mock(DatastoreQueryMethod.class);
		when(this.queryMethod.getReturnedObjectType()).thenReturn((Class) TestEntity.class);
		this.spannerTemplate = mock(DatastoreTemplate.class);
		this.spannerMappingContext = new DatastoreMappingContext();
		this.datastoreEntityConverter = mock(DatastoreEntityConverter.class);
		this.readWriteConversions = mock(ReadWriteConversions.class);
		when(this.spannerTemplate.getDatastoreEntityConverter())
				.thenReturn(this.datastoreEntityConverter);
		when(this.datastoreEntityConverter.getConversions())
				.thenReturn(this.readWriteConversions);
	}

	private PartTreeDatastoreQuery<Trade> createQuery() {
		return new PartTreeDatastoreQuery<>(this.queryMethod, this.spannerTemplate,
				this.spannerMappingContext, Trade.class);
	}

	@Test
	public void compoundNameConventionTest() {
		when(this.queryMethod.getName())
				.thenReturn("findTop333ByActionAndSymbolAndPriceLessThanAndPriceGreater"
						+ "ThanEqualAndIdIsNullOrderByIdDesc");
		this.partTreeSpannerQuery = createQuery();

		Object[] params = new Object[] { "BUY", "abcd", 8.88, 3.33 };

		when(this.spannerTemplate.queryKeysOrEntities(any(), any())).thenAnswer(invocation -> {
			EntityQuery statement = invocation.getArgument(0);

			EntityQuery expected = StructuredQuery.newEntityQueryBuilder()
					.setFilter(CompositeFilter.and(PropertyFilter.eq("action", "BUY"),
							PropertyFilter.eq("ticker", "abcd"),
							PropertyFilter.lt("price", 8.88),
							PropertyFilter.ge("price", 3.33),
							PropertyFilter.isNull("id")))
					.setKind("trades")
					.setOrderBy(OrderBy.desc("id")).setLimit(333).build();

			assertEquals(expected, statement);

					return Collections.emptyList();
		});

		when(this.queryMethod.getCollectionReturnType()).thenReturn(List.class);

		this.partTreeSpannerQuery.execute(params);
		verify(this.spannerTemplate, times(1))
				.queryKeysOrEntities(any(), any());
	}

	@Test(expected = DatastoreDataException.class)
	public void unspecifiedParametersTest() {
		when(this.queryMethod.getName())
				.thenReturn("findByActionAndSymbolAndPriceLessThanAndPriceGreater"
						+ "ThanEqualAndIdIsNullOrderByIdDesc");
		this.partTreeSpannerQuery = createQuery();

		// There are too few params specified, so the exception will occur.
		Object[] params = new Object[] { "BUY", "abcd", 8.88 };

		this.partTreeSpannerQuery.execute(params);
	}

	@Test(expected = DatastoreDataException.class)
	public void unsupportedParamTypeTest() {
		when(this.queryMethod.getName())
				.thenReturn("findByActionAndSymbolAndPriceLessThanAndPriceGreater"
						+ "ThanEqualAndIdIsNullOrderByIdDesc");
		this.partTreeSpannerQuery = createQuery();

		// There are too few params specified, so the exception will occur.
		Object[] params = new Object[] { "BUY", "abcd", 8.88, new ArrayList<>() };

		this.partTreeSpannerQuery.execute(params);
	}

	@Test(expected = DatastoreDataException.class)
	public void unSupportedPredicateTest() {
		when(this.queryMethod.getName()).thenReturn("countByTraderIdBetween");
		this.partTreeSpannerQuery = createQuery();
		this.partTreeSpannerQuery.execute(EMPTY_PARAMETERS);
	}

	@Test(expected = DatastoreDataException.class)
	public void unSupportedOrTest() {
		when(this.queryMethod.getName()).thenReturn("countByTraderIdOrPrice");
		this.partTreeSpannerQuery = createQuery();
		this.partTreeSpannerQuery.execute(EMPTY_PARAMETERS);
	}

	@Test
	public void countTest() {
		List<Trade> results = new ArrayList<>();
		results.add(new Trade());

		queryWithMockResult("countByAction", results);

		PartTreeDatastoreQuery spyQuery = spy(this.partTreeSpannerQuery);
		Object[] params = new Object[] { "BUY", };
		assertEquals(1L, spyQuery.execute(params));
	}

	@Test
	public void existShouldBeTrueWhenResultSetIsNotEmpty() {
		List<Trade> results = new ArrayList<>();
		results.add(new Trade());

		queryWithMockResult("existsByAction", results);

		PartTreeDatastoreQuery spyQuery = spy(this.partTreeSpannerQuery);

		doAnswer(invocation -> invocation.getArgument(0)).when(spyQuery)
				.processRawObjectForProjection(any());

		Object[] params = new Object[] { "BUY", };
		assertTrue((boolean) spyQuery.execute(params));
	}

	@Test
	public void existShouldBeFalseWhenResultSetIsEmpty() {
		queryWithMockResult("existsByAction", Collections.emptyList());

		PartTreeDatastoreQuery spyQuery = spy(this.partTreeSpannerQuery);

		doAnswer(invocation -> invocation.getArgument(0)).when(spyQuery)
				.processRawObjectForProjection(any());

		Object[] params = new Object[] { "BUY", };
		assertFalse((boolean) spyQuery.execute(params));
	}

	private void queryWithMockResult(String queryName, List results) {
		when(this.queryMethod.getName()).thenReturn(queryName);
		this.partTreeSpannerQuery = createQuery();
		when(this.spannerTemplate.queryKeysOrEntities(any(), Mockito.<Class<Trade>>any()))
				.thenReturn(results);
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
