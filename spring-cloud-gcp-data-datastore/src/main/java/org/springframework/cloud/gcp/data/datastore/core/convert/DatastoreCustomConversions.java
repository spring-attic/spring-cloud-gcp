/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.datastore.core.convert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.cloud.datastore.BaseKey;
import com.google.cloud.datastore.Key;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.JodaTimeConverters;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.data.convert.ThreeTenBackPortConverters;

/**
 * Value object to capture custom conversion. {@link DatastoreCustomConversions}
 *
 * @author Dmitry Solomakha
 *
 * @since 1.1
 */
public class DatastoreCustomConversions extends CustomConversions {

	private static final StoreConversions STORE_CONVERSIONS;

	private static final List<Converter<?, ?>> STORE_CONVERTERS;

	static {
		ArrayList<Converter<?, ?>> converters = new ArrayList<>();
		converters.addAll(JodaTimeConverters.getConvertersToRegister());
		converters.addAll(Jsr310Converters.getConvertersToRegister());
		converters.addAll(ThreeTenBackPortConverters.getConvertersToRegister());
		converters.add(new Converter<BaseKey, Long>() {
			@Override
			public Long convert(BaseKey key) {
				Long id = null;
				// embedded entities have IncompleteKey, and have no inner value
				if (key instanceof Key) {
					id = ((Key) key).getId();
					if (id == null) {
						throw new DatastoreDataException("The given key doesn't have a numeric ID but a conversion" +
								" to Long was attempted: " + key);
					}
				}
				return id;
			}
		});
		converters.add(new Converter<BaseKey, String>() {
			@Override
			public String convert(BaseKey key) {
				String name = null;
				// embedded entities have IncompleteKey, and have no inner value
				if (key instanceof Key) {
					name = ((Key) key).getName();
					if (name == null) {
						throw new DatastoreDataException("The given key doesn't have a String name value but " +
								"a conversion to String was attempted: " + key);
					}
				}
				return name;
			}
		});
		STORE_CONVERTERS = Collections.unmodifiableList(converters);

		STORE_CONVERSIONS =
				StoreConversions.of(DatastoreNativeTypes.HOLDER, STORE_CONVERTERS);
	}

	public DatastoreCustomConversions() {
		this(Collections.emptyList());
	}

	public DatastoreCustomConversions(List<?> converters) {
		super(STORE_CONVERSIONS, converters);
	}

}
