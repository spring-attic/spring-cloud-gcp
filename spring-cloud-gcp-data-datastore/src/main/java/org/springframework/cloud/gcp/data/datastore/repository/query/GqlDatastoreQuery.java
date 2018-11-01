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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.datastore.BaseEntity;
import com.google.cloud.datastore.GqlQuery;
import com.google.cloud.datastore.GqlQuery.Builder;
import com.google.cloud.datastore.Key;
import com.google.common.annotations.VisibleForTesting;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreNativeTypes;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;

/**
 * Query Method for GQL queries.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class GqlDatastoreQuery<T> extends AbstractDatastoreQuery<T> {


	private final String gql;

	// unused currently, but will be used for SpEL expression in the query.
	private QueryMethodEvaluationContextProvider evaluationContextProvider;

	// unused currently, but will be used for SpEL expression in the query.
	private SpelExpressionParser expressionParser;

	/**
	 * Constructor
	 * @param type the underlying entity type
	 * @param queryMethod the underlying query method to support.
	 * @param datastoreTemplate used for executing queries.
	 * @param datastoreMappingContext used for getting metadata about entities.
	 */
	public GqlDatastoreQuery(Class<T> type, DatastoreQueryMethod queryMethod,
			DatastoreTemplate datastoreTemplate, String gql,
			QueryMethodEvaluationContextProvider evaluationContextProvider,
			SpelExpressionParser expressionParser,
			DatastoreMappingContext datastoreMappingContext) {
		super(queryMethod, datastoreTemplate, datastoreMappingContext, type);
		this.evaluationContextProvider = evaluationContextProvider;
		this.expressionParser = expressionParser;
		this.gql = StringUtils.trimTrailingCharacter(gql.trim(), ';');
	}

	private static Object getNonEntityObjectFromRow(Object x) {
		Object mappedResult;
		if (x instanceof Key) {
			mappedResult = x;
		}
		else {
			BaseEntity entity = (BaseEntity) x;
			Set<String> colNames = entity.getNames();
			if (colNames.size() > 1) {
				throw new DatastoreDataException(
						"The query method returns non-entity types, but the query result has "
								+ "more than one column. Use a Projection entity type instead.");
			}
			mappedResult = entity.getValue((String) colNames.toArray()[0]).get();
		}
		return mappedResult;
	}

	@Override
	public Object execute(Object[] parameters) {
		GqlQuery query = bindArgsToGqlQuery(this.gql, getParamTags(), parameters);
		boolean returnTypeIsCollection = this.queryMethod.isCollectionQuery();
		Class returnedItemType = this.queryMethod.getReturnedObjectType();

		boolean isNonEntityReturnType = isNonEntityReturnedType(returnedItemType);

		Iterable found = isNonEntityReturnType
				? this.datastoreTemplate.query(query,
						GqlDatastoreQuery::getNonEntityObjectFromRow)
				: this.datastoreTemplate.queryKeysOrEntities(query, this.entityType);

		List rawResult = found == null ? Collections.emptyList()
				: (List) StreamSupport.stream(found.spliterator(), false)
						.collect(Collectors.toList());

		Object result;

		if (returnTypeIsCollection) {
			result = convertCollectionResult(returnedItemType, isNonEntityReturnType,
					rawResult);
		}
		else {
			if (rawResult.isEmpty()) {
				result = null;
			}
			else {

				result = convertSingularResult(returnedItemType, isNonEntityReturnType,
						rawResult);
			}
		}

		return result;
	}

	private Object convertCollectionResult(Class returnedItemType,
			boolean isNonEntityReturnType, List rawResult) {
		Object result = this.datastoreTemplate.getDatastoreEntityConverter()
				.getConversions().convertOnRead(
						isNonEntityReturnType ? rawResult : applyProjection(rawResult),
						this.queryMethod.getCollectionReturnType(), returnedItemType);
		return result;
	}

	private Object convertSingularResult(Class returnedItemType,
			boolean isNonEntityReturnType, List rawResult) {

		if (this.queryMethod.isCountQuery()) {
			return rawResult.size();
		}
		else if (this.queryMethod.isExistsQuery()) {
			return !rawResult.isEmpty();
		}
		if (rawResult.size() > 1) {
			throw new DatastoreDataException(
					"The query method returns a singular object but "
							+ "the query returned more than one result.");
		}
		return isNonEntityReturnType
				? this.datastoreTemplate.getDatastoreEntityConverter().getConversions()
						.convertOnRead(rawResult.get(0), null, returnedItemType)
				: this.queryMethod.getResultProcessor().processResult(rawResult.get(0));
	}

	@VisibleForTesting
	boolean isNonEntityReturnedType(Class returnedType) {
		return this.datastoreTemplate.getDatastoreEntityConverter().getConversions()
				.getDatastoreCompatibleType(returnedType).isPresent();
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

	private GqlQuery<? extends BaseEntity> bindArgsToGqlQuery(String gql,
			List<String> tags,
			Object[] vals) {
		Builder builder = GqlQuery.newGqlQueryBuilder(gql);
		builder.setAllowLiteral(true);
		if (tags.size() != vals.length) {
			throw new DatastoreDataException("Annotated GQL Query Method "
					+ this.queryMethod.getName() + " has " + tags.size()
					+ " tags but a different number of parameter values: " + vals.length);
		}
		for (int i = 0; i < tags.size(); i++) {
			Object val = vals[i];
			DatastoreNativeTypes.bindValueToGqlBuilder(builder, tags.get(i), val);
		}
		return builder.build();
	}
}
