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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
public class SqlSpannerQuery implements RepositoryQuery {

	// A character that isn't used in SQL
	private static String ENTITY_CLASS_NAME_BOOKEND = ":";

	private final QueryMethod queryMethod;

	private final Class entityType;

	private final SpannerOperations spannerOperations;

	private final String sql;

	private final List<String> tags;

	private final SpannerMappingContext spannerMappingContext;

	private EvaluationContextProvider evaluationContextProvider;

	private SpelExpressionParser expressionParser;

	public SqlSpannerQuery(Class type, QueryMethod queryMethod,
			SpannerOperations spannerOperations, String sql,
			EvaluationContextProvider evaluationContextProvider,
			SpelExpressionParser expressionParser,
			SpannerMappingContext spannerMappingContext) {
		this.queryMethod = queryMethod;
		this.entityType = type;
		this.spannerOperations = spannerOperations;
		this.tags = getTags();
		this.evaluationContextProvider = evaluationContextProvider;
		this.expressionParser = expressionParser;
		this.spannerMappingContext = spannerMappingContext;
		this.sql = sql;
	}

	private List<String> getTags() {
		List<String> tags = new ArrayList<>();
		Parameters parameters = getQueryMethod().getParameters();
		for (int i = 0; i < parameters.getNumberOfParameters(); i++) {
			Optional<String> paramName = parameters.getParameter(i).getName();
			if (!paramName.isPresent() || paramName.get() == null) {
				throw new SpannerDataException(
						"Query method has a parameter without a valid name: "
								+ getQueryMethod().getName());
			}
			tags.add(paramName.get());
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

	@Override
	public Object execute(Object[] parameters) {
		return this.spannerOperations.query(this.entityType,
				SpannerStatementQueryExecutor.buildStatementFromSqlWithArgs(
						resolveEntityClassNames(this.sql), this.tags, parameters));
	}

	@Override
	public QueryMethod getQueryMethod() {
		return this.queryMethod;
	}
}
