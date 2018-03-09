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
import java.util.List;

import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Struct;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
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
		this.readConverter = new MappingSpannerReadConverter(spannerMappingContext);
		this.writeConverter = new MappingSpannerWriteConverter(spannerMappingContext);
	}

	@Override
	public <T> List<T> mapToList(ResultSet resultSet, Class<T> entityClass) {
		ArrayList<T> result = new ArrayList<>();
		while (resultSet.next()) {
			result.add(read(entityClass, resultSet.getCurrentRowAsStruct()));
		}
		return result;
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
