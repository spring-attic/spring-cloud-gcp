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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.cloud.ByteArray;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Mutation.WriteBuilder;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Struct;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.CustomConversions.StoreConversions;
import org.springframework.util.Assert;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
public class MappingSpannerConverter extends AbstractSpannerCustomConverter
		implements SpannerConverter {

	private static final Collection<Converter> DEFAULT_SPANNER_CONVERTERS = ImmutableSet
			.<Converter>builder()
			.addAll(SpannerConverters.DEFAULT_SPANNER_WRITE_CONVERTERS)
			.addAll(SpannerConverters.DEFAULT_SPANNER_READ_CONVERTERS).build();

	private static Set<Class> SPANNER_KEY_COMPATIBLE_TYPES = ImmutableSet
			.<Class>builder().add(Boolean.class).add(Integer.class).add(Long.class)
			.add(Float.class).add(Double.class).add(String.class).add(ByteArray.class)
			.add(Timestamp.class).add(com.google.cloud.Date.class).build();

	private final MappingSpannerReadConverter readConverter;

	private final MappingSpannerWriteConverter writeConverter;

	public MappingSpannerConverter(SpannerMappingContext spannerMappingContext) {
		super(getCustomConversions(DEFAULT_SPANNER_CONVERTERS), null);
		Assert.notNull(spannerMappingContext,
				"A valid mapping context for Spanner is required.");
		this.readConverter = new MappingSpannerReadConverter(spannerMappingContext,
				getCustomConversions(SpannerConverters.DEFAULT_SPANNER_READ_CONVERTERS));
		this.writeConverter = new MappingSpannerWriteConverter(spannerMappingContext,
				getCustomConversions(SpannerConverters.DEFAULT_SPANNER_WRITE_CONVERTERS));
	}

	public MappingSpannerConverter(SpannerMappingContext spannerMappingContext,
			Collection<Converter> writeConverters, Collection<Converter> readConverters) {
		super(getCustomConversions(
				ImmutableList.<Converter>builder().addAll(DEFAULT_SPANNER_CONVERTERS)
						.addAll(readConverters).addAll(writeConverters).build()),
				null);
		Assert.notNull(spannerMappingContext,
				"A valid mapping context for Spanner is required.");
		this.readConverter = new MappingSpannerReadConverter(spannerMappingContext,
				getCustomConversions(ImmutableList.<Converter>builder()
						.addAll(SpannerConverters.DEFAULT_SPANNER_READ_CONVERTERS)
						.addAll(readConverters).build()));
		this.writeConverter = new MappingSpannerWriteConverter(spannerMappingContext,
				getCustomConversions(ImmutableList.<Converter>builder()
						.addAll(SpannerConverters.DEFAULT_SPANNER_WRITE_CONVERTERS)
						.addAll(writeConverters).build()));
	}

	private static CustomConversions getCustomConversions(
			Collection<Converter> converters) {
		return new CustomConversions(StoreConversions.NONE, converters);
	}

	@VisibleForTesting
	MappingSpannerReadConverter getReadConverter() {
		return this.readConverter;
	}

	@VisibleForTesting
	MappingSpannerWriteConverter getWriteConverter() {
		return this.writeConverter;
	}

	@Override
	public boolean isValidSpannerKeyType(Class type) {
		return SPANNER_KEY_COMPATIBLE_TYPES.contains(type);
	}

	@Override
	public Set<Class> directlyWriteableSpannerTypes() {
		return MappingSpannerWriteConverter.singleItemType2ToMethodMap.keySet();
	}

	@Override
	public <T> List<T> mapToList(ResultSet resultSet, Class<T> entityClass) {
		return mapToList(resultSet, entityClass, Optional.empty(), false);
	}

	@Override
	public <T> List<T> mapToList(ResultSet resultSet, Class<T> entityClass,
			Optional<Set<String>> includeColumns, boolean allowMissingColumns) {
		ArrayList<T> result = new ArrayList<>();
		while (resultSet.next()) {
			result.add(this.readConverter.read(entityClass,
					resultSet.getCurrentRowAsStruct(),
					includeColumns == null || !includeColumns.isPresent() ? null
							: includeColumns.get(),
					allowMissingColumns));
		}
		resultSet.close();
		return result;
	}

	@Override
	public <T> List<T> mapToList(ResultSet resultSet, Class<T> entityClass,
			String... includeColumns) {
		return mapToList(resultSet, entityClass,
				includeColumns.length == 0 ? Optional.empty()
						: Optional.of(new HashSet<>(Arrays.asList(includeColumns))),
				false);
	}

	@Override
	public boolean canConvert(Class sourceType, Class targetType) {
		return super.canConvert(sourceType, targetType);
	}

	@Override
	public Object convert(Object source, Class targetType) {
		return super.convert(source, targetType);
	}

	@Override
	public Class getCorrespondingSpannerJavaType(Class originalType, boolean isIterableInnerType) {
		Set<Class> spannerTypes = (isIterableInnerType
				? MappingSpannerWriteConverter.iterablePropertyType2ToMethodMap
				: MappingSpannerWriteConverter.singleItemType2ToMethodMap).keySet();
		if (spannerTypes.contains(originalType)) {
			return originalType;
		}
		Class ret = null;
		for (Class spannerType : spannerTypes) {
			if (isIterableInnerType
					&& canHandlePropertyTypeForArrayRead(originalType, spannerType)
					&& canHandlePropertyTypeForArrayWrite(originalType, spannerType)) {
				ret = spannerType;
				break;
			}
			else if (!isIterableInnerType
					&& canHandlePropertyTypeForSingularRead(originalType, spannerType)
					&& canHandlePropertyTypeForSingularWrite(originalType, spannerType)) {
				ret = spannerType;
				break;
			}
		}
		return ret;
	}

	private boolean canHandlePropertyTypeForSingularRead(Class type,
			Class spannerSupportedType) {
		if (!MappingSpannerReadConverter.singleItemReadMethodMapping
				.containsKey(spannerSupportedType)) {
			throw new SpannerDataException(
					"The given spannerSupportedType type is not a known "
							+ "Spanner directly-supported column type: "
							+ spannerSupportedType);
		}
		return type.equals(spannerSupportedType)
				|| this.readConverter.canConvert(spannerSupportedType, type);
	}

	private boolean canHandlePropertyTypeForArrayRead(Class type,
			Class spannerSupportedArrayInnerType) {
		if (!MappingSpannerReadConverter.readIterableMapping
				.containsKey(spannerSupportedArrayInnerType)) {
			throw new SpannerDataException(
					"The given spannerSupportedArrayInnerType is not a known Spanner "
							+ "directly-supported array column inner-type: "
							+ spannerSupportedArrayInnerType);
		}
		return type.equals(spannerSupportedArrayInnerType)
				|| this.readConverter.canConvert(spannerSupportedArrayInnerType, type);
	}

	private boolean canHandlePropertyTypeForSingularWrite(Class type,
			Class spannerSupportedType) {
		if (!MappingSpannerWriteConverter.singleItemType2ToMethodMap
				.containsKey(spannerSupportedType)) {
			throw new SpannerDataException(
					"The given spannerSupportedType is not a known Spanner directly-supported column type: "
							+ spannerSupportedType);
		}
		return type.equals(spannerSupportedType)
				|| this.writeConverter.canConvert(type, spannerSupportedType);
	}

	private boolean canHandlePropertyTypeForArrayWrite(Class type,
			Class spannerSupportedArrayInnerType) {
		if (!MappingSpannerWriteConverter.iterablePropertyType2ToMethodMap
				.containsKey(spannerSupportedArrayInnerType)) {
			throw new SpannerDataException(
					"The given spannerSupportedArrayInnerType is not a known "
							+ "Spanner directly-supported column type: "
							+ spannerSupportedArrayInnerType);
		}
		return type.equals(spannerSupportedArrayInnerType)
				|| this.writeConverter.canConvert(type, spannerSupportedArrayInnerType);
	}

	/**
	 * Writes each of the source properties to the sink.
	 * @param source entity to be written
	 * @param sink the stateful {@link Mutation.WriteBuilder} as a target for writing.
	 */
	@Override
	public void write(Object source, Mutation.WriteBuilder sink) {
		this.writeConverter.write(source, sink);
	}

	@Override
	public void write(Object source, WriteBuilder sink, Set<String> includeColumns) {
		this.writeConverter.write(source, sink, includeColumns);
	}

	@Override
	public <R> R read(Class<R> type, Struct source) {
		return this.readConverter.read(type, source);
	}
}
