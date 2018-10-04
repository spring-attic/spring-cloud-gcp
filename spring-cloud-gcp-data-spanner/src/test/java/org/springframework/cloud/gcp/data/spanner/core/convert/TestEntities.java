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

package org.springframework.cloud.gcp.data.spanner.core.convert;

import java.time.Instant;
import java.util.List;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Struct;
import com.google.spanner.v1.TypeCode;

import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Embedded;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Interleaved;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;

/**
 * @author Chengyuan Zhao
 * @author Balint Pato
 */
public class TestEntities {

	@Table(name = "custom_test_table")
	static class TestEntity {
		@PrimaryKey
		String id;

		@PrimaryKey(keyOrder = 3)
		String id4;

		@PrimaryKey(keyOrder = 2)
		@Embedded
		TestEmbeddedColumns testEmbeddedColumns;

		@Column(name = "custom_col")
		String stringField;

		@Column(name = "")
		boolean booleanField;

		// This long is forced to be stored as a String for testing
		@Column(spannerType = TypeCode.STRING)
		long longField;

		double doubleField;

		// This double array is forced to be stored as a String for testing
		@Column(spannerType = TypeCode.STRING)
		double[] doubleArray;

		// int is not a native Cloud Spanner type, so this will utilize custom conversions.
		int intField;

		List<Double> doubleList;

		List<String> stringList;

		List<Boolean> booleanList;

		// This long list is forced to be stored as a String for testing
		@Column(spannerType = TypeCode.STRING)
		List<Long> longList;

		List<Timestamp> timestampList;

		List<Date> dateList;

		List<ByteArray> bytesList;

		List<Instant> momentsInTime;

		Date dateField;

		Timestamp timestampField;

		ByteArray bytes;

		@Interleaved
		List<ChildTestEntity> childTestEntities;
	}

	static class ChildTestEntity {
		@PrimaryKey
		String id;

		@PrimaryKey(keyOrder = 3)
		String id4;

		@PrimaryKey(keyOrder = 2)
		@Embedded
		TestEmbeddedColumns testEmbeddedColumns;

		@PrimaryKey(keyOrder = 4)
		String id5;
	}

	static class TestEmbeddedColumns {
		@PrimaryKey
		String id2;

		@PrimaryKey(keyOrder = 2)
		String id3;

		int intField2;
	}

	@Table(name = "faulty_test_table")
	static class FaultyTestEntity {
		@PrimaryKey
		String id;

		TestEntity fieldWithUnsupportedType;
	}

	@Table(name = "faulty_test_table_2")
	static class FaultyTestEntity2 {
		@PrimaryKey
		String id;

		List<TestEntity> listWithUnsupportedInnerType;
	}

	@Table(name = "outer_test_entity")
	static class OuterTestEntity {
		@PrimaryKey
		String id;

		List<InnerTestEntity> innerTestEntities;
	}

	@Table(name = "outer_test_entity")
	static class OuterTestHoldingStructsEntity {
		@PrimaryKey
		String id;

		List<Struct> innerStructs;
	}

	@Table(name = "outer_test_entity")
	static class OuterTestHoldingStructEntity {
		@PrimaryKey
		String id;

		Struct innerStruct;
	}

	@Table(name = "outer_test_entity_flat")
	static class OuterTestEntityFlat {
		@PrimaryKey
		String id;

		List<Integer> innerLengths;
	}

	@Table(name = "outer_test_entity_flat_faulty")
	static class OuterTestEntityFlatFaulty {
		@PrimaryKey
		String id;

		Integer innerLengths;
	}

	static class InnerTestEntity {
		@PrimaryKey
		String value;

		String missingColumnValue;
	}

	static class SimpleConstructorTester {
		@PrimaryKey
		final String id;

		SimpleConstructorTester(String id) {
			this.id = id;
		}
	}

	static class TestEntityWithListWithZeroTypeArgs {
		@PrimaryKey
		List zeroArgsListOfObjects;
	}

	@Table(name = "outer_test_entity")
	static class OuterTestEntityWithConstructor {
		@PrimaryKey
		String id;

		List<InnerTestEntity> innerTestEntities;

		OuterTestEntityWithConstructor(String id, List<InnerTestEntity> innerTestEntities) {
			this.id = id;
			this.innerTestEntities = innerTestEntities;
		}
	}

	@Table(name = "custom_test_table")
	static class PartialConstructor {
		@PrimaryKey
		String id;

		@Column(name = "custom_col")
		String stringField;

		@Column(name = "")
		boolean booleanField;

		long longField;

		double doubleField;

		PartialConstructor(String id, String stringField, boolean booleanField) {
			this.id = id;
			this.stringField = stringField;
			this.booleanField = booleanField;
		}
	}
}
