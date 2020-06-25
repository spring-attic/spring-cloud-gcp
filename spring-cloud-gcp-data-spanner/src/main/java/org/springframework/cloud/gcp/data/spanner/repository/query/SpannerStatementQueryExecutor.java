/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.spanner.repository.query;

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.ValueBinder;
import org.apache.commons.lang3.StringUtils;

import org.springframework.cloud.gcp.data.spanner.core.SpannerPageableQueryOptions;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.convert.ConversionUtils;
import org.springframework.cloud.gcp.data.spanner.core.convert.ConverterAwareMappingSpannerEntityWriter;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerCustomConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentProperty;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Where;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.Part.IgnoreCaseType;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * Executes Cloud Spanner query statements using
 * {@link org.springframework.data.repository.query.parser.PartTree} parsed method
 * definitions.
 *
 * @author Chengyuan Zhao
 * @author Balint Pato
 * @author Mike Eltsufin
 * @author Roman Solodovnichenko
 * @author Dmitry Solomakha
 *
 * @since 1.1
 */
public final class SpannerStatementQueryExecutor {

	private SpannerStatementQueryExecutor() {
	}

	/**
	 * Executes a PartTree-based query.
	 * @param type the type of the underlying entity
	 * @param tree the parsed metadata of the query
	 * @param parameterAccessor the parameters of this specific query
	 * @param queryMethodParamsMetadata parameter metadata from Query Method
	 * @param spannerTemplate used to execute the query
	 * @param spannerMappingContext used to get metadata about the entity type
	 * @param <T> the type of the underlying entity
	 * @return list of entities.
	 */
	public static <T> List<T> executeQuery(Class<T> type, PartTree tree, ParameterAccessor parameterAccessor,
			Parameter[] queryMethodParamsMetadata,
			SpannerTemplate spannerTemplate,
			SpannerMappingContext spannerMappingContext) {
		SqlStringAndPlaceholders sqlStringAndPlaceholders = buildPartTreeSqlString(tree, spannerMappingContext, type, parameterAccessor);
		Map<String, Parameter> paramMetadataMap = preparePartTreeSqlTagParameterMap(queryMethodParamsMetadata,
				sqlStringAndPlaceholders);
		Object[] params = StreamSupport.stream(parameterAccessor.spliterator(), false).toArray();
		return spannerTemplate.query(type, buildStatementFromSqlWithArgs(
				sqlStringAndPlaceholders.getSql(), sqlStringAndPlaceholders.getPlaceholders(), null,
				spannerTemplate.getSpannerEntityProcessor().getWriteConverter(), params, paramMetadataMap), null);
	}

	private static Map<String, Parameter> preparePartTreeSqlTagParameterMap(Parameter[] paramsMetadata,
			SqlStringAndPlaceholders sqlStringAndPlaceholders) {
		Map<String, Parameter> paramMetadataMap = new HashMap<>();
		for (int i = 0; i < paramsMetadata.length; i++) {
			Parameter param = paramsMetadata[i];
			//Skip Pageable and Sort parameters because they don't need to be bound to the tags in the query.
			//They are processed separately in applySort and buildLimit methods.
			if (param.getType() != Pageable.class && param.getType() != Sort.class) {
				paramMetadataMap.put(sqlStringAndPlaceholders.getPlaceholders().get(i), param);
			}
		}
		return paramMetadataMap;
	}

