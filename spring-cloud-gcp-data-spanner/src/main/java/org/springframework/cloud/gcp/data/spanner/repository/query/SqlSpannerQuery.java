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
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.SpannerQueryOptions;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
public class SqlSpannerQuery extends AbstractSpannerQuery {

	// A character that isn't used in SQL
	private static String ENTITY_CLASS_NAME_BOOKEND = ":";

	private final String sql;

	private final List<String> tags;

	private EvaluationContextProvider evaluationContextProvider;

	private SpelExpressionParser expressionParser;

	public SqlSpannerQuery(Class type, QueryMethod queryMethod,
			SpannerOperations spannerOperations, String sql,
			EvaluationContextProvider evaluationContextProvider,
			SpelExpressionParser expressionParser,
			SpannerMappingContext spannerMappingContext) {
		super(type, queryMethod, spannerOperations, spannerMappingContext);
		this.tags = getTags();
		this.evaluationContextProvider = evaluationContextProvider;
		this.expressionParser = expressionParser;
		this.sql = StringUtils.trimTrailingCharacter(sql.trim(), ';');
	}

	private boolean isPageableSort(Class type) {
		return Pageable.class.isAssignableFrom(type) || Sort.class.isAssignableFrom(type);
	}

	private List<String> getTags() {
		List<String> tags = new ArrayList<>();
		Parameters parameters = getQueryMethod().getParameters();
		for (int i = 0; i < parameters.getNumberOfParameters(); i++) {
			Parameter param = parameters.getParameter(i);
			if (isPageableSort(param.getType())) {
				continue;
			}
			Optional<String> paramName = param.getName();
			if (!paramName.isPresent()) {
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

	private String applyPageable(Pageable pageable, String sql) {
		return applySort(pageable.getSort(), sql) + " LIMIT " + pageable.getPageSize()
				+ " OFFSET " + pageable.getOffset();
	}

	private String applySort(Sort sort, String sql) {
		if (sort == null || sort.isUnsorted()) {
			return sql;
		}
		String s = "SELECT * FROM (" + sql + ") ORDER BY ";
		StringJoiner sj = new StringJoiner(" , ");
		sort.iterator().forEachRemaining(
				o -> sj.add(o.getProperty() + (o.isAscending() ? " ASC" : " DESC")));
		return s + sj.toString();
	}

	@Override
	public Object executeRawResult(Object[] parameters) {
		List<Object> params = new ArrayList();

		Pageable pageable = null;
		Sort sort = null;

		for (Object param : parameters) {
			Class paramClass = param.getClass();
			if (isPageableSort(paramClass)) {
				if (pageable != null || sort != null) {
					throw new SpannerDataException(
							"Only a single Pageable or Sort param is allowed.");
				}
				else {
					if (Pageable.class.isAssignableFrom(paramClass)) {
						pageable = (Pageable) param;
					}
					else {
						sort = (Sort) param;
					}
				}
			}
			else {
				params.add(param);
			}
		}

		String sql = resolveEntityClassNames(this.sql);

		if (pageable == null) {
			if (sort != null) {
				sql = applySort(sort, sql);
			}
		}
		else {
			sql = applyPageable(pageable, sql);
		}

		return this.spannerOperations.query(this.entityType,
				SpannerStatementQueryExecutor.buildStatementFromSqlWithArgs(
						sql, this.tags, params.toArray()),
				new SpannerQueryOptions().setAllowPartialRead(true));
	}
}
