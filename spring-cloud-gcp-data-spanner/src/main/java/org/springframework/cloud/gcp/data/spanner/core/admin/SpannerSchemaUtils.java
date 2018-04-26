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

package org.springframework.cloud.gcp.data.spanner.core.admin;

import java.util.OptionalLong;
import java.util.StringJoiner;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.Type;

import org.springframework.cloud.gcp.data.spanner.core.convert.ConversionUtils;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerEntityProcessor;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerTypeMapper;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentProperty;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.util.Assert;

/**
 * Contains functions related to the table schema of entities.
 *
 * @author Chengyuan Zhao
 */
public class SpannerSchemaUtils {

	private final SpannerMappingContext mappingContext;

	private final SpannerEntityProcessor spannerEntityProcessor;

	private final SpannerTypeMapper spannerTypeMapper;

	public SpannerSchemaUtils(SpannerMappingContext mappingContext,
			SpannerEntityProcessor spannerEntityProcessor) {
		Assert.notNull(mappingContext,
				"A valid mapping context for Spanner is required.");
		Assert.notNull(spannerEntityProcessor,
				"A valid results mapper for Spanner is required.");
		this.mappingContext = mappingContext;
		this.spannerEntityProcessor = spannerEntityProcessor;
		this.spannerTypeMapper = new SpannerTypeMapper();
	}

	private String getTypeDDLString(Type.Code type, boolean isArray, OptionalLong dataLength) {
		Assert.notNull(type, "A valid Spanner column type is required.");
		if (isArray) {
			return "ARRAY<" + getTypeDDLString(type, false, dataLength) + ">";
		}
		return type.toString()
				+ (type == Type.Code.STRING || type == Type.Code.BYTES
						? "(" + (dataLength.isPresent() ? dataLength.getAsLong() : "MAX") + ")"
						: "");
	}

	public String getColumnDDLString(
			SpannerPersistentProperty spannerPersistentProperty, SpannerEntityProcessor spannerEntityProcessor) {
		Class columnType = spannerPersistentProperty.getType();
		String ddlString;
		if (ConversionUtils.isIterableNonByteArrayType(columnType)) {
			Class innerType = spannerPersistentProperty.getColumnInnerType();
			if (innerType == null) {
				throw new SpannerDataException(
						"Cannot get column DDL for iterable type without "
								+ "annotated"
								+ " "
								+ "inner "
								+ "type"
								+ ".");
			}
			Class spannerJavaType = spannerEntityProcessor.getCorrespondingSpannerJavaType(innerType, true);
			Type.Code spannerSupportedInnerType = this.spannerTypeMapper.getSimpleTypeCodeForJavaType(spannerJavaType);

			if (spannerSupportedInnerType == null) {
				throw new SpannerDataException(
						"Could not find suitable Spanner column inner type for "
								+ "property type:"
								+ innerType);
			}
			ddlString = getTypeDDLString(spannerSupportedInnerType, true,
					spannerPersistentProperty.getMaxColumnLength());
		}
		else {
			Class spannerJavaType = spannerEntityProcessor.getCorrespondingSpannerJavaType(columnType, false);
			Type.Code spannerColumnType = spannerJavaType.isArray()
					? this.spannerTypeMapper.getArrayTypeCodeForJavaType(spannerJavaType)
					: this.spannerTypeMapper.getSimpleTypeCodeForJavaType(spannerJavaType);

			if (spannerColumnType == null) {
				throw new SpannerDataException(
						"Could not find suitable Spanner column type for property " + "type:" + columnType);
			}

			ddlString = getTypeDDLString(spannerColumnType, spannerJavaType.isArray(),
					spannerPersistentProperty.getMaxColumnLength());
		}
		return spannerPersistentProperty.getColumnName() + " " + ddlString;
	}

	/**
	 * Gets the key for the given object.
	 * @param object
	 * @return
	 */
	public Key getKey(Object object) {
		SpannerPersistentEntity persistentEntity = this.mappingContext
				.getPersistentEntity(object.getClass());
		return (Key) persistentEntity.getPropertyAccessor(object)
				.getProperty(persistentEntity.getIdProperty());
	}

	/**
	 * Gets the DDL string to create the table for the given entity in Spanner. This is just
	 * one of the possible schemas that can support the given entity type. The specific schema
	 * is determined by the configured property type converters used by the read and write
	 * methods in this SpannerOperations and will be compatible with those methods.
	 * @param entityClass the entity type.
	 * @return the DDL string.
	 */
	public String getCreateTableDDLString(Class entityClass) {
		SpannerPersistentEntity spannerPersistentEntity = this.mappingContext
				.getPersistentEntity(entityClass);

		StringBuilder stringBuilder = new StringBuilder(
				"CREATE TABLE " + spannerPersistentEntity.tableName() + " ( ");

		StringJoiner columnStrings = new StringJoiner(" , ");
		spannerPersistentEntity.doWithProperties(
				(PropertyHandler<SpannerPersistentProperty>) spannerPersistentProperty -> columnStrings
						.add(getColumnDDLString(
								spannerPersistentProperty,
								this.spannerEntityProcessor)));

		stringBuilder.append(columnStrings.toString() + " ) PRIMARY KEY ( ");

		StringJoiner keyStrings = new StringJoiner(" , ");

		for (SpannerPersistentProperty keyProp : spannerPersistentEntity
				.getPrimaryKeyProperties()) {
			keyStrings.add(keyProp.getColumnName());
		}

		stringBuilder.append(keyStrings.toString() + " )");
		return stringBuilder.toString();
	}

	/**
	 * Gets the DDL string to drop the table for the given entity in Spanner.
	 * @param entityClass the entity type.
	 * @return the DDL string.
	 */
	public String getDropTableDDLString(Class entityClass) {
		return "DROP TABLE "
				+ this.mappingContext.getPersistentEntity(entityClass).tableName();
	}
}
