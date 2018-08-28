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

package org.springframework.cloud.gcp.data.datastore.repository.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.GqlQuery;
import com.google.cloud.datastore.GqlQuery.Builder;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query.ResultType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreOperations;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.CompositeStringExpression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;

/**
 * Query Method for GQL queries.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class GqlDatastoreQuery<T> implements RepositoryQuery {

	private static final Map<Class<?>, Function<Builder, BiFunction<String, Object, Builder>>>
      GQL_PARAM_BINDING_FUNC_MAP;

	// A small string that isn't used in GQL syntax
  private static String ENTITY_CLASS_NAME_BOOKEND = "|";

	static {
		GQL_PARAM_BINDING_FUNC_MAP = ImmutableMap
				.<Class<?>, Function<Builder, BiFunction<String, Object, Builder>>> builder()
				.put(Cursor.class, builder -> (s, o) -> builder.setBinding(s, (Cursor) o))
				.put(String.class, builder -> (s, o) -> builder.setBinding(s, (String) o))
				.put(Long.class, builder -> (s, o) -> builder.setBinding(s, (long) o))
				.put(Double.class, builder -> (s, o) -> builder.setBinding(s, (double) o))
				.put(Boolean.class,
						builder -> (s, o) -> builder.setBinding(s, (boolean) o))
				.put(Timestamp.class,
						builder -> (s, o) -> builder.setBinding(s, (Timestamp) o))
				.put(Key.class, builder -> (s, o) -> builder.setBinding(s, (Key) o))
				.put(Blob.class, builder -> (s, o) -> builder.setBinding(s, (Blob) o))
				.build();
	}

  protected final QueryMethod queryMethod;

  protected final DatastoreOperations datastoreOperations;

  protected final DatastoreMappingContext datastoreMappingContext;

  protected final Class<T> entityType;

  private final String gql;

	private QueryMethodEvaluationContextProvider evaluationContextProvider;

  private SpelExpressionParser expressionParser;

  /**
   * Constructor
   * @param type the underlying entity type
   * @param queryMethod the underlying query method to support.
   * @param datastoreOperations used for executing queries.
   * @param datastoreMappingContext used for getting metadata about entities.
   */
  GqlDatastoreQuery(Class<T> type, QueryMethod queryMethod,
      DatastoreOperations datastoreOperations,String gql,
			QueryMethodEvaluationContextProvider evaluationContextProvider,
      SpelExpressionParser expressionParser,
      DatastoreMappingContext datastoreMappingContext) {
    this.queryMethod = queryMethod;
    this.entityType = type;
    this.datastoreOperations = datastoreOperations;
    this.datastoreMappingContext = datastoreMappingContext;
    this.evaluationContextProvider = evaluationContextProvider;
    this.expressionParser = expressionParser;
    this.gql = StringUtils.trimTrailingCharacter(gql.trim(), ';');
  }

  @Override
  public Object execute(Object[] parameters) {
    List<T> rawResult = executeRawResult(parameters);
    return applyProjection(rawResult);
  }

  protected Object applyProjection(List<T> rawResult) {
    if (rawResult == null) {
      return null;
    }
    return rawResult.stream().map(result -> processRawObjectForProjection(result))
        .collect(Collectors.toList());
  }

  @Override
  public QueryMethod getQueryMethod() {
    return this.queryMethod;
  }

  @VisibleForTesting
  Object processRawObjectForProjection(Object object) {
    return this.queryMethod.getResultProcessor().processResult(object);
  }

  List<T> executeRawResult(Object[] parameters) {
    List<Object> params = new ArrayList<>();

    for (Object param : parameters) {
      params.add(param);
    }

    QueryTagValue queryTagValue = new QueryTagValue(getParamTags(), parameters,
        params.toArray(),
        resolveEntityClassNames(this.gql));

    resolveSpELTags(queryTagValue);

		List<T> results = new ArrayList<>();
		Iterable<T> found =
		this.datastoreOperations
				.query(bindArgsToGqlQuery(resolveEntityClassNames(queryTagValue.gql),
								queryTagValue.tags, queryTagValue.params),
								this.entityType);
		if (found != null) {
			results.forEach(results::add);
		}
		return results;
	}

	private GqlQuery<Entity> bindArgsToGqlQuery(String gql, List<String> tags,
			List<Object> vals) {
		Builder<Entity> builder = GqlQuery.newGqlQueryBuilder(ResultType.ENTITY, gql);
		if (tags.size() != vals.size()) {
			throw new DatastoreDataException("Annotated GQL Query Method "
					+ this.queryMethod.getName() + " has " + tags.size()
					+ " tags but a different number of parameter values: " + vals.size());
		}
		for (int i = 0; i < tags.size(); i++) {
			Object val = vals.get(i);
			if (!GQL_PARAM_BINDING_FUNC_MAP.containsKey(val.getClass())) {
				throw new DatastoreDataException(
						"Param value for GQL annotated query is not a supported Cloud "
                + "Datastore GQL param type: "
								+ val.getClass());
			}
			GQL_PARAM_BINDING_FUNC_MAP.get(val.getClass()).apply(builder)
					.apply(tags.get(i), val);
		}
		return builder.build();
  }

  private List<String> getParamTags() {
    List<String> tags = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    Parameters parameters = getQueryMethod().getParameters();
    for (int i = 0; i < parameters.getNumberOfParameters(); i++) {
      Parameter param = parameters.getParameter(i);
      Optional<String> paramName = param.getName();
      if (!paramName.isPresent()) {
        throw new DatastoreDataException(
            "Query method has a parameter without a valid name: "
                + getQueryMethod().getName());
      }
      String name = paramName.get();
      if (seen.contains(name)) {
        throw new DatastoreDataException(
            "More than one param has the same name: " + name);
      }
      seen.add(name);
      tags.add(name);
    }
    return tags;
  }

  private void resolveSpELTags(QueryTagValue queryTagValue) {
    Expression[] expressions = detectExpressions(queryTagValue.gql);
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
        throw new DatastoreDataException(
            "Unexpected expression type. GQL queries are expected to be "
                + "concatenation of Literal and SpEL expressions.");
      }
    }
    queryTagValue.gql = sb.toString();
  }

  private Expression[] detectExpressions(String gql) {
    Expression expression = this.expressionParser.parseExpression(gql,
        ParserContext.TEMPLATE_EXPRESSION);
    if (expression instanceof LiteralExpression) {
      return new Expression[] { expression };
    }
    else if (expression instanceof CompositeStringExpression) {
      return ((CompositeStringExpression) expression).getExpressions();
    }
    else {
      throw new DatastoreDataException("Unexpected expression type. "
					+ "Query can either contain no SpEL expressions or have only "
					+ "literal SpEL expressions in the GQL.");
    }
  }

  private String resolveEntityClassNames(String sql) {
    Pattern pattern = Pattern.compile("\\" + ENTITY_CLASS_NAME_BOOKEND + "\\S+\\"
        + ENTITY_CLASS_NAME_BOOKEND + "");
    Matcher matcher = pattern.matcher(sql);
    String result = sql;
    while (matcher.find()) {
      String matched = matcher.group();
      String className = matched.substring(1, matched.length() - 1);
      try {
        Class entityClass = Class.forName(className);
        DatastorePersistentEntity datastorePersistentEntity = this.datastoreMappingContext
            .getPersistentEntity(entityClass);
        if (datastorePersistentEntity == null) {
          throw new DatastoreDataException(
              "The class used in the GQL statement is not a Cloud Datastore persistent entity: "
                  + className);
        }
        result = result.replace(matched, datastorePersistentEntity.kindName());
      }
      catch (ClassNotFoundException e) {
        throw new DatastoreDataException(
            "The class name does not refer to an available entity type: "
                + className);
      }
    }
    return result;
  }

  // Convenience class to hold a grouping of SQL, tags, and parameter values.
  private static class QueryTagValue {

    List<String> tags;

    final Set<String> initialTags;

    List<Object> params;

    final Object[] intialParams;

    final Object[] rawParams;

    String gql;

    QueryTagValue(List<String> tags, Object[] rawParams, Object[] params,
        String gql) {
      this.tags = tags;
      this.intialParams = params;
      this.gql = gql;
      this.initialTags = new HashSet<>(tags);
      this.params = new ArrayList<>(Arrays.asList(params));
      this.rawParams = rawParams;
    }
  }
}
