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

import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.ColumnInnerType;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;

/**
 * @author Balint Pato
 */
public class Entities {

	@Table(name = "custom_test_table")
	static class TestEntity {
		@PrimaryKey
		String id;

		@Column(name = "custom_col")
		String stringField;

		@Column(name = "")
		boolean booleanField;

		long longField;

		double doubleField;

		double[] doubleArray;

		// int is not a native Spanner type, so this will utilize custom conversions.
		int intField;

		@ColumnInnerType(innerType = Double.class)
		List<Double> doubleList;

		@ColumnInnerType(innerType = String.class)
		List<String> stringList;

		@ColumnInnerType(innerType = Boolean.class)
		List<Boolean> booleanList;

		@ColumnInnerType(innerType = Long.class)
		List<Long> longList;

		@ColumnInnerType(innerType = Timestamp.class)
		List<Timestamp> timestampList;

		@ColumnInnerType(innerType = Date.class)
		List<Date> dateList;

		@ColumnInnerType(innerType = ByteArray.class)
		List<ByteArray> bytesList;

		@ColumnInnerType(innerType = Instant.class)
		List<Instant> momentsInTime;

		Date dateField;

		Timestamp timestampField;

		ByteArray bytes;
	}

	@Table(name = "faulty_test_table")
	static class FaultyTestEntity {
		@PrimaryKey
		String id;

		TestEntity fieldWithUnsupportedType;

		List<Double> doubleList;
	}

	@Table(name = "faulty_test_table_2")
	static class FaultyTestEntity2 {
		@PrimaryKey
		String id;

		@ColumnInnerType(innerType = TestEntity.class)
		List<TestEntity> listWithUnsupportedInnerType;
	}

	@Table(name = "outer_test_entity")
	static class OuterTestEntity {
		@PrimaryKey
		String id;

		@ColumnInnerType(innerType = InnerTestEntity.class)
		List<InnerTestEntity> innerTestEntities;
	}

	static class InnerTestEntity {
		@PrimaryKey
		String value;
	}

	static class MockResults {
		List<Struct> structs;

		int counter = -1;

		boolean next() {
			if (this.counter < this.structs.size() - 1) {
				this.counter++;
				return true;
			}
			return false;
		}

		Struct getCurrent() {
			return this.structs.get(this.counter);
		}
	}

}
