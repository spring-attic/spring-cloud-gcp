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

package org.springframework.cloud.gcp.data.spanner.core.admin;

import java.util.List;
import java.util.OptionalLong;

import com.google.cloud.ByteArray;
import com.google.cloud.spanner.Key;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.cloud.gcp.data.spanner.core.convert.ConverterAwareMappingSpannerEntityProcessor;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerEntityProcessor;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.ColumnLength;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Embedded;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentProperty;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
public class SpannerSchemaUtilsTests {

	private SpannerSchemaUtils spannerSchemaUtils;

	private SpannerEntityProcessor spannerEntityProcessor;

	@Before
	public void setUp() {
		SpannerMappingContext spannerMappingContext = new SpannerMappingContext();
		this.spannerSchemaUtils = new SpannerSchemaUtils(spannerMappingContext,
				new ConverterAwareMappingSpannerEntityProcessor(spannerMappingContext));
		this.spannerEntityProcessor = new ConverterAwareMappingSpannerEntityProcessor(
				spannerMappingContext);
	}

	@Test
	public void getDropDDLTest() {
		assertEquals("DROP TABLE custom_test_table",
				this.spannerSchemaUtils.getDropTableDdlString(TestEntity.class));
	}

	@Test
	public void getCreateDDLTest() {
		assertEquals("CREATE TABLE custom_test_table ( id STRING(MAX) , id3 INT64 , "
						+ "id2 STRING(MAX) , bytes2 BYTES(MAX) , custom_col STRING(MAX) , other STRING(333) ,"
						+ " bytes BYTES(MAX) , bytesList ARRAY<BYTES(111)> , integerList ARRAY<INT64> , "
						+ "doubles ARRAY<FLOAT64> ) PRIMARY KEY ( id , id2 , id3 )",
				this.spannerSchemaUtils.getCreateTableDDLString(TestEntity.class));
	}

	@Test
	public void createDDLString() {
		assertColumnDDL(String.class, null,
				"id", OptionalLong.empty(),
				"id STRING(MAX)");
	}

	@Test
	public void createDDLStringCustomLength() {
		assertColumnDDL(String.class, null,
				"id", OptionalLong.of(333L),
				"id STRING(333)");
	}

	@Test
	public void createDDLBytesMax() {
		assertColumnDDL(ByteArray.class, null,
				"bytes", OptionalLong.empty(),
				"bytes BYTES(MAX)");
	}

	@Test
	public void createDDLBytesCustomLength() {
		assertColumnDDL(ByteArray.class, null,
				"bytes", OptionalLong.of(333L),
				"bytes BYTES(333)");
	}

	@Test
	public void ddlForListOfByteArray() {
		assertColumnDDL(List.class, ByteArray.class,
				"bytesList", OptionalLong.of(111L),
				"bytesList ARRAY<BYTES(111)>");
	}

	@Test
	public void ddlForDoubleArray() {
		assertColumnDDL(double[].class, null,
				"doubles", OptionalLong.of(111L),
				"doubles ARRAY<FLOAT64>");
	}

	@Test
	public void ddlForListOfListOfIntegers() {
		assertColumnDDL(List.class, Integer.class,
				"integerList", OptionalLong.empty(),
				"integerList ARRAY<INT64>");
	}

	@Test
	public void ddlForListOfListOfDoubles() {
		assertColumnDDL(List.class, Double.class,
				"doubleList", OptionalLong.empty(),
				"doubleList ARRAY<FLOAT64>");
	}

	private void assertColumnDDL(Class clazz, Class innerClazz, String name,
			OptionalLong length, String expectedDDL) {
		SpannerPersistentProperty spannerPersistentProperty = mock(SpannerPersistentProperty.class);

		// @formatter:off
		Mockito.<Class>when(spannerPersistentProperty.getType()).thenReturn(clazz);
		Mockito.<Class>when(spannerPersistentProperty.getColumnInnerType()).thenReturn(innerClazz);
		// @formatter:on

		when(spannerPersistentProperty.getColumnName()).thenReturn(name);
		when(spannerPersistentProperty.getMaxColumnLength()).thenReturn(length);
		assertEquals(expectedDDL,
				this.spannerSchemaUtils.getColumnDdlString(spannerPersistentProperty,
						this.spannerEntityProcessor));
	}

	@Test
	public void getIdTest() {
		TestEntity t = new TestEntity();
		t.id = "aaa";
		t.embeddedColumns = new EmbeddedColumns();
		t.embeddedColumns.id2 = "2";
		t.id3 = 3L;
		assertEquals(Key.newBuilder().append(t.id).appendObject(t.embeddedColumns.id2).append(t.id3).build(),
				this.spannerSchemaUtils.getKey(t));
	}

	@Table(name = "custom_test_table")
	private static class TestEntity {
		@PrimaryKey(keyOrder = 1)
		String id;

		@PrimaryKey(keyOrder = 3)
		long id3;

		@PrimaryKey(keyOrder = 2)
		@Embedded
		EmbeddedColumns embeddedColumns;

		@Column(name = "custom_col")
		String something;

		@ColumnLength(maxLength = 333)
		@Column(name = "")
		String other;

		ByteArray bytes;

		@ColumnLength(maxLength = 111)
		List<ByteArray> bytesList;

		List<Integer> integerList;

		double[] doubles;
	}

	private static class EmbeddedColumns {
		@PrimaryKey
		String id2;

		ByteArray bytes2;
	}
}