	/**
	 * Executes a PartTree-based query and applies a custom row-mapping function to the
	 * result.
	 * @param rowFunc the function to apply to each row of the result.
	 * @param type the type of the underlying entity
	 * @param tree the parsed metadata of the query
	 * @param parameterAccessor the parameters of this specific query
	 * @param queryMethodParamsMetadata parameter metadata from Query Method
	 * @param spannerTemplate used to execute the query
	 * @param spannerMappingContext used to get metadata about the entity type
	 * @param <A> the type to which to convert Struct params
	 * @param <T> the type of the underlying entity on which to query
	 * @return list of objects mapped using the given function.
	 */
	public static <A, T> List<A> executeQuery(Function<Struct, A> rowFunc, Class<T> type,
			PartTree tree, ParameterAccessor parameterAccessor, Parameter[] queryMethodParamsMetadata, SpannerTemplate spannerTemplate,
			SpannerMappingContext spannerMappingContext) {
		SqlStringAndPlaceholders sqlStringAndPlaceholders = buildPartTreeSqlString(tree,
				spannerMappingContext, type, parameterAccessor);
		Map<String, Parameter> paramMetadataMap = preparePartTreeSqlTagParameterMap(queryMethodParamsMetadata,
				sqlStringAndPlaceholders);
		Object[] params = StreamSupport.stream(parameterAccessor.spliterator(), false).toArray();
		return spannerTemplate.query(rowFunc, buildStatementFromSqlWithArgs(
				sqlStringAndPlaceholders.getSql(), sqlStringAndPlaceholders.getPlaceholders(), null,
				spannerTemplate.getSpannerEntityProcessor().getWriteConverter(), params, paramMetadataMap), null);
	}

	/**
	 * Apply paging and sorting options to a query string.
	 * @param entityClass the domain type whose table is being queried.
	 * @param options query options containing the sorting and paging options
	 * @param sql the sql that will be wrapped with sorting and paging options.
	 * @param mappingContext a mapping context to convert between Cloud Spanner column names
	 *     and underlying property names.
	 * @param fetchInterleaved when {@code true} additional subqueries will be added
	 *     to fetch eager-Interleaved lists with a single query.
	 *     Please note, it doesn't make sense to pass it as {@code true} when the {@code sql} already contains
	 *     a complete lists of all eager-Interleaved properties generated by the method {@link #getColumnsStringForSelect}.
	 * @param <T> the domain type.
	 * @return the final SQL string with paging and sorting applied.
	 * @see #getColumnsStringForSelect
	 */
	public static <T> String applySortingPagingQueryOptions(Class<T> entityClass,
			SpannerPageableQueryOptions options, String sql,
			SpannerMappingContext mappingContext, boolean fetchInterleaved) {
		SpannerPersistentEntity<?> persistentEntity = mappingContext
				.getPersistentEntity(entityClass);

		// Cloud Spanner does not preserve the order of derived tables so we must not wrap the
		// derived table
		// in SELECT * FROM () if there is no overriding pageable param.
		if ((options.getSort() == null || options.getSort().isUnsorted()) && options.getLimit() == null
				&& options.getOffset() == null && !fetchInterleaved) {
			return sql;
		}
		final String subquery = fetchInterleaved ? getChildrenSubquery(persistentEntity, mappingContext) : "";
		final String alias = subquery.isEmpty() ? "" : " " + persistentEntity.tableName();
		StringBuilder sb = applySort(options.getSort(),
				new StringBuilder("SELECT *").append(subquery)
						.append(" FROM (").append(sql).append(")").append(alias)
						.append(buildWhere(persistentEntity)), persistentEntity);
		if (options.getLimit() != null) {
			sb.append(" LIMIT ").append(options.getLimit());
		}
		if (options.getOffset() != null) {
			sb.append(" OFFSET ").append(options.getOffset());
		}
		return sb.toString();
	}

	/**
	 * Builds an SQL where clause for the persistent entity.
	 * @param entity the persistent entity instance
	 * @return SQL where clause for the persistent entity instance.
	 * @see SpannerPersistentEntity#hasWhere()
	 * @see SpannerPersistentEntity#getWhere()
	 */
	public static String buildWhere(SpannerPersistentEntity<?> entity) {
		return entity.hasWhere() ? " WHERE " + entity.getWhere() : "";
	}

