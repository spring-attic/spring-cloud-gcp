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
import java.util.Set;

import com.google.cloud.spanner.ResultSet;

/**
 * Interface for processors that can populate fields from Spanner Structs and write them
 * to Spanner Mutations.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 * @author Balint Pato
 *
 * @since 1.1
 */
public interface SpannerEntityProcessor extends SpannerEntityReader, SpannerEntityWriter {

	/**
	 * Converts a set of Spanner {@link ResultSet} into a list of objects.
	 * @param resultSet The Spanner results to convert. The ResultSet will be exhausted and
	 * closed.
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
	 * @param includeColumns the Set of columns to read. If this param is null then all
	 * columns will be read.
	 * @param allowMissingColumns if true, then properties with no corresponding column
	 * are not mapped. If false, then an exception is thrown.
	 * @return A list of objects.
	 */
	<T> List<T> mapToList(ResultSet resultSet, Class<T> entityClass,
			Set<String> includeColumns, boolean allowMissingColumns);

	/**
	 * Converts a set of Spanner {@link ResultSet} into a list of objects.
	 * @param resultSet The Spanner results to convert. The ResultSet will be exhausted and
	 * closed.
	 * @param entityClass The type of the objects the Spanner results represent.
	 * @param <T> The type of the objects the Spanner results represent.
	 * @param includeColumns the columns to read. If none are provided then all columns are
	 * read.
	 * @return A list of objects.
	 */
	<T> List<T> mapToList(ResultSet resultSet, Class<T> entityClass,
			String... includeColumns);

	/**
	 * Gets the type that will work for both read and writes with Spanner directly.
	 * @param originalType the original type that is possibly convertable by this converter.
	 * @param isIterableInnerType true if the given type refers to an inner type. This is
	 * significant because Spanner does not support the same types as singular items and as
	 * array elements.
	 * @return the Java type that works directly with Spanner.
	 */
	Class getCorrespondingSpannerJavaType(Class originalType, boolean isIterableInnerType);

	/**
	 * Get the write converter used by this processor.
	 * @return the write converter.
	 */
	SpannerWriteConverter getWriteConverter();

	/**
	 * Get the read converter used by this processor.
	 * @return the read converter.
	 */
	SpannerReadConverter getReadConverter();
}
