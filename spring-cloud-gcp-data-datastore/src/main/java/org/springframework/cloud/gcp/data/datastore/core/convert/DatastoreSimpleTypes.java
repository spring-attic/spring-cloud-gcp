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
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * A class to manage Datastore-specific simple type conversions.
 *
 * @author Dmitry Solomakha
 */
public class DatastoreSimpleTypes {

	public static final Set<Class<?>> DATASTORE_NATIVE_TYPES;

	public static final Set<Class<?>> ID_TYPES;

	public static final List<Class<?>> DATASTORE_NATIVE_TYPES_RESOLUTION;

	static {
		ID_TYPES = ImmutableSet.<Class<?>>builder()
				.add(String.class)
				.add(Long.class)
				.build();

		DATASTORE_NATIVE_TYPES_RESOLUTION = ImmutableList.<Class<?>>builder()
				.add(Boolean.class)
				.add(Long.class)
				.add(Double.class)
				.add(LatLng.class)
				.add(Timestamp.class)
				.add(String.class)
				.add(Blob.class)
				.build();

		DATASTORE_NATIVE_TYPES = ImmutableSet.<Class<?>>builder()
				.addAll(DATASTORE_NATIVE_TYPES_RESOLUTION)
				.build();
	}

	public static final SimpleTypeHolder HOLDER = new SimpleTypeHolder(DATASTORE_NATIVE_TYPES, true);


	final private Map<Class, Optional<Class<?>>> writeConverters = new HashMap<>();

	final private ConversionService conversionService;

	public DatastoreSimpleTypes(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public static boolean isSimple(Class aClass) {
		return DATASTORE_NATIVE_TYPES.contains(aClass);
	}

	public Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType) {
		if (isSimple(sourceType)) {
			return Optional.empty();
		}
		return this.writeConverters.computeIfAbsent(sourceType, this::getSimpleTypeWithBidirectionalConversion);
	}

	private Optional<Class<?>> getSimpleTypeWithBidirectionalConversion(Class inputType) {
		return DatastoreSimpleTypes.DATASTORE_NATIVE_TYPES_RESOLUTION.stream()
				.filter(simpleType ->
						this.conversionService.canConvert(inputType, simpleType)
						&& this.conversionService.canConvert(simpleType, inputType))
				.findAny();
	}
}