	/**
	 * Gets a {@link Statement} that returns the rows associated with a parent entity. This function is
	 * intended to be used with parent-child interleaved tables, so that the retrieval of all
	 * child rows having the parent's key values is efficient.
	 * @param parentKey the parent key whose children to get.
	 * @param spannerPersistentProperty the property with interleaved list of child entries in the parent entity.
	 * @param writeConverter a converter to convert key values as needed to bind to the query
	 *     statement.
	 * @param mappingContext mapping context
	 * @return the Spanner statement to perform the retrieval.
	 */
	public static Statement getChildrenRowsQuery(Key parentKey,
			SpannerPersistentProperty spannerPersistentProperty, SpannerCustomConverter writeConverter,
			SpannerMappingContext mappingContext) {
		Class<?> childType = spannerPersistentProperty.getColumnInnerType();
		SpannerPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(childType);
		String whereClause = getWhere(spannerPersistentProperty, persistentEntity);
		return buildQuery(KeySet.singleKey(parentKey), persistentEntity, writeConverter, mappingContext, whereClause);
	}

	/**
	 * Builds a query that returns the rows associated with a key set.
	 * If the entity class has {@link org.springframework.cloud.gcp.data.spanner.core.mapping.Where}
	 * annotation it will be used to build the query.
	 * @param keySet the key set whose members to get.
	 * @param persistentEntity the persistent entity of the table.
	 * @param <T> the type of the persistent entity
	 * @param writeConverter a converter to convert key values as needed to bind to the query statement.
	 * @param mappingContext mapping context
	 * @return the Spanner statement to perform the retrieval.
	 */
	public static <T> Statement buildQuery(KeySet keySet,
			SpannerPersistentEntity<T> persistentEntity, SpannerCustomConverter writeConverter,
			SpannerMappingContext mappingContext) {
		return buildQuery(keySet, persistentEntity, writeConverter, mappingContext, persistentEntity.getWhere());
	}

	/**
	 * Builds a query that returns the rows associated with a key set with additional SQL-where.
	 * The {@link org.springframework.cloud.gcp.data.spanner.core.mapping.Where} of the {@code persistentEntity} parameter
	 * is ignored, you should pass the SQL-where as a {@code whereClause} parameter.
	 * @param keySet the key set whose members to get.
	 * @param persistentEntity the persistent entity of the table.
	 * @param <T> the type of the persistent entity
	 * @param writeConverter a converter to convert key values as needed to bind to the query statement.
	 * @param mappingContext mapping context
	 * @param whereClause SQL where clause
	 * @return the Spanner statement to perform the retrieval.
	 */
	public static <T> Statement buildQuery(KeySet keySet,
			SpannerPersistentEntity<T> persistentEntity, SpannerCustomConverter writeConverter,
			SpannerMappingContext mappingContext, String whereClause) {
		return buildQuery(keySet, persistentEntity, writeConverter, mappingContext, whereClause, null);
	}

