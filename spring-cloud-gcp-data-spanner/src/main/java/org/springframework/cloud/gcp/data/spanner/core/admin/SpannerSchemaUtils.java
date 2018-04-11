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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import com.google.cloud.spanner.Key;

import org.springframework.cloud.gcp.data.spanner.core.convert.ConversionUtils;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentProperty;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.util.Assert;

/**
 *	Contains functions related to the table schema of entities.
 *
 * @author Chengyuan Zhao
 */
public class SpannerSchemaUtils {

	private final SpannerMappingContext mappingContext;

	private final SpannerConverter spannerConverter;

	public SpannerSchemaUtils(SpannerMappingContext mappingContext,
			SpannerConverter spannerConverter) {
		Assert.notNull(mappingContext,
				"A valid mapping context for Spanner is required.");
		Assert.notNull(spannerConverter,
				"A valid results mapper for Spanner is required.");
		this.mappingContext = mappingContext;
		this.spannerConverter = spannerConverter;
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
	 * Gets the DDL string to create the table for the given entity in Spanner. This is
	 * just one of the possible schemas that can support the given entity type. The
	 * specific schema is determined by the configured property type converters used by
	 * the read and write methods in this SpannerOperations and will be compatible with
	 * those methods.
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
				(PropertyHandler<SpannerPersistentProperty>) spannerPersistentProperty -> {
					// Child entities do not directly appear as columns, but are related
					// by their primary keys
					if (ConversionUtils
							.isSpannerTableProperty(spannerPersistentProperty)) {
						return;
					}
					columnStrings
							.add(spannerPersistentProperty.getColumnName() + " "
									+ ConversionUtils.getColumnDDLString(
											spannerPersistentProperty,
											this.spannerConverter));
				});

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
	 * Gets a list of DDL strings to create the tables rooted at the given entity class.
	 * The DDL-create strings are ordered in the list starting with the given root class
	 * and are topologically sorted.
	 * @param entityClass
	 * @return
	 */
	public List<String> getCreateTableDDLStringsForHierarchy(Class entityClass) {
		List<String> ddlStrings = new ArrayList<>();
		getCreateTableDDLStringsForHierarchy(null, entityClass, ddlStrings,
				new HashSet<>());
		return ddlStrings;
	}

	private void getCreateTableDDLStringsForHierarchy(String parentTable,
			Class entityClass, List<String> ddlStrings, Set<Class> seenClasses) {
		if (seenClasses.contains(entityClass)) {
			return;
		}
		ddlStrings.add(getCreateTableDDLString(entityClass) + (parentTable == null ? ""
				: ", INTERLEAVE IN PARENT " + parentTable + " ON DELETE CASCADE"));
		SpannerPersistentEntity spannerPersistentEntity = this.mappingContext
				.getPersistentEntity(entityClass);
		spannerPersistentEntity.doWithProperties(
				(PropertyHandler<SpannerPersistentProperty>) spannerPersistentProperty -> ConversionUtils
						.applyIfChildEntityType(spannerPersistentProperty, childType -> {
							getCreateTableDDLStringsForHierarchy(
									spannerPersistentEntity.tableName(), childType,
									ddlStrings, seenClasses);
							return null;
						}));
		seenClasses.add(entityClass);
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

	/**
	 * Gets the DDL strings to drop the tables of this entity and all of its sub-entities.
	 * The list is given in reverse topological sort, since parent tables cannot be
	 * dropped before their children tables.
	 * @param entityClass the root entity whose table to drop
	 * @return the list of drop DDL strings
	 */
	public List<String> getDropTableDDLStringsForHierarchy(Class entityClass) {
		List<String> ddlStrings = new ArrayList<>();
		getDropTableDDLStringsForHierarchy(entityClass, ddlStrings, new HashSet<>());
		return ddlStrings;
	}

	private void getDropTableDDLStringsForHierarchy(Class entityClass,
			List<String> dropStrings, Set<Class> seenClasses) {
		if (seenClasses.contains(entityClass)) {
			return;
		}
		seenClasses.add(entityClass);
		dropStrings.add(0, getDropTableDDLString(entityClass));
		SpannerPersistentEntity spannerPersistentEntity = this.mappingContext
				.getPersistentEntity(entityClass);
		spannerPersistentEntity.doWithProperties(
				(PropertyHandler<SpannerPersistentProperty>) spannerPersistentProperty -> ConversionUtils
						.applyIfChildEntityType(spannerPersistentProperty, childType -> {
							getDropTableDDLStringsForHierarchy(childType, dropStrings,
									seenClasses);
							return null;
						}));
	}
}
