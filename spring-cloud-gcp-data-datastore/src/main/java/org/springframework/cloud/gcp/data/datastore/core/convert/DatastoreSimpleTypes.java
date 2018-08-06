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

package org.springframework.cloud.gcp.data.datastore.core.convert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.LatLng;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.springframework.core.convert.ConversionService;

/**
 * A class to manage Datastore-specific simple type conversions.
 *
 * @author Dmitry Solomakha
 */
public class DatastoreSimpleTypes implements StorageAwareConversions {

	public static final Set<Class> DATASTORE_SIMPLE_TYPES;

	public static final Set<Class> ID_TYPES;

	public static final List<Class> DATASTORE_SIMPLE_TYPES_RESOLUTION;

	static {
		ID_TYPES = ImmutableSet.<Class>builder()
				.add(String.class)
				.add(Long.class)
				.build();

		DATASTORE_SIMPLE_TYPES_RESOLUTION = ImmutableList.<Class>builder()
				.add(Boolean.class)
				.add(Long.class)
				.add(Double.class)
				.add(LatLng.class)
				.add(Timestamp.class)
				.add(String.class)
				.add(Blob.class)
				.build();

		DATASTORE_SIMPLE_TYPES = ImmutableSet.<Class>builder()
				.addAll(DATASTORE_SIMPLE_TYPES_RESOLUTION)
				.build();
	}

	final private Map<Class, Class> writeConverters = new HashMap<>();

	final private ConversionService conversionService;

	public DatastoreSimpleTypes(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public static boolean isSimple(Class aClass) {
		return DATASTORE_SIMPLE_TYPES.contains(aClass);
	}

	@Override
	public Class getWriteTarget(Class<?> sourceType) {
		return this.writeConverters.computeIfAbsent(sourceType, (Class inputType) -> {
			Optional<Class> targetType = getSimpleTypeWithBidirectionalConversion(inputType);
			return targetType.orElse(null);
		});
	}

	private Optional<Class> getSimpleTypeWithBidirectionalConversion(Class inputType) {
		return DatastoreSimpleTypes.DATASTORE_SIMPLE_TYPES_RESOLUTION.stream()
				.filter(simpleType ->
						this.conversionService.canConvert(inputType, simpleType)
						&& this.conversionService.canConvert(simpleType, inputType))
				.findAny();
	}
}
