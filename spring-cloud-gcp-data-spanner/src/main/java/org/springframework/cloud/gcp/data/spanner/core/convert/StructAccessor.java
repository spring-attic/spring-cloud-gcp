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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AbstractStructReader;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Type.Code;
import com.google.common.collect.ImmutableMap;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;

/**
 * A convenience wrapper class around Struct to make reading columns easier without
 * knowing their type.
 *
 * @author Balint Pato
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class StructAccessor {

	// @formatter:off
	static final Map<Class, BiFunction<Struct, String, List>> readIterableMapping =
					new ImmutableMap.Builder<Class, BiFunction<Struct, String, List>>()
					// @formatter:on
					.put(Boolean.class, AbstractStructReader::getBooleanList)
					.put(Long.class, AbstractStructReader::getLongList)
					.put(String.class, AbstractStructReader::getStringList)
					.put(Double.class, AbstractStructReader::getDoubleList)
					.put(Timestamp.class, AbstractStructReader::getTimestampList)
					.put(Date.class, AbstractStructReader::getDateList)
					.put(ByteArray.class, AbstractStructReader::getBytesList)
					.put(Struct.class, AbstractStructReader::getStructList)
					.build();

	// @formatter:off
	static final Map<Class, BiFunction<Struct, String, ?>> singleItemReadMethodMapping =
					new ImmutableMap.Builder<Class, BiFunction<Struct, String, ?>>()
					// @formatter:on
					.put(Boolean.class, AbstractStructReader::getBoolean)
					.put(Long.class, AbstractStructReader::getLong)
					.put(long.class, AbstractStructReader::getLong)
					.put(String.class, AbstractStructReader::getString)
					.put(Double.class, AbstractStructReader::getDouble)
					.put(double.class, AbstractStructReader::getDouble)
					.put(Timestamp.class, AbstractStructReader::getTimestamp)
					.put(Date.class, AbstractStructReader::getDate)
					.put(ByteArray.class, AbstractStructReader::getBytes)
					.put(double[].class, AbstractStructReader::getDoubleArray)
					.put(long[].class, AbstractStructReader::getLongArray)
					.put(boolean[].class, AbstractStructReader::getBooleanArray)
					// Note that Struct.class appears in this map. While we support
					// converting structs into POJO fields of POJOs, the value in this map is for
					// the case where the field within the POJO is Struct.
					.put(Struct.class, Struct::getStruct).build();

	// @formatter:off
	static final Map<Class, BiFunction<Struct, Integer, List>> readIterableMappingIntCol =
			new ImmutableMap.Builder<Class, BiFunction<Struct, Integer, List>>()
					// @formatter:on
					.put(Boolean.class, AbstractStructReader::getBooleanList)
					.put(Long.class, AbstractStructReader::getLongList)
					.put(String.class, AbstractStructReader::getStringList)
					.put(Double.class, AbstractStructReader::getDoubleList)
					.put(Timestamp.class, AbstractStructReader::getTimestampList)
					.put(Date.class, AbstractStructReader::getDateList)
					.put(ByteArray.class, AbstractStructReader::getBytesList)
					.put(Struct.class, AbstractStructReader::getStructList).build();

	// @formatter:off
	static final Map<Class, BiFunction<Struct, Integer, ?>> singleItemReadMethodMappingIntCol =
			new ImmutableMap.Builder<Class, BiFunction<Struct, Integer, ?>>()
					// @formatter:on
					.put(Boolean.class, AbstractStructReader::getBoolean)
					.put(Long.class, AbstractStructReader::getLong)
					.put(long.class, AbstractStructReader::getLong)
					.put(String.class, AbstractStructReader::getString)
					.put(Double.class, AbstractStructReader::getDouble)
					.put(double.class, AbstractStructReader::getDouble)
					.put(Timestamp.class, AbstractStructReader::getTimestamp)
					.put(Date.class, AbstractStructReader::getDate)
					.put(ByteArray.class, AbstractStructReader::getBytes)
					.put(double[].class, AbstractStructReader::getDoubleArray)
					.put(long[].class, AbstractStructReader::getLongArray)
					.put(boolean[].class, AbstractStructReader::getBooleanArray)
					// Note that Struct.class appears in this map. While we support
					// converting structs into POJO fields of POJOs, the value in this map
					// is for
					// the case where the field within the POJO is Struct.
					.put(Struct.class, Struct::getStruct).build();


	private Struct struct;

	private Set<String> columnNamesIndex;

	public StructAccessor(Struct struct) {
		this.struct = struct;
		this.columnNamesIndex = indexColumnNames();
	}

	Object getSingleValue(String colName) {
		Type colType = this.struct.getColumnType(colName);
		Class sourceType = getSingleItemTypeCode(colType);
		BiFunction readFunction = singleItemReadMethodMapping.get(sourceType);
		if (readFunction == null) {
			// This case should only occur if the POJO field is non-Iterable, but the column type
			// is ARRAY of STRUCT, TIMESTAMP, DATE, BYTES, or STRING. This use-case is not supported.
			return null;
		}
		return readFunction.apply(this.struct, colName);
	}

	public Object getSingleValue(int colIndex) {
		Type colType = this.struct.getColumnType(colIndex);
		Class sourceType = getSingleItemTypeCode(colType);
		BiFunction readFunction = singleItemReadMethodMappingIntCol.get(sourceType);
		if (readFunction == null) {
			// This case should only occur if the POJO field is non-Iterable, but the
			// column type
			// is ARRAY of STRUCT, TIMESTAMP, DATE, BYTES, or STRING. This use-case is not
			// supported.
			return null;
		}
		return readFunction.apply(this.struct, colIndex);
	}

	List getListValue(String colName) {
		if (this.struct.getColumnType(colName).getCode() != Code.ARRAY) {
			throw new SpannerDataException("Column is not an ARRAY type: " + colName);
		}
		Type.Code innerTypeCode = this.struct.getColumnType(colName).getArrayElementType().getCode();
		Class clazz = SpannerTypeMapper.getSimpleJavaClassFor(innerTypeCode);
		BiFunction<Struct, String, List> readMethod = readIterableMapping.get(clazz);
		return readMethod.apply(this.struct, colName);
	}

	boolean hasColumn(String columnName) {
		return this.columnNamesIndex.contains(columnName);
	}

	boolean isNull(String columnName) {
		return this.struct.isNull(columnName);
	}

	private Set<String> indexColumnNames() {
		Set<String> cols = new HashSet<>();
		for (Type.StructField f : this.struct.getType().getStructFields()) {
			cols.add(f.getName());
		}
		return cols;
	}

	private Class getSingleItemTypeCode(Type colType) {
		Code code = colType.getCode();
		return code.equals(Code.ARRAY)
				? SpannerTypeMapper.getArrayJavaClassFor(colType.getArrayElementType().getCode())
				: SpannerTypeMapper.getSimpleJavaClassFor(code);
	}
}
