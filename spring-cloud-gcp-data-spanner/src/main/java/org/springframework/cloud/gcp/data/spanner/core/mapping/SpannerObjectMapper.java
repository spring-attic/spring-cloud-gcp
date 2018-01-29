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

package org.springframework.cloud.gcp.data.spanner.core.mapping;

import java.util.List;

import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Struct;

/**
 * Interface for mappers that can populate fields from Spanner results.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 */
public interface SpannerObjectMapper {

	/**
	 * Populates an object with data from a Spanner row
	 * @param s The struct holding the Spanner row's data
	 * @param target The object whose fields will be populated
	 */
	void map(Struct s, Object target);

	/**
	 * Converts a set of Spanner results into a list of objects.
	 * @param resultSet The Spanner results to convert.
	 * @param entityClass The type of the objects the Spanner results represent.
	 * @param <T> The type of the objects the Spanner results represent.
	 * @return A list of objects.
	 */
	<T> List<T> mapToUnmodifiableList(ResultSet resultSet, Class<T> entityClass);

}
