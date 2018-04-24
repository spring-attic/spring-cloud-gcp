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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.SpannerQueryOptions;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.CompositeStringExpression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
public class SqlSpannerQuery extends AbstractSpannerQuery {

	// A character that isn't used in SQL
	private static String ENTITY_CLASS_NAME_BOOKEND = ":";

	private final String sql;

	private EvaluationContextProvider evaluationContextProvider;

	private SpelExpressionParser expressionParser;

	public SqlSpannerQuery(Class type, QueryMethod queryMethod,
			SpannerOperations spannerOperations, String sql,
			EvaluationContextProvider evaluationContextProvider,
			SpelExpressionParser expressionParser,
			SpannerMappingContext spannerMappingContext) {
		super(type, queryMethod, spannerOperations, spannerMappingContext);
		this.evaluationContextProvider = evaluationContextProvider;
		this.expressionParser = expressionParser;
		this.sql = sql;
	}

	private List<String> getParamTags() {
		List<String> tags = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		Parameters parameters = getQueryMethod().getParameters();
		for (int i = 0; i < parameters.getNumberOfParameters(); i++) {
			Optional<String> paramName = parameters.getParameter(i).getName();
			if (!paramName.isPresent()) {
				throw new SpannerDataException(
						"Query method has a parameter without a valid name: "
								+ getQueryMethod().getName());
			}
			String name = paramName.get();
			if (seen.contains(name)) {
				throw new SpannerDataException(
						"More than one param has same name: " + name);
			}
			seen.add(name);
			tags.add(name);
		}
		return tags;
	}

	private String resolveEntityClassNames(String sql) {
		Pattern pattern = Pattern.compile("\\:\\S+\\:");
		Matcher matcher = pattern.matcher(sql);
		String result = sql;
		while (matcher.find()) {
			String matched = matcher.group();
			String className = matched.substring(1, matched.length() - 1);
			try {
				Class entityClass = Class.forName(className);
				SpannerPersistentEntity spannerPersistentEntity = this.spannerMappingContext
						.getPersistentEntity(entityClass);
				if (spannerPersistentEntity == null) {
					throw new SpannerDataException(
							"The class used in the SQL statement is not a Spanner persistent entity: "
									+ className);
				}
				result = result.replace(matched, spannerPersistentEntity.tableName());
			}
			catch (ClassNotFoundException e) {
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
						queryTagValue.intialParams);
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
	public Object executeRawResult(Object[] parameters) {

		QueryTagValue queryTagValue = new QueryTagValue(getParamTags(), parameters,
				resolveEntityClassNames(this.sql));

		resolveSpELTags(queryTagValue);

		return this.spannerOperations.query(this.entityType,
				SpannerStatementQueryExecutor.buildStatementFromSqlWithArgs(
						queryTagValue.sql, queryTagValue.tags,
						queryTagValue.params.toArray()),
				new SpannerQueryOptions().setAllowPartialRead(true));
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

		String sql;

		QueryTagValue(List<String> tags, Object[] params, String sql) {
			this.tags = tags;
			this.intialParams = params;
			this.sql = sql;
			this.initialTags = new HashSet<>(tags);
			this.params = new ArrayList<>(Arrays.asList(params));
		}
	}
}
