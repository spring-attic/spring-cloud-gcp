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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Key;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Balint Pato
 */
@RunWith(Parameterized.class)
public class KeyConversionTests {
	private final SpannerWriteConverter writeConverter;

	private final ConverterAwareMappingSpannerEntityWriter spannerEntityWriter;

	private Object objectToTest;

	private Object expectedResult;

	public KeyConversionTests(String testCaseName, Object objectToTest, Object expectedResult) {
		this.objectToTest = objectToTest;
		this.expectedResult = expectedResult;
		this.writeConverter = new SpannerWriteConverter();
		this.spannerEntityWriter = new ConverterAwareMappingSpannerEntityWriter(new SpannerMappingContext(),
				this.writeConverter);
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static Collection<Object[]> types() {
		return Arrays.asList(new Object[][] {
				{ "single boolean", true, Key.of(true) },
				{ "single int", 1, Key.of(1) },
				{ "single long", 23123123L, Key.of(23123123L) },
				{ "single float", .223f, Key.of(.223f) },
				{ "single double", 3.14, Key.of(3.14) },
				{ "single string", "hello", Key.of("hello") },
				{ "single bytearray", ByteArray.copyFrom("world"), Key.of(ByteArray.copyFrom("world")) },
				{ "single timestamp", Timestamp.ofTimeMicroseconds(123132),
						Key.of(Timestamp.ofTimeMicroseconds(123132)) },
				{ "single date", Date.parseDate("2018-04-20"), Key.of(Date.parseDate("2018-04-20")) },
				{ "mixed array", new Object[] { 1, true, false, "hello", ByteArray.copyFrom("world") },
						Key.of(1, true, false, "hello", ByteArray.copyFrom("world")) },
				{ "mixed list", Arrays.asList(1, true, false, "hello", ByteArray.copyFrom("world")),
						Key.of(1, true, false, "hello", ByteArray.copyFrom("world")) },
				{ "converted default type (date)",
						java.util.Date.from(Instant.ofEpochSecond(123)),
						Key.of(SpannerConverters.JAVA_TO_SPANNER_DATE_CONVERTER
								.convert(java.util.Date.from(Instant.ofEpochSecond(123)))) },
				{ "unsupported type (TestEntity)", new TestEntities.TestEntity(), SpannerDataException.class },
				{ "empty key (Object[])", new Object[] {}, SpannerDataException.class },
				{ "empty key (List{})", Collections.emptyList(), SpannerDataException.class },
		});
	}

	@Test
	public void keyWritingTestCase() {
		try {
			Key key = this.spannerEntityWriter.writeToKey(this.objectToTest);
			assertThat(key, is(this.expectedResult));
		}
		catch (Exception e) {
			assertTrue("Unexpected exception: " + e + "\nexpected: " + this.expectedResult,
					e.getClass().equals(this.expectedResult));
		}
	}

}
