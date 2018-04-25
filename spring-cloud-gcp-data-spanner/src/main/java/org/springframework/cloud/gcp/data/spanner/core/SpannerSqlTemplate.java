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

package org.springframework.cloud.gcp.data.spanner.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.cloud.spanner.AbstractStructReader;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Type.Code;
import com.google.cloud.spanner.Type.StructField;
import com.google.common.collect.ImmutableMap;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;

/**
 * @author Chengyuan Zhao
 */
public abstract class SpannerSqlTemplate {

	private static final Map<Type, BiFunction<Struct, String, Object>> STRUCT_GETTER_MAPPING =
			ImmutableMap
			.<Type, BiFunction<Struct, String, Object>>builder()
			.put(Type.bool(), AbstractStructReader::getBoolean)
			.put(Type.bytes(), AbstractStructReader::getBytes)
			.put(Type.date(), AbstractStructReader::getDate)
			.put(Type.float64(), AbstractStructReader::getDouble)
			.put(Type.int64(), AbstractStructReader::getLong)
			.put(Type.string(), AbstractStructReader::getString)
			.put(Type.array(Type.float64()), AbstractStructReader::getDoubleArray)
			.put(Type.array(Type.int64()), AbstractStructReader::getLongArray)
			.put(Type.array(Type.bool()), AbstractStructReader::getBooleanArray)
			.put(Type.array(Type.string()), AbstractStructReader::getStringList)
			.put(Type.array(Type.bytes()), AbstractStructReader::getBytesList)
			.put(Type.array(Type.date()), AbstractStructReader::getDateList)
			.put(Type.array(Type.timestamp()), AbstractStructReader::getTimestampList)
			.put(Type.timestamp(), AbstractStructReader::getTimestamp).build();

	/**
	 * Performs a read on Spanner.
	 * @param tableName the name of the table to read from
	 * @param keys the keys of the rows to retrieve
	 * @param columns the columns to retrieve.
	 * @param options the options which which to perform the read. For example, staleness
	 * of read or secondary index.
	 * @param rowMapper a function that maps from a Map to a value. The keys in the map
	 * are column names or component names of nested structs, and the values are values in
	 * those columns or struct components.
	 * @return The list of mapped objects
	 */
	public <T> List<T> executeRead(String tableName, KeySet keys,
			Iterable<String> columns, SpannerReadOptions options,
			Function<Map<String, Object>, T> rowMapper) {
		return mapRows(executeRead(tableName, keys, columns, options), rowMapper);
	}

	/**
	 * Performs a read operation using an SQL statement.
	 * @param statement the statement holding the SQL and parameters to run.
	 * @param options the query options. For example, the staleness of the read. * @param
	 * rowMapper a function that maps from a Map to a value. The keys in the map are
	 * column names or component names of nested structs, and the values are values in
	 * those columns or struct components.
	 * @return The {@link ResultSet} of rows.
	 */
	public <T> List<T> executeQuery(Statement statement, SpannerQueryOptions options,
			Function<Map<String, Object>, T> rowMapper) {
		return mapRows(executeQuery(statement, options), rowMapper);
	}

	private <T> List<T> mapRows(ResultSet resultSet,
			Function<Map<String, Object>, T> rowMapper) {
		List<T> result = new ArrayList<>();
		while (resultSet.next()) {
			result.add(rowMapper.apply(mapStruct(resultSet.getCurrentRowAsStruct())));
		}
		resultSet.close();
		return result;
	}

	private Map<String, Object> mapStruct(Struct struct) {
		Map<String, Object> map = new HashMap<>();
		for (StructField sf : struct.getType().getStructFields()) {
			Type type = sf.getType();
			String name = sf.getName();
			if (type.getCode() == Code.ARRAY
					&& type.getArrayElementType().getCode() == Code.STRUCT) {
				map.put(name, struct.getStructList(name).stream().map(s -> mapStruct(s))
						.collect(Collectors.toList()));
			}
			else {
				BiFunction<Struct, String, Object> getter = STRUCT_GETTER_MAPPING
						.get(type);
				if (getter == null) {
					throw new SpannerDataException(
							"Could not find getter for Spanner type: " + type);
				}
				map.put(name, getter.apply(struct, name));
			}
		}
		return map;
	}

	protected abstract ResultSet executeRead(String tableName, KeySet keys,
			Iterable<String> columns, SpannerReadOptions options);

	protected abstract ResultSet executeQuery(Statement statement,
			SpannerQueryOptions options);
}
