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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
public class SqlSpannerQuery implements RepositoryQuery {

	private final QueryMethod queryMethod;

	private final Class entityType;

	private final SpannerOperations spannerOperations;

	private final String sql;

	private final List<String> tags;

	public SqlSpannerQuery(Class type, QueryMethod queryMethod,
			SpannerOperations spannerOperations, String sql) {
		this.queryMethod = queryMethod;
		this.entityType = type;
		this.spannerOperations = spannerOperations;
		this.sql = sql;
		this.tags = getTags(sql);
	}

	private List<String> getTags(String sql) {
		Pattern pattern = Pattern.compile("@\\S+");
		Matcher matcher = pattern.matcher(sql);
		List<String> tags = new ArrayList<>();
		while (matcher.find()) {
			// The initial '@' character must be excluded for Spanner
			tags.add(matcher.group().substring(1));
		}
		return tags;
	}

	@Override
	public Object execute(Object[] parameters) {
		return this.spannerOperations.find(this.entityType, SpannerStatementQueryExecutor
				.buildStatementFromSqlWithArgs(this.sql, this.tags, parameters));
	}

	@Override
	public QueryMethod getQueryMethod() {
		return this.queryMethod;
	}
}
