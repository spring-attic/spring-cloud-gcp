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

package org.springframework.cloud.gcp.data.spanner.repository.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.ValueBinder;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.SpannerPageableQueryOptions;
import org.springframework.cloud.gcp.data.spanner.core.convert.ConverterAwareMappingSpannerEntityWriter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentProperty;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.repository.query.parser.Part.IgnoreCaseType;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.Pair;

/**
 * Executes Cloud Spanner query statements using
 * {@link org.springframework.data.repository.query.parser.PartTree} parsed method
 * definitions.
 *
 * @author Chengyuan Zhao
 * @author Balint Pato
 * @author Mike Eltsufin
 *
 * @since 1.1
 */
public class SpannerStatementQueryExecutor {
	/**
	 * Executes a PartTree-based query.
	 * @param type the type of the underlying entity
	 * @param tree the parsed metadata of the query
	 * @param params the parameters of this specific query
	 * @param spannerOperations used to execute the query
	 * @param spannerMappingContext used to get metadata about the entity type
	 * @return List of entities.
	 */
	public static <T> List<T> executeQuery(Class<T> type, PartTree tree, Object[] params,
			SpannerOperations spannerOperations,
			SpannerMappingContext spannerMappingContext) {
		Pair<String, List<String>> sqlAndTags = buildPartTreeSqlString(tree,
				spannerMappingContext, type);
		return spannerOperations.query(type, buildStatementFromSqlWithArgs(
				sqlAndTags.getFirst(), sqlAndTags.getSecond(), null, params), null);
	}

	/**
	 * Executes a PartTree-based query and applies a custom row-mapping function to the
	 * result.
	 * @param rowFunc the function to apply to each row of the result.
	 * @param type the type of the underlying entity
	 * @param tree the parsed metadata of the query
	 * @param params the parameters of this specific query
	 * @param spannerOperations used to execute the query
	 * @param spannerMappingContext used to get metadata about the entity type
	 * @return List of objects mapped using the given function.
	 */
	public static <A, T> List<A> executeQuery(Function<Struct, A> rowFunc, Class<T> type,
			PartTree tree, Object[] params, SpannerOperations spannerOperations,
			SpannerMappingContext spannerMappingContext) {
		Pair<String, List<String>> sqlAndTags = buildPartTreeSqlString(tree,
				spannerMappingContext, type);
		return spannerOperations.query(rowFunc, buildStatementFromSqlWithArgs(
				sqlAndTags.getFirst(), sqlAndTags.getSecond(), null, params), null);
	}

	public static <T> String applySortingPagingQueryOptions(Class<T> entityClass,
			SpannerPageableQueryOptions options, String sql,
			SpannerMappingContext mappingContext) {
		SpannerPersistentEntity<?> persistentEntity = mappingContext
				.getPersistentEntity(entityClass);
		StringBuilder sb = SpannerStatementQueryExecutor.applySort(options.getSort(),
				new StringBuilder("SELECT * FROM (").append(sql).append(")"), o -> {
					SpannerPersistentProperty property = persistentEntity
							.getPersistentProperty(o.getProperty());
					return property == null ? o.getProperty() : property.getColumnName();
				});
		if (options.getLimit() != null) {
			sb.append(" LIMIT ").append(options.getLimit());
		}
		if (options.getOffset() != null) {
			sb.append(" OFFSET ").append(options.getOffset());
		}
		return sb.toString();
	}

	/**
	 * Gets a query that returns the rows associated with a parent entity. This function
	 * is intended to be used with parent-child interleaved tables, so that the retrieval
	 * of all child rows having the parent's key values is efficient.
	 * @param parentKey the parent key whose children to get.
	 * @param childPersistentEntity the persistent entity of the child table.
	 * @return the Spanner statement to perform the retrieval.
	 */
	public static <T> Statement getChildrenRowsQuery(Key parentKey,
			SpannerPersistentEntity<T> childPersistentEntity) {
		StringBuilder sb = new StringBuilder(
				"SELECT " + getColumnsStringForSelect(childPersistentEntity) + " FROM "
						+ childPersistentEntity.tableName() + " WHERE ");
		StringJoiner sj = new StringJoiner(" and ");
		List<String> tags = new ArrayList<>();
		List keyParts = new ArrayList();
		int tagNum = 0;
		List<SpannerPersistentProperty> childKeyProperties = childPersistentEntity
				.getFlattenedPrimaryKeyProperties();
		Iterator parentKeyParts = parentKey.getParts().iterator();
		while (parentKeyParts.hasNext()) {
			SpannerPersistentProperty keyProp = childKeyProperties.get(tagNum);
			String tagName = "tag" + tagNum;
			sj.add(keyProp.getColumnName() + " = @" + tagName);
			tags.add(tagName);
			keyParts.add(parentKeyParts.next());
			tagNum++;
		}
		return buildStatementFromSqlWithArgs(sb.toString() + sj.toString(), tags, null,
				keyParts.toArray());
	}

