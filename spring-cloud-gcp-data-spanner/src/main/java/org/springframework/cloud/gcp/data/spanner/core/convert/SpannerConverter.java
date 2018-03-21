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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Mutation.WriteBuilder;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Struct;

import org.springframework.data.convert.EntityReader;
import org.springframework.data.convert.EntityWriter;

/**
 * Interface for mappers that can populate fields from Spanner results.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 * @author Balint Pato
 */
public interface SpannerConverter extends EntityReader<Object, Struct>,
		EntityWriter<Object, Mutation.WriteBuilder> {

	/**
	 * Converts a set of Spanner {@link ResultSet} into a list of objects.
	 * @param resultSet The Spanner results to convert. The ResultSet will be exhausted
	 * and closed.
	 * @param entityClass The type of the objects the Spanner results represent.
	 * @param <T> The type of the objects the Spanner results represent.
	 * @return A list of objects.
	 */
	<T> List<T> mapToList(ResultSet resultSet, Class<T> entityClass);

	/**
	 * Converts a set of Spanner {@link ResultSet} into a list of objects.
	 * @param resultSet The Spanner results to convert. The ResultSet will be exhausted
	 * and closed.
	 * @param entityClass The type of the objects the Spanner results represent.
	 * @param <T> The type of the objects the Spanner results represent.
	 * @param includeColumns the Set of columns to read. If the Set is not present or this
	 * param is null then all columns will be read.
	 * @return A list of objects.
	 */
	<T> List<T> mapToList(ResultSet resultSet, Class<T> entityClass,
			Optional<Set<String>> includeColumns);

	/**
	 * Converts a set of Spanner {@link ResultSet} into a list of objects.
	 * @param resultSet The Spanner results to convert. The ResultSet will be exhausted
	 * and closed.
	 * @param entityClass The type of the objects the Spanner results represent.
	 * @param <T> The type of the objects the Spanner results represent.
	 * @param includeColumns the columns to read. If none are provided then all columns
	 * are read.
	 * @return A list of objects.
	 */
	<T> List<T> mapToList(ResultSet resultSet, Class<T> entityClass,
			String... includeColumns);

	/**
	 * Writes an object's properties to the sink.
	 * @param source the object to write
	 * @param sink the sink to which to write
	 * @param includeColumns the properties/columns to write. If null, then all columns
	 * are written.
	 */
	void write(Object source, WriteBuilder sink, Set<String> includeColumns);
}
