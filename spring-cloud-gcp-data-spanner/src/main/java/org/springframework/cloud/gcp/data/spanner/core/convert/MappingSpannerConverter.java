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

import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Mutation.WriteBuilder;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Struct;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.CustomConversions.StoreConversions;
import org.springframework.util.Assert;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
public class MappingSpannerConverter implements SpannerConverter {

	private final MappingSpannerReadConverter readConverter;

	private final MappingSpannerWriteConverter writeConverter;

	public MappingSpannerConverter(SpannerMappingContext spannerMappingContext) {
		Assert.notNull(spannerMappingContext,
				"A valid mapping context for Spanner is required.");
		this.readConverter = new MappingSpannerReadConverter(spannerMappingContext,
				getCustomConversions(ConversionUtils.DEFAULT_SPANNER_READ_CONVERTERS));
		this.writeConverter = new MappingSpannerWriteConverter(spannerMappingContext,
				getCustomConversions(ConversionUtils.DEFAULT_SPANNER_WRITE_CONVERTERS));
	}

	public MappingSpannerConverter(SpannerMappingContext spannerMappingContext,
			Collection<Converter> writeConverters, Collection<Converter> readConverters) {
		Assert.notNull(spannerMappingContext,
				"A valid mapping context for Spanner is required.");
		this.readConverter = new MappingSpannerReadConverter(spannerMappingContext,
				getCustomConversions(readConverters));
		this.writeConverter = new MappingSpannerWriteConverter(spannerMappingContext,
				getCustomConversions(writeConverters));
	}

	public MappingSpannerConverter(SpannerMappingContext spannerMappingContext,
			CustomConversions writeConversions, CustomConversions readConversions) {
		Assert.notNull(spannerMappingContext,
				"A valid mapping context for Spanner is required.");
		this.readConverter = new MappingSpannerReadConverter(spannerMappingContext,
				readConversions);
		this.writeConverter = new MappingSpannerWriteConverter(spannerMappingContext,
				writeConversions);
	}

	private CustomConversions getCustomConversions(Collection<Converter> converters) {
		return new CustomConversions(StoreConversions.NONE, converters);
	}

	@Override
	public <T> List<T> mapToList(ResultSet resultSet, Class<T> entityClass) {
		return mapToList(resultSet, entityClass, Optional.empty());
	}

	@Override
	public <T> List<T> mapToList(ResultSet resultSet, Class<T> entityClass,
			Optional<Set<String>> includeColumns) {
		ArrayList<T> result = new ArrayList<>();
		while (resultSet.next()) {
			result.add(this.readConverter.read(entityClass,
					resultSet.getCurrentRowAsStruct(),
					includeColumns == null || !includeColumns.isPresent() ? null
							: includeColumns.get()));
		}
		resultSet.close();
		return result;
	}

	@Override
	public <T> List<T> mapToList(ResultSet resultSet, Class<T> entityClass,
			String... includeColumns) {
		return mapToList(resultSet, entityClass,
				includeColumns.length == 0 ? Optional.empty()
						: Optional.of(new HashSet<>(Arrays.asList(includeColumns))));
	}

	public void write(Object source, WriteBuilder sink, Set<String> includeColumns) {
		this.writeConverter.write(source, sink, includeColumns);
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
	public <R> R read(Class<R> type, Struct source) {
		return this.readConverter.read(type, source);
	}
}