	/**
	 * Creates a Cloud Spanner statement.
	 * @param sql the SQL string with tags.
	 * @param tags the tags that appear in the SQL string.
	 * @param paramStructConvertFunc a function to use to convert params to {@link Struct}
	 * objects if they cannot be directly mapped to Cloud Spanner supported param types.
	 * If null then this last-attempt conversion is skipped.
	 * @param params the parameters to substitute the tags. The ordering must be the same
	 * as the tags.
	 * @return an SQL statement ready to use with Spanner.
	 * @throws IllegalArgumentException if the number of tags does not match the number of
	 * params, or if a param of an unsupported type is given.
	 */
	@SuppressWarnings("unchecked")
	public static Statement buildStatementFromSqlWithArgs(String sql, List<String> tags,
			Function<Object, Struct> paramStructConvertFunc, Object[] params) {
		if (tags == null && params == null) {
			return Statement.of(sql);
		}
		if (tags == null || params == null || tags.size() != params.length) {
			throw new IllegalArgumentException(
					"The number of tags does match the number of params.");
		}
		Statement.Builder builder = Statement.newBuilder(sql);
		for (int i = 0; i < tags.size(); i++) {
			Object param = params[i];
			// @formatter:off
			BiFunction<ValueBinder, Object, ?> toMethod = (BiFunction<ValueBinder, Object, ?>)
					ConverterAwareMappingSpannerEntityWriter.singleItemType2ToMethodMap
					.get(Struct.class.isAssignableFrom(param.getClass()) ? Struct.class : param.getClass());
			// @formatter:on
			if (toMethod == null) {
				// try to convert the param object into a Struct
				if (paramStructConvertFunc == null) {
					throw new IllegalArgumentException("Param: " + param.toString()
							+ " is not a supported type: " + param.getClass());
				}
				try {
					// @formatter:off
					toMethod = (BiFunction<ValueBinder, Object, ?>)
							ConverterAwareMappingSpannerEntityWriter.singleItemType2ToMethodMap
							.get(Struct.class);
					// @formatter:on
					param = paramStructConvertFunc.apply(param);
				}
				catch (SpannerDataException e) {
					throw new IllegalArgumentException("Param: " + param.toString()
							+ " is not a supported type: " + param.getClass(), e);
				}
			}
			builder = (Statement.Builder) toMethod.apply(builder.bind(tags.get(i)),
					param);
		}
		return builder.build();
	}

	public static String getColumnsStringForSelect(
			SpannerPersistentEntity spannerPersistentEntity) {
		return String.join(" , ", spannerPersistentEntity.columns());
	}

	private static Pair<String, List<String>> buildPartTreeSqlString(PartTree tree,
			SpannerMappingContext spannerMappingContext, Class type) {

		SpannerPersistentEntity<?> persistentEntity = spannerMappingContext
				.getPersistentEntity(type);
		List<String> tags = new ArrayList<>();
		StringBuilder stringBuilder = new StringBuilder();

		buildSelect(persistentEntity, tree, stringBuilder);
		buildFrom(persistentEntity, stringBuilder);
		buildWhere(tree, persistentEntity, tags, stringBuilder);
		applySort(tree.getSort(), stringBuilder, o -> persistentEntity
				.getPersistentProperty(o.getProperty()).getColumnName());
		buildLimit(tree, stringBuilder);

		String selectSql = stringBuilder.toString();

		String finalSql = selectSql;

		if (tree.isCountProjection()) {
			finalSql = "SELECT COUNT(1) FROM (" + selectSql + ")";
		}
		else if (tree.isExistsProjection()) {
			finalSql = "SELECT EXISTS(" + selectSql + ")";
		}
		return Pair.of(finalSql, tags);
	}

	private static StringBuilder buildSelect(
			SpannerPersistentEntity spannerPersistentEntity, PartTree tree,
			StringBuilder stringBuilder) {
		stringBuilder.append("SELECT " + (tree.isDistinct() ? "DISTINCT " : "")
				+ getColumnsStringForSelect(spannerPersistentEntity) + " ");
		return stringBuilder;
	}