	/**
	 * Builds a query that returns the rows associated with a key set with additional SQL-where.
	 * The {@link org.springframework.cloud.gcp.data.spanner.core.mapping.Where} of the {@code persistentEntity} parameter
	 * is ignored, you should pass the SQL-where as a {@code whereClause} parameter.
	 * The secondary {@code index} will be used instead of the table name when the corresponding parameter is not null.
	 * @param keySet the key set whose members to get.
	 * @param persistentEntity the persistent entity of the table.
	 * @param <T> the type of the persistent entity
	 * @param writeConverter a converter to convert key values as needed to bind to the query statement.
	 * @param mappingContext mapping context
	 * @param whereClause SQL where clause
	 * @param index the secondary index name
	 * @return the Spanner statement to perform the retrieval.
	 */
	public static <T> Statement buildQuery(KeySet keySet,
			SpannerPersistentEntity<T> persistentEntity, SpannerCustomConverter writeConverter,
			SpannerMappingContext mappingContext, String whereClause, String index) {
		List<String> orParts = new ArrayList<>();
		List<String> tags = new ArrayList<>();
		List keyParts = new ArrayList();
		int tagNum = 0;
		List<SpannerPersistentProperty> keyProperties = persistentEntity.getFlattenedPrimaryKeyProperties();

		for (Key key : keySet.getKeys()) {
			StringJoiner andJoiner = new StringJoiner(" AND ");
			Iterator parentKeyParts = key.getParts().iterator();
			while (parentKeyParts.hasNext()) {
				SpannerPersistentProperty keyProp = keyProperties.get(tagNum % keyProperties.size());
				String tagName = "tag" + tagNum;
				andJoiner.add(keyProp.getColumnName() + " = @" + tagName);
				tags.add(tagName);
				keyParts.add(parentKeyParts.next());
				tagNum++;
			}
			orParts.add(andJoiner.toString());
		}
		String keyClause = orParts.stream().map(s -> "(" + s + ")").collect(Collectors.joining(" OR "));
		String condition = combineWithAnd(keyClause, whereClause);
		String sb = "SELECT " + getColumnsStringForSelect(persistentEntity, mappingContext, true) + " FROM "
				+ (StringUtils.isEmpty(index) ? persistentEntity.tableName() : String.format("%s@{FORCE_INDEX=%s}", persistentEntity.tableName(), index))
				+ (condition.isEmpty() ? "" : " WHERE " + condition);
		return buildStatementFromSqlWithArgs(sb, tags, null, writeConverter,
				keyParts.toArray(), null);
	}

	private static <C, P> String getChildrenStructsQuery(
			SpannerPersistentEntity<C> childPersistentEntity,
			SpannerPersistentEntity<P> parentPersistentEntity, SpannerMappingContext mappingContext,
			String columnName, String whereClause) {
		String tableName = childPersistentEntity.tableName();
		List<SpannerPersistentProperty> parentKeyProperties = parentPersistentEntity
				.getFlattenedPrimaryKeyProperties();
		String keylCause = parentKeyProperties.stream()
				.map(keyProp -> tableName + "." + keyProp.getColumnName()
						+ " = "
						+ parentPersistentEntity.tableName() + "." + keyProp.getColumnName())
				.collect(Collectors.joining(" AND "));
		String condition = combineWithAnd(keylCause, whereClause);
		return "ARRAY (SELECT AS STRUCT " + getColumnsStringForSelect(childPersistentEntity, mappingContext, true) + " FROM "
				+ tableName + " WHERE " + condition + ") AS " + columnName;
	}

	/**
	 * Combines two SQL conditions with {@code AND} or returns only one when another is a null or empty string.
	 * The method never return null but an empty string instead.
	 * @param cond1 the first SQL condition
	 * @param cond2 the second SQL condition
	 * @return the combination of SQL conditions joined by {@code AND}.
	 */
	private static String combineWithAnd(String cond1, String cond2) {
		if (StringUtils.isEmpty(cond1)) {
			return StringUtils.defaultIfEmpty(cond2, StringUtils.EMPTY);
		}
		if (StringUtils.isEmpty(cond2)) {
			return StringUtils.defaultIfEmpty(cond1, StringUtils.EMPTY);
		}
		return "(" + cond1 + ") AND (" + cond2 + ")";
	}

	/**
	 * Creates a Cloud Spanner statement.
	 * @param sql the SQL string with tags.
	 * @param tags the tags that appear in the SQL string.
	 * @param paramStructConvertFunc a function to use to convert params to {@link Struct}
	 *     objects if they cannot be directly mapped to Cloud Spanner supported param types.
	 *     If null then this last-attempt conversion is skipped.
	 * @param spannerCustomConverter a converter used to convert params that aren't Cloud
	 *     Spanner native types. if {@code null} then this conversion is not attempted.
	 * @param params the parameters to substitute the tags. The ordering must be the same as
	 *     the tags.
	 * @param queryMethodParams the parameter metadata from Query Method if available.
	 * @return an SQL statement ready to use with Spanner.
	 * @throws IllegalArgumentException if the number of tags does not match the number of
	 *     params, or if a param of an unsupported type is given.
	 */
	public static Statement buildStatementFromSqlWithArgs(String sql, List<String> tags,
			Function<Object, Struct> paramStructConvertFunc, SpannerCustomConverter spannerCustomConverter,
			Object[] params, Map<String, Parameter> queryMethodParams) {
		if (tags == null && params == null) {
			return Statement.of(sql);
		}
		if (tags == null || params == null || tags.size() != params.length) {
			throw new IllegalArgumentException(
					"The number of tags does not match the number of params.");
		}
		Statement.Builder builder = Statement.newBuilder(sql);
		for (int i = 0; i < tags.size(); i++) {
			bindParameter(builder.bind(tags.get(i)), paramStructConvertFunc, spannerCustomConverter,
					params[i], queryMethodParams == null ? null : queryMethodParams.get(tags.get(i)));
		}
		return builder.build();
	}

