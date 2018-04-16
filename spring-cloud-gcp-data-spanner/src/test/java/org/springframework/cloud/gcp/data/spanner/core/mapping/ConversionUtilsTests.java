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

package org.springframework.cloud.gcp.data.spanner.core.mapping;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;

import org.junit.Test;

import org.springframework.cloud.gcp.data.spanner.core.convert.ConversionUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author Chengyuan Zhao
 */
public class ConversionUtilsTests {

	@Test
	public void dateConversionTest() {
		Date date = Date.fromYearMonthDay(2018, 3, 29);
		assertEquals(date, ConversionUtils.JAVA_TO_SPANNER_DATE_CONVERTER
				.convert(ConversionUtils.SPANNER_TO_JAVA_DATE_CONVERTER.convert(date)));
	}

	@Test
	public void sqlDateConversionTest() {
		Date date = Date.fromYearMonthDay(2018, 3, 29);
		assertEquals(date, ConversionUtils.JAVA_SQL_TO_SPANNER_DATE_CONVERTER
				.convert(ConversionUtils.SPANNER_TO_JAVA_SQL_DATE_CONVERTER.convert(date)));
	}

	@Test
	public void timestampInstantConversionTest() {
		Timestamp timestamp = Timestamp.ofTimeMicroseconds(12345678);
		assertEquals(timestamp, ConversionUtils.INSTANT_TIMESTAMP_CONVERTER
				.convert(ConversionUtils.TIMESTAMP_INSTANT_CONVERTER.convert(timestamp)));
	}

	@Test
	public void timestampConversionTest() {
		Timestamp timestamp = Timestamp.ofTimeMicroseconds(12345678);
		assertEquals(timestamp, ConversionUtils.JAVA_TO_SPANNER_TIMESTAMP_CONVERTER
				.convert(ConversionUtils.SPANNER_TO_JAVA_TIMESTAMP_CONVERTER.convert(timestamp)));
	}

	@Test
	public void bytesConversionTest() {
		ByteArray byteArray = ByteArray.copyFrom("some bytes");
		assertEquals(byteArray, ConversionUtils.JAVA_TO_SPANNER_BYTE_ARRAY_CONVERTER
				.convert(ConversionUtils.SPANNER_TO_JAVA_BYTE_ARRAY_CONVERTER.convert(byteArray)));
	}
}
