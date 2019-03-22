/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.data.spanner.core.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Struct;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

/**
 * The primary class for writing entity objects to Spanner and creating entity objects
 * from rows stored in Spanner.
 *
 * @author Chengyuan Zhao
 * @author Balint Pato
 *
 * @since 1.1
 */
public class ConverterAwareMappingSpannerEntityProcessor implements SpannerEntityProcessor {

	private final ConverterAwareMappingSpannerEntityReader entityReader;

	private final ConverterAwareMappingSpannerEntityWriter entityWriter;

	private final SpannerReadConverter readConverter;

	private final SpannerWriteConverter writeConverter;

	public ConverterAwareMappingSpannerEntityProcessor(SpannerMappingContext spannerMappingContext) {
		this(spannerMappingContext, null, null);
	}

	public ConverterAwareMappingSpannerEntityProcessor(SpannerMappingContext spannerMappingContext,
			Collection<Converter> writeConverters, Collection<Converter> readConverters) {
		Assert.notNull(spannerMappingContext,
				"A valid mapping context for Spanner is required.");

		this.readConverter = new SpannerReadConverter(readConverters);
		this.entityReader = new ConverterAwareMappingSpannerEntityReader(spannerMappingContext, this.readConverter);
		this.writeConverter = new SpannerWriteConverter(writeConverters);
		this.entityWriter = new ConverterAwareMappingSpannerEntityWriter(spannerMappingContext, this.writeConverter);
	}

	@Override
	public <T> List<T> mapToList(ResultSet resultSet, Class<T> entityClass) {
		return mapToList(resultSet, entityClass, null, false);
	}

	@Override
	public <T> List<T> mapToList(ResultSet resultSet, Class<T> entityClass,
			Set<String> includeColumns, boolean allowMissingColumns) {
		ArrayList<T> result = new ArrayList<>();
		while (resultSet.next()) {
			result.add(this.entityReader.read(entityClass,
					resultSet.getCurrentRowAsStruct(),
					includeColumns,
					allowMissingColumns));
		}
		resultSet.close();
		return result;
	}

	@Override
	public <T> List<T> mapToList(ResultSet resultSet, Class<T> entityClass,
			String... includeColumns) {
		return mapToList(resultSet, entityClass,
				(includeColumns.length == 0) ? null
						: new HashSet<>(Arrays.asList(includeColumns)),
				false);
	}

	@Override
	public Class<?> getCorrespondingSpannerJavaType(Class originalType, boolean isIterableInnerType) {
		Class<?> compatible;
		if (isIterableInnerType) {
			if (ConverterAwareMappingSpannerEntityWriter.iterablePropertyType2ToMethodMap.keySet()
					.contains(originalType)) {
				return originalType;
			}
			compatible = ConverterAwareMappingSpannerEntityWriter.findFirstCompatibleSpannerMultupleItemNativeType(
					(spannerType) -> canHandlePropertyTypeForArrayRead(originalType, spannerType)
							&& this.writeConverter.canConvert(originalType, spannerType));
		}
		else {
			if (ConverterAwareMappingSpannerEntityWriter.singleItemTypeValueBinderMethodMap.keySet()
					.contains(originalType)) {
				return originalType;
			}
			compatible = ConverterAwareMappingSpannerEntityWriter
					.findFirstCompatibleSpannerSingleItemNativeType(
							(spannerType) -> canHandlePropertyTypeForSingularRead(originalType, spannerType)
									&& this.writeConverter.canConvert(originalType, spannerType));
		}
		return compatible;
	}

	private boolean canHandlePropertyTypeForSingularRead(Class type,
			Class spannerSupportedType) {
		if (!StructAccessor.singleItemReadMethodMapping
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
		if (!StructAccessor.readIterableMapping
				.containsKey(spannerSupportedArrayInnerType)) {
			throw new SpannerDataException(
					"The given spannerSupportedArrayInnerType is not a known Spanner "
							+ "directly-supported array column inner-type: "
							+ spannerSupportedArrayInnerType);
		}
		return type.equals(spannerSupportedArrayInnerType)
				|| this.readConverter.canConvert(spannerSupportedArrayInnerType, type);
	}

	/**
	 * Writes each of the source properties to the sink.
	 * @param source entity to be written
	 * @param sink the stateful multiple-value-binder as a target for writing.
	 */
	@Override
	public void write(Object source, MultipleValueBinder sink) {
		this.entityWriter.write(source, sink);
	}

	@Override
	public void write(Object source, MultipleValueBinder sink,
			Set<String> includeColumns) {
		this.entityWriter.write(source, sink, includeColumns);
	}

	@Override
	public Key convertToKey(Object key) {
		return this.entityWriter.convertToKey(key);
	}

	@Override
	public SpannerWriteConverter getSpannerWriteConverter() {
		return this.entityWriter.getSpannerWriteConverter();
	}

	@Override
	public <R> R read(Class<R> type, Struct source, Set<String> includeColumns, boolean allowMissingColumns) {
		return this.entityReader.read(type, source, includeColumns, allowMissingColumns);
	}

	@Override
	public SpannerWriteConverter getWriteConverter() {
		return this.writeConverter;
	}

	@Override
	public SpannerReadConverter getReadConverter() {
		return this.readConverter;
	}
}
