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
import java.util.List;
import java.util.StringJoiner;
import java.util.function.BiFunction;

import com.google.cloud.spanner.Statement;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.convert.MappingSpannerWriteConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.Pair;

/**
 * Executes Google Spanner query statements using
 * {@link org.springframework.data.repository.query.parser.PartTree} parsed method
 * definitions.
 *
 * @author Chengyuan Zhao
 * @author Balint Pato
 */
public class SpannerStatementQueryExecutor {
	/**
	 * Executes a PartTree-based query.
	 * @param type the type of the underlying entity
	 * @param tree the parsed metadata of the query
	 * @param params the parameters of this specific query
	 * @param spannerOperations used to execute the query
	 * @param spannerMappingContext used to get metadata about the entity type
	 * @return A boolean for EXISTS queries, an integer for COUNT queries, and a List of
	 * entities otherwise.
	 * @throws UnsupportedOperationException for DELETE queries.
	 */
	public static Object executeQuery(Class type, PartTree tree, Object[] params,
			SpannerOperations spannerOperations,
			SpannerMappingContext spannerMappingContext) {
		if (tree.isDelete()) {
			throw new UnsupportedOperationException(
					"Delete queries are not supported in Spanner");
		}
		Pair<String, List<String>> sqlAndTags = buildPartTreeSqlString(tree,
				spannerMappingContext, type);
		List results = spannerOperations.query(type, buildStatementFromSqlWithArgs(
				sqlAndTags.getFirst(), sqlAndTags.getSecond(), params));
		if (tree.isCountProjection()) {
			return results.size();
		}
		else if (tree.isExistsProjection()) {
			return !results.isEmpty();
		}
		else {
			return results;
		}
	}

	/**
	 * Creates a Spanner statement.
	 * @param sql the SQL string with tags.
	 * @param tags the tags that appear in the SQL string.
	 * @param params the parameters to substitute the tags. The ordering must be the same
	 * as the tags.
	 * @return an SQL statement ready to use with Spanner.
	 * @throws IllegalArgumentException if the number of tags does not match the number of
	 * params, or if a param of an unsupported type is given.
	 */
	public static Statement buildStatementFromSqlWithArgs(String sql, List<String> tags,
			Object[] params) {
		if (tags.size() != params.length) {
			throw new IllegalArgumentException(
					"The number of tags does match the number of params.");
		}
		Statement.Builder builder = Statement.newBuilder(sql);
		for (int i = 0; i < tags.size(); i++) {
			Object param = params[i];
			BiFunction toMethod = MappingSpannerWriteConverter.singleItemType2ToMethodMap
					.get(param.getClass());
			if (toMethod == null) {
				throw new IllegalArgumentException("Param: " + param.toString()
						+ " is not a supported type: " + param.getClass());
			}
			builder = (Statement.Builder) toMethod.apply(builder.bind(tags.get(i)),
					param);
		}
		return builder.build();
	}

	private static Pair<String, List<String>> buildPartTreeSqlString(PartTree tree,
			SpannerMappingContext spannerMappingContext, Class type) {
		SpannerPersistentEntity<?> persistentEntity = spannerMappingContext
				.getPersistentEntity(type);
		List<String> tags = new ArrayList<>();
		StringBuilder stringBuilder = new StringBuilder();

		buildSelect(tree, stringBuilder);
		buildFrom(persistentEntity, stringBuilder);
		buildWhere(tree, persistentEntity, tags, stringBuilder);
		buildOrderBy(persistentEntity, stringBuilder, tree.getSort());
		buildLimit(tree, stringBuilder);

		stringBuilder.append(";");
		return Pair.of(stringBuilder.toString(), tags);
	}

	private static StringBuilder buildSelect(PartTree tree, StringBuilder stringBuilder) {
		stringBuilder.append("SELECT ");
		if (tree.isDistinct()) {
			stringBuilder.append("DISTINCT ");
		}
		stringBuilder.append("* ");
		return stringBuilder;
	}

	private static void buildFrom(SpannerPersistentEntity<?> persistentEntity,
			StringBuilder stringBuilder) {
		stringBuilder.append("FROM " + persistentEntity.tableName() + " ");
	}

	public static void buildOrderBy(SpannerPersistentEntity<?> persistentEntity,
			StringBuilder stringBuilder, Sort sort) {
		if (sort.isSorted()) {
			stringBuilder.append("ORDER BY ");
			StringJoiner orderStrings = new StringJoiner(" , ");
			sort.iterator().forEachRemaining(o -> orderStrings.add(ordering(persistentEntity, o)));
			stringBuilder.append(orderStrings.toString());
		}
	}

	private static String ordering(SpannerPersistentEntity<?> persistentEntity, Order order) {
		return persistentEntity.getPersistentProperty(
				order.getProperty()).getColumnName() + " " + toString(order);
	}

	private static String toString(Order order) {
		return order.isAscending() ? "ASC" : "DESC";
	}

	private static void buildWhere(PartTree tree, SpannerPersistentEntity<?> persistentEntity,
			List<String> tags, StringBuilder stringBuilder) {
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
					String andString = persistentEntity.getPersistentProperty(segment)
							.getColumnName();

					switch (part.getType()) {
					case LIKE:
						andString += " LIKE %@" + tag;
						break;
					case SIMPLE_PROPERTY:
						andString += "=@" + tag;
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
						andString += "<@" + tag;
						break;
					case IS_NOT_NULL:
						andString += "<>NULL";
						break;
					case LESS_THAN_EQUAL:
						andString += "<=@" + tag;
						break;
					case GREATER_THAN:
						andString += ">@" + tag;
						break;
					case GREATER_THAN_EQUAL:
						andString += ">=@" + tag;
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
		if (tree.isLimiting()) {
			stringBuilder.append(" LIMIT " + tree.getMaxResults());
		}
	}
}
