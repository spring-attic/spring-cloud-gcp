/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.datastore.core.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.core.convert.converter.Converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TwoStepsConversions}.
 *
 * Higher level tests invoking {@link TwoStepsConversions} as part of read/write
 * operations are in {@link TestDatastoreItemCollections}.
 *
 * @author Elena Felder
 * @since 1.1
 */
public class TwoStepsConversionsTests {

	private final TwoStepsConversions twoStepsConversions = new TwoStepsConversions(
			new DatastoreCustomConversions(Arrays.asList()), null);

	@Test
	public void convertOnReadReturnsNullWhenConvertingNullSimpleValue() {

		assertThat(this.twoStepsConversions.<String>convertOnRead(null, null, String.class))
				.isNull();
	}

	@Test
	public void convertOnReadThrowsNpeWhenConvertingCollectionWithNullElement() {

		List<String> listWithNull = new ArrayList<>();
		listWithNull.add(null);

		assertThatThrownBy(() -> {
			this.twoStepsConversions.convertOnRead(listWithNull, Set.class, String.class);
		}).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Cannot convert a null value.");
	}

	@Test
	public void convertOnReadConvertsCollectionAndElementTypesCorrectly() {

		List<String> okayList = new ArrayList<>();
		okayList.add("128");
		okayList.add("256");

		Set<Integer> result = this.twoStepsConversions.<Set<Integer>>convertOnRead(okayList, Set.class, Integer.class);
		assertThat(result).isNotNull();
		assertThat(result).containsExactlyInAnyOrder(128, 256);
	}

	@Test
	public void convertOnReadConvertsSimpleElementTypesCorrectly() {

		Integer result = this.twoStepsConversions.<Integer>convertOnRead("512", null, Integer.class);
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo(512);
	}

	@Test
	public void convertOnReadFailsOnIncompatibleTypes() {
		assertThatThrownBy(() -> {
			this.twoStepsConversions.<String>convertOnRead(3, null, String.class);
		}).isInstanceOf(DatastoreDataException.class)
		.hasMessageContaining("Unable to convert class java.lang.Integer to class java.lang.String");
	}


	@Test
	public void convertOnReadUsesCustomConverter() {
		List<String> numberNames = Arrays.asList("zero", "one", "two", "three", "four", "five");
		Converter<Long, String> converter = new Converter<Long, String>() {
			@Override
			public String convert(Long num) {
				if (num < 0 || num > 5) {
					return null;
				}
				return numberNames.get(num.intValue());
			}
		};

		TwoStepsConversions twoStepsConversionsThatSpeaksEnglish = new TwoStepsConversions(
				new DatastoreCustomConversions(Arrays.asList(converter)), null);
		String result = twoStepsConversionsThatSpeaksEnglish.<String>convertOnRead(3L, null, String.class);
		assertThat(result).isEqualTo("three");
	}

}