	@SuppressWarnings("unchecked")
	private static void bindParameter(ValueBinder<Statement.Builder> bind,
			Function<Object, Struct> paramStructConvertFunc, SpannerCustomConverter spannerCustomConverter,
			Object originalParam, Parameter paramMetadata) {

		// Gets the type of the bind parameter; if null then infer the type from the parameter metadata.
		Class propType = originalParam != null ? originalParam.getClass() : paramMetadata.getType();
		if (ConversionUtils.isIterableNonByteArrayType(propType)) {
			if (!ConverterAwareMappingSpannerEntityWriter.attemptSetIterableValueOnBinder((Iterable) originalParam,
					bind, spannerCustomConverter, (Class) ((ParameterizedType) paramMetadata.getParameterizedType())
							.getActualTypeArguments()[0])) {
				throw new IllegalArgumentException(
						"Could not convert to an ARRAY of compatible type: " + paramMetadata);
			}
			return;
		}
		if (!ConverterAwareMappingSpannerEntityWriter.attemptBindSingleValue(originalParam, propType,
				bind, spannerCustomConverter)) {
			if (paramStructConvertFunc == null) {
				throw new IllegalArgumentException("Param: " + originalParam
						+ " is not a supported type: " + propType);
			}
			try {
				Object unused = ((BiFunction<ValueBinder, Object, Struct>)
						ConverterAwareMappingSpannerEntityWriter.singleItemTypeValueBinderMethodMap
						.get(Struct.class)).apply(bind, paramStructConvertFunc.apply(originalParam));
			}
			catch (SpannerDataException ex) {
				throw new IllegalArgumentException("Param: " + originalParam
						+ " is not a supported type: " + propType, ex);
			}
		}
	}

	public static String getColumnsStringForSelect(SpannerPersistentEntity<?> spannerPersistentEntity,
			SpannerMappingContext mappingContext, boolean fetchInterleaved) {
		final String sql = String.join(", ", spannerPersistentEntity.columns());
		return fetchInterleaved ? sql + getChildrenSubquery(spannerPersistentEntity, mappingContext) : sql;
	}

	/**
	 * Returns the value of the {@link Where} annotation of the Property or Persistent Entity.
	 * When the {@link Where} is used on the Property it has higher priority then from Persistent Entity and overrides it.
	 * @param spannerPersistentProperty the Persistent Property.
	 * @param childPersistentEntity the Entity of the Persistent Property.
	 * @return the value of the {@link Where} annotation of the Property or Persistent Entity
	 */
	private static String getWhere(SpannerPersistentProperty spannerPersistentProperty, SpannerPersistentEntity<?> childPersistentEntity) {
		return spannerPersistentProperty.hasWhere()
				?  spannerPersistentProperty.getWhere() : childPersistentEntity.getWhere();
	}

