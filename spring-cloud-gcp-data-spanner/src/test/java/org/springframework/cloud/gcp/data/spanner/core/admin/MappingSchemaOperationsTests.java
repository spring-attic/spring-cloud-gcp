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

import com.google.cloud.ByteArray;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.data.spanner.core.convert.MappingSpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.ColumnInnerType;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKeyColumn;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;

import static org.junit.Assert.assertEquals;

/**
 * @author Chengyuan Zhao
 */
public class MappingSchemaOperationsTests {

	private SpannerMappingContext spannerMappingContext;

	private MappingSchemaOperations mappingSchemaGenerator;

	@Before
	public void setUp() {
		this.spannerMappingContext = new SpannerMappingContext();
		this.mappingSchemaGenerator = new MappingSchemaOperations(
				this.spannerMappingContext,
				new MappingSpannerConverter(this.spannerMappingContext));
	}

	@Test
	public void getDropDDLTest() {
		assertEquals("DROP TABLE custom_test_table",
				this.mappingSchemaGenerator.getDropTableDDLString(TestEntity.class));
	}

	@Test
	public void getCreateDDLTest() {
		assertEquals("CREATE TABLE custom_test_table ( id STRING(MAX) , id2 INT64 "
				+ ", custom_col STRING(MAX) , other STRING(MAX) , bytes BYTES(MAX) "
				+ ", bytesList ARRAY<BYTES(MAX)> , integerList ARRAY<INT64> "
				+ ", doubles ARRAY<FLOAT64> ) PRIMARY KEY ( id , id2 )",
				this.mappingSchemaGenerator.getCreateTableDDLString(TestEntity.class));
	}

	@Test
	public void getCreateDDLHierachyTest() {
		List<String> createStrings = this.mappingSchemaGenerator
				.getCreateTableDDLStringsForHierarchy(ParentEntity.class);
		assertEquals(3, createStrings.size());
		assertEquals(
				"CREATE TABLE parent_test_table ( id STRING(MAX) , id_2 STRING(MAX) , "
						+ "custom_col STRING(MAX) , other STRING(MAX) ) PRIMARY KEY ( id , id_2 )",
				createStrings.get(0));
		assertEquals(
				"CREATE TABLE child_test_table ( id STRING(MAX) , id_2 STRING(MAX) , "
						+ "id3 STRING(MAX) ) PRIMARY KEY ( id , id_2 , id3 ), INTERLEAVE IN PARENT "
						+ "parent_test_table ON DELETE CASCADE",
				createStrings.get(1));
		assertEquals(
				"CREATE TABLE grand_child_test_table ( id STRING(MAX) , id_2 STRING(MAX) , "
						+ "id3 STRING(MAX) , id4 STRING(MAX) ) PRIMARY KEY ( id , id_2 , id3 , id4 ), "
						+ "INTERLEAVE IN PARENT child_test_table ON DELETE CASCADE",
				createStrings.get(2));
	}

	@Table(name = "custom_test_table")
	private static class TestEntity {
		@PrimaryKeyColumn(keyOrder = 1)
		String id;

		@PrimaryKeyColumn(keyOrder = 2)
		long id2;

		@Column(name = "custom_col")
		String something;

		@Column(name = "")
		String other;

		ByteArray bytes;

		@ColumnInnerType(innerType = ByteArray.class)
		List<ByteArray> bytesList;

		@ColumnInnerType(innerType = Integer.class)
		List<Integer> integerList;

		double[] doubles;
	}

	@Table(name = "parent_test_table")
	private static class ParentEntity {
		@PrimaryKeyColumn(keyOrder = 1)
		String id;

		@PrimaryKeyColumn(keyOrder = 2)
		@Column(name = "id_2")
		String id2;

		@Column(name = "custom_col")
		String something;

		@Column(name = "")
		String other;

		ChildEntity childEntity;

		@ColumnInnerType(innerType = ChildEntity.class)
		List<ChildEntity> childEntities;
	}

	@Table(name = "child_test_table")
	private static class ChildEntity {
		@PrimaryKeyColumn(keyOrder = 1)
		String id;

		@PrimaryKeyColumn(keyOrder = 2)
		String id_2;

		@PrimaryKeyColumn(keyOrder = 3)
		String id3;

		GrandChildEntity childEntity;

		@ColumnInnerType(innerType = GrandChildEntity.class)
		List<GrandChildEntity> childEntities;
	}

	@Table(name = "grand_child_test_table")
	private static class GrandChildEntity {
		@PrimaryKeyColumn(keyOrder = 1)
		String id;

		@PrimaryKeyColumn(keyOrder = 2)
		String id_2;

		@PrimaryKeyColumn(keyOrder = 3)
		String id3;

		@PrimaryKeyColumn(keyOrder = 4)
		String id4;
	}
}