	private static void buildFrom(SpannerPersistentEntity<?> persistentEntity,
			StringBuilder stringBuilder) {
		stringBuilder.append("FROM " + persistentEntity.tableName() + " ");
	}

	public static StringBuilder applySort(Sort sort, StringBuilder sql,
			Function<Order, String> sortedPropertyNameFunction) {
		if (sort == null || sort.isUnsorted()) {
			return sql;
		}
		sql.append(" ORDER BY ");
		StringJoiner sj = new StringJoiner(" , ");
		sort.iterator().forEachRemaining(o -> {
			String sortedPropertyName = sortedPropertyNameFunction.apply(o);
			String sortedProperty = o.isIgnoreCase() ? "LOWER(" + sortedPropertyName + ")"
					: sortedPropertyName;
			sj.add(sortedProperty + (o.isAscending() ? " ASC" : " DESC"));
		});
		return sql.append(sj);
	}

	private static void buildWhere(PartTree tree,
			SpannerPersistentEntity<?> persistentEntity, List<String> tags,
			StringBuilder stringBuilder) {
		if (tree.hasPredicate()) {
			stringBuilder.append("WHERE ");

			StringJoiner orStrings = new StringJoiner(" OR ");

			tree.iterator().forEachRemaining(orPart -> {
				String orString = "( ";

				StringJoiner andStrings = new StringJoiner(" AND ");

				orPart.forEach(part -> {
					String segment = part.getProperty().getSegment();
					String tag = "tag" + tags.size();
					tags.add(tag);

					SpannerPersistentProperty spannerPersistentProperty = persistentEntity
							.getPersistentProperty(segment);

					if (spannerPersistentProperty.isEmbedded()) {
						throw new SpannerDataException(
								"Embedded class properties are not currently supported in query method names: "
										+ segment);
					}

					String andString = spannerPersistentProperty.getColumnName();
					String insertedTag = "@" + tag;
					if (part.shouldIgnoreCase() == IgnoreCaseType.ALWAYS) {
						andString = "LOWER(" + andString + ")";
						insertedTag = "LOWER(" + insertedTag + ")";
					}
					else if (part.shouldIgnoreCase() != IgnoreCaseType.NEVER) {
						throw new SpannerDataException(
								"Only ignore-case types ALWAYS and NEVER are supported, "
										+ "because the underlying table schema is not retrieved at query time to"
										+ " check that the column is the STRING or BYTES Cloud Spanner "
										+ " type supported for ignoring case.");
					}

					switch (part.getType()) {
					case LIKE:
						andString += " LIKE " + insertedTag;
						break;
					case NOT_LIKE:
						andString += " NOT LIKE " + insertedTag;
						break;
					case CONTAINING:
						andString = " REGEXP_CONTAINS(" + andString + "," + insertedTag
								+ ") =TRUE";
						break;
					case NOT_CONTAINING:
						andString = " REGEXP_CONTAINS(" + andString + "," + insertedTag
								+ ") =FALSE";
						break;
					case SIMPLE_PROPERTY:
						andString += "=" + insertedTag;
						break;
					case TRUE:
						andString += "=TRUE";
						break;
					case FALSE:
						andString += "=FALSE";
						break;
					case IS_NULL:
						andString += "=NULL";
						break;
					case LESS_THAN:
						andString += "<" + insertedTag;
						break;
					case IS_NOT_NULL:
						andString += "<>NULL";
						break;
					case LESS_THAN_EQUAL:
						andString += "<=" + insertedTag;
						break;
					case GREATER_THAN:
						andString += ">" + insertedTag;
						break;
					case GREATER_THAN_EQUAL:
						andString += ">=" + insertedTag;
						break;
					default:
						throw new UnsupportedOperationException("The statement type: "
								+ part.getType() + " is not supported.");
					}

					andStrings.add(andString);
				});

				orString += andStrings.toString();
				orString += " )";
				orStrings.add(orString);
			});

			stringBuilder.append(orStrings.toString());
		}
	}

	private static void buildLimit(PartTree tree, StringBuilder stringBuilder) {
		if (tree.isExistsProjection()) {
			stringBuilder.append(" LIMIT 1");
		}
		else if (tree.isLimiting()) {
			stringBuilder.append(" LIMIT " + tree.getMaxResults());
		}
	}
}
