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

import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
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

		@Column(name = "custom_col")
		String stringField;

		@Column(name = "")
		boolean booleanField;

		long longField;

		double doubleField;

		double[] doubleArray;

		// int is not a native Spanner type, so this will utilize custom conversions.
		int intField;

		List<Double> doubleList;

		List<String> stringList;

		List<Boolean> booleanList;

		List<Long> longList;

		List<Timestamp> timestampList;

		List<Date> dateList;

		List<ByteArray> bytesList;

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

	static class InnerTestEntity {
		@PrimaryKey
		String value;
	}


	static class TestEntityWithListWithZeroTypeArgs {
		@PrimaryKey
		List zeroArgsListOfObjects;
	}
}
