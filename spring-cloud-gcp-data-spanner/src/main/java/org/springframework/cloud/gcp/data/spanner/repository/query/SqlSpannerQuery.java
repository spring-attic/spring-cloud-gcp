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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Struct.Builder;

import org.springframework.cloud.gcp.data.spanner.core.SpannerPageableQueryOptions;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.convert.StructAccessor;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.CompositeStringExpression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;

/**
 * A Query Method for Spanner using SQL strings.
 *
 * @param <T> the return type of the Query Method
 * @author Balint Pato
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class SqlSpannerQuery<T> extends AbstractSpannerQuery<T> {

	// A character that isn't used in SQL
	private static String ENTITY_CLASS_NAME_BOOKEND = ":";
	private static Pattern ENTITY_CLASS_NAME_PATTERN = Pattern.compile(
			"\\" + ENTITY_CLASS_NAME_BOOKEND + "\\S+\\" + ENTITY_CLASS_NAME_BOOKEND);

	private final String sql;

	private final boolean isDml;

	private final Function<Object, Struct> paramStructConvertFunc = (param) -> {
		Builder builder = Struct.newBuilder();
		this.spannerTemplate.getSpannerEntityProcessor().write(param, builder::set);
		return builder.build();
	};

	private QueryMethodEvaluationContextProvider evaluationContextProvider;

	private SpelExpressionParser expressionParser;

	SqlSpannerQuery(Class<T> type, SpannerQueryMethod queryMethod,
			SpannerTemplate spannerTemplate, String sql,
			QueryMethodEvaluationContextProvider evaluationContextProvider,
			SpelExpressionParser expressionParser,
			SpannerMappingContext spannerMappingContext, boolean isDml) {
		super(type, queryMethod, spannerTemplate, spannerMappingContext);
		this.evaluationContextProvider = evaluationContextProvider;
		this.expressionParser = expressionParser;
		this.sql = StringUtils.trimTrailingCharacter(sql.trim(), ';');
		this.isDml = isDml;
	}

	private boolean isPageableOrSort(Class<?> type) {
		return Pageable.class.isAssignableFrom(type) || Sort.class.isAssignableFrom(type);
	}

	private List<String> getParamTags() {
		List<String> tags = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		Parameters<?, ?> parameters = getQueryMethod().getParameters();
		for (int i = 0; i < parameters.getNumberOfParameters(); i++) {
			Parameter param = parameters.getParameter(i);
			if (isPageableOrSort(param.getType())) {
				continue;
			}
			Optional<String> paramName = param.getName();
			if (!paramName.isPresent()) {
				throw new SpannerDataException(
						"Query method has a parameter without a valid name: "
								+ getQueryMethod().getName());
			}
			String name = paramName.get();
			if (seen.contains(name)) {
				throw new SpannerDataException(
						"More than one param has the same name: " + name);
			}
			seen.add(name);
			tags.add(name);
		}
		return tags;
	}

	private static String resolveEntityClassNames(String sql, SpannerMappingContext spannerMappingContext) {
		Matcher matcher = ENTITY_CLASS_NAME_PATTERN.matcher(sql);
		String result = sql;
		while (matcher.find()) {
			String matched = matcher.group();
			String className = matched.substring(1, matched.length() - 1);
			try {
				Class<?> entityClass = Class.forName(className);
				SpannerPersistentEntity<?> spannerPersistentEntity = spannerMappingContext
						.getPersistentEntity(entityClass);
				if (spannerPersistentEntity == null) {
					throw new SpannerDataException(
							"The class used in the SQL statement is not a Cloud Spanner persistent entity: "
									+ className);
				}
				result = result.replace(matched, spannerPersistentEntity.tableName());
			}
			catch (ClassNotFoundException ex) {
				throw new SpannerDataException(
						"The class name does not refer to an available entity type: "
								+ className);
			}
		}
		return result;
	}

	private void resolveSpELTags(QueryTagValue queryTagValue) {
		Expression[] expressions = detectExpressions(queryTagValue.sql);
		StringBuilder sb = new StringBuilder();
		Map<Object, String> valueToTag = new HashMap<>();
		int tagNum = 0;
		EvaluationContext evaluationContext = this.evaluationContextProvider
				.getEvaluationContext(this.queryMethod.getParameters(),
						queryTagValue.rawParams);
		for (Expression expression : expressions) {
			if (expression instanceof LiteralExpression) {
				sb.append(expression.getValue(String.class));
			}
			else if (expression instanceof SpelExpression) {
				Object value = expression.getValue(evaluationContext);
				if (valueToTag.containsKey(value)) {
					sb.append("@").append(valueToTag.get(value));
				}
				else {
					String newTag;
					do {
						tagNum++;
						newTag = "SpELtag" + tagNum;
					}
					while (queryTagValue.initialTags.contains(newTag));
					valueToTag.put(value, newTag);
					queryTagValue.params.add(value);
					queryTagValue.tags.add(newTag);
					sb.append("@").append(newTag);
				}
			}
			else {
				throw new SpannerDataException(
						"Unexpected expression type. SQL queries are expected to be "
								+ "concatenation of Literal and SpEL expressions.");
			}
		}
		queryTagValue.sql = sb.toString();
	}

	@Override
	public List executeRawResult(Object[] parameters) {

		ParameterAccessor paramAccessor = new ParametersParameterAccessor(getQueryMethod().getParameters(), parameters);
		Object[] params = StreamSupport.stream(paramAccessor.spliterator(), false).toArray();

		QueryTagValue queryTagValue = new QueryTagValue(getParamTags(), parameters,
						params, resolveEntityClassNames(this.sql, this.spannerMappingContext));

		resolveSpELTags(queryTagValue);

		return this.isDml
				? Collections.singletonList(
						this.spannerTemplate.executeDmlStatement(buildStatementFromQueryAndTags(queryTagValue)))
				: executeReadSql(paramAccessor.getPageable(), paramAccessor.getSort(), queryTagValue);
	}

	private List executeReadSql(Pageable pageable, Sort sort, QueryTagValue queryTagValue) {
		SpannerPageableQueryOptions spannerQueryOptions = new SpannerPageableQueryOptions()
				.setAllowPartialRead(true);

		if (sort != null && sort.isSorted()) {
			spannerQueryOptions.setSort(sort);
		}

		if (pageable != null && pageable.isPaged()) {
			spannerQueryOptions.setOffset(pageable.getOffset()).setLimit(pageable.getPageSize());
		}

		final Class<?> returnedType = getReturnedType();
		final SpannerPersistentEntity<?> entity = returnedType == null ? null : this.spannerMappingContext.getPersistentEntity(returnedType);

		queryTagValue.sql = SpannerStatementQueryExecutor
				.applySortingPagingQueryOptions(this.entityType, spannerQueryOptions,
						queryTagValue.sql, this.spannerMappingContext, entity != null && entity.hasEagerlyLoadedProperties());

		Statement statement = buildStatementFromQueryAndTags(queryTagValue);

		return (getReturnedSimpleConvertableItemType() != null)
				? this.spannerTemplate.query(
						(struct) -> new StructAccessor(struct).getSingleValue(0), statement,
						spannerQueryOptions)
				: this.spannerTemplate.query(this.entityType,
						statement,
				spannerQueryOptions);
	}

	private Statement buildStatementFromQueryAndTags(QueryTagValue queryTagValue) {
		Map<String, java.lang.reflect.Parameter> paramMetadataMap = new HashMap<>();
		for (java.lang.reflect.Parameter param : getQueryMethod().getMethod().getParameters()) {
			Param annotation = param.getAnnotation(Param.class);
			paramMetadataMap.put(annotation == null ? param.getName() : annotation.value(), param);
		}
		return SpannerStatementQueryExecutor.buildStatementFromSqlWithArgs(
				queryTagValue.sql, queryTagValue.tags,
				this.paramStructConvertFunc, this.spannerTemplate.getSpannerEntityProcessor().getWriteConverter(),
				queryTagValue.params.toArray(), paramMetadataMap);
	}

	private Expression[] detectExpressions(String sql) {
		Expression expression = this.expressionParser.parseExpression(sql,
				ParserContext.TEMPLATE_EXPRESSION);
		if (expression instanceof LiteralExpression) {
			return new Expression[] { expression };
		}
		else if (expression instanceof CompositeStringExpression) {
			return ((CompositeStringExpression) expression).getExpressions();
		}
		else {
			throw new SpannerDataException("Unexpected expression type. "
					+ "Query can either contain no SpEL expressions or have SpEL expressions in the SQL.");
		}
	}

	// Convenience class to hold a grouping of SQL, tags, and parameter values.
	private static class QueryTagValue {

		List<String> tags;

		final Set<String> initialTags;

		List<Object> params;

		final Object[] intialParams;

		final Object[] rawParams;

		String sql;

		QueryTagValue(List<String> tags, Object[] rawParams, Object[] params, String sql) {
			this.tags = tags;
			this.intialParams = params;
			this.sql = sql;
			this.initialTags = new HashSet<>(tags);
			this.params = new ArrayList<>(Arrays.asList(params));
			this.rawParams = rawParams;
		}
	}
}