	private static String getChildrenSubquery(
			SpannerPersistentEntity<?> spannerPersistentEntity, SpannerMappingContext mappingContext) {
		StringJoiner joiner = new StringJoiner(", ", ", ", "").setEmptyValue("");
		spannerPersistentEntity.doWithInterleavedProperties(spannerPersistentProperty -> {
			if (spannerPersistentProperty.isEagerInterleaved()) {
				Class<?> childType = spannerPersistentProperty.getColumnInnerType();
				SpannerPersistentEntity<?> childPersistentEntity = mappingContext.getPersistentEntity(childType);
				joiner.add(getChildrenStructsQuery(
						childPersistentEntity, spannerPersistentEntity, mappingContext, spannerPersistentProperty.getColumnName(),
						getWhere(spannerPersistentProperty, childPersistentEntity))
				);
			}
		});
		return joiner.toString();
	}

	private static SqlStringAndPlaceholders buildPartTreeSqlString(PartTree tree,
			SpannerMappingContext spannerMappingContext, Class type, ParameterAccessor params) {

		SpannerPersistentEntity<?> persistentEntity = spannerMappingContext
				.getPersistentEntity(type);
		List<String> tags = new ArrayList<>();
		StringBuilder stringBuilder = new StringBuilder();

		buildSelect(persistentEntity, tree, stringBuilder, spannerMappingContext);
		buildFrom(persistentEntity, stringBuilder);
		buildWhere(tree, persistentEntity, tags, stringBuilder);
		applySort(params.getSort().isSorted() ? params.getSort() : tree.getSort(), stringBuilder, persistentEntity);
		buildLimit(tree, stringBuilder, params.getPageable());

		String selectSql = stringBuilder.toString();

		String finalSql = selectSql;

		if (tree.isCountProjection()) {
			finalSql = "SELECT COUNT(1) FROM (" + selectSql + ")";
		}
		else if (tree.isExistsProjection()) {
			finalSql = "SELECT EXISTS(" + selectSql + ")";
		}
		return new SqlStringAndPlaceholders(finalSql, tags);
	}

	private static void buildSelect(
			SpannerPersistentEntity<?> spannerPersistentEntity, PartTree tree,
			StringBuilder stringBuilder, SpannerMappingContext mappingContext) {
		stringBuilder.append("SELECT ").append(tree.isDistinct() ? "DISTINCT " : "")
				.append(getColumnsStringForSelect(spannerPersistentEntity, mappingContext,
						!(tree.isExistsProjection() || tree.isCountProjection()))).append(" ");
	}

	private static void buildFrom(SpannerPersistentEntity<?> persistentEntity,
			StringBuilder stringBuilder) {
		stringBuilder.append("FROM ").append(persistentEntity.tableName()).append(" ");
	}

	public static StringBuilder applySort(Sort sort, StringBuilder sql,
			SpannerPersistentEntity<?> persistentEntity) {
		if (sort == null || sort.isUnsorted()) {
			return sql;
		}
		sql.append(" ORDER BY ");
		StringJoiner sj = new StringJoiner(" , ");
		sort.iterator().forEachRemaining((o) -> {
			SpannerPersistentProperty property = persistentEntity.getPersistentProperty(o.getProperty());
			String sortedPropertyName = (property != null) ? property.getColumnName() : o.getProperty();
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

			tree.iterator().forEachRemaining((orPart) -> {
				String orString = "( ";

				StringJoiner andStrings = new StringJoiner(" AND ");

				orPart.forEach((part) -> {
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
					case IN:
						andString += " IN UNNEST(" + insertedTag + ")";
						break;
					case NOT_IN:
						andString += " NOT IN UNNEST(" + insertedTag + ")";
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
			stringBuilder.append(
					combineWithAnd(orStrings.toString(), persistentEntity.getWhere()));
		}
	}

	private static void buildLimit(PartTree tree, StringBuilder stringBuilder, Pageable pageable) {
		if (tree.isExistsProjection()) {
			stringBuilder.append(" LIMIT 1");
		}
		else if (pageable.isPaged()) {
			stringBuilder.append(" LIMIT ").append(pageable.getPageSize())
					.append(" OFFSET ").append(pageable.getOffset());
		}
		else if (tree.isLimiting()) {
			stringBuilder.append(" LIMIT ").append(tree.getMaxResults());
		}
	}
}
