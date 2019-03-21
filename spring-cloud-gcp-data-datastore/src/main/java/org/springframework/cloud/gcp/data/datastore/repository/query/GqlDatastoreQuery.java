/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.datastore.repository.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.datastore.BaseEntity;
import com.google.cloud.datastore.GqlQuery;
import com.google.cloud.datastore.GqlQuery.Builder;
import com.google.cloud.datastore.Key;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreNativeTypes;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.cloud.gcp.data.datastore.core.util.ValueUtil;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.SpelEvaluator;
import org.springframework.data.repository.query.SpelQueryContext;
import org.springframework.util.StringUtils;

/**
 * Query Method for GQL queries.
 * @param <T> the return type of the Query Method
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class GqlDatastoreQuery<T> extends AbstractDatastoreQuery<T> {

	// A small string that isn't used in GQL syntax
	private static final String ENTITY_CLASS_NAME_BOOKEND = "|";

	private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("\\" + ENTITY_CLASS_NAME_BOOKEND + "\\S+\\"
			+ ENTITY_CLASS_NAME_BOOKEND + "");

	private final String originalGql;

	private String gqlResolvedEntityClassName;

	private List<String> originalParamTags;

	private QueryMethodEvaluationContextProvider evaluationContextProvider;

	private SpelQueryContext.EvaluatingSpelQueryContext evaluatingSpelQueryContext;

	/**
	 * Constructor.
	 * @param type the underlying entity type
	 * @param queryMethod the underlying query method to support.
	 * @param datastoreTemplate used for executing queries.
	 * @param gql the query text.
	 * @param evaluationContextProvider the provider used to evaluate SpEL expressions in queries.
	 * @param datastoreMappingContext used for getting metadata about entities.
	 */
	public GqlDatastoreQuery(Class<T> type, DatastoreQueryMethod queryMethod,
			DatastoreTemplate datastoreTemplate, String gql,
			QueryMethodEvaluationContextProvider evaluationContextProvider,
			DatastoreMappingContext datastoreMappingContext) {
		super(queryMethod, datastoreTemplate, datastoreMappingContext, type);
		this.evaluationContextProvider = evaluationContextProvider;
		this.originalGql = StringUtils.trimTrailingCharacter(gql.trim(), ';');
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

		ParsedQueryWithTagsAndValues parsedQueryWithTagsAndValues = new ParsedQueryWithTagsAndValues(
				getOriginalParamTags(), parameters);

		GqlQuery query = bindArgsToGqlQuery(parsedQueryWithTagsAndValues.finalGql,
				parsedQueryWithTagsAndValues.tagsOrdered, parsedQueryWithTagsAndValues.params);

		boolean returnTypeIsCollection = this.queryMethod.isCollectionQuery();
		Class returnedItemType = this.queryMethod.getReturnedObjectType();

		boolean isNonEntityReturnType = isNonEntityReturnedType(returnedItemType);

		Iterable found = isNonEntityReturnType
				? this.datastoreTemplate.query(query,
						GqlDatastoreQuery::getNonEntityObjectFromRow)
				: this.datastoreTemplate.queryKeysOrEntities(query, this.entityType);

		List rawResult = (found != null)
				? (List) StreamSupport.stream(found.spliterator(), false).collect(Collectors.toList())
				: Collections.emptyList();

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

	boolean isNonEntityReturnedType(Class returnedType) {
		return this.datastoreTemplate.getDatastoreEntityConverter().getConversions()
				.getDatastoreCompatibleType(returnedType).isPresent();
	}

	private List<String> getOriginalParamTags() {
		if (this.originalParamTags == null) {
			this.originalParamTags = new ArrayList<>();
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
				this.originalParamTags.add(name);
			}
		}
		return this.originalParamTags;
	}

	private GqlQuery<? extends BaseEntity> bindArgsToGqlQuery(String gql,
			List<String> tags,
			List vals) {
		Builder builder = GqlQuery.newGqlQueryBuilder(gql);
		builder.setAllowLiteral(true);
		if (tags.size() != vals.size()) {
			throw new DatastoreDataException("Annotated GQL Query Method "
					+ this.queryMethod.getName() + " has " + tags.size()
					+ " tags but a different number of parameter values: " + vals.size());
		}
		for (int i = 0; i < tags.size(); i++) {
			Object val = vals.get(i);
			Object boundVal;
			if (ValueUtil.isCollectionLike(val.getClass())) {
				boundVal = convertCollectionParamToCompatibleArray((List) ValueUtil.toListIfArray(val));
			}
			else {
				boundVal = this.datastoreTemplate.getDatastoreEntityConverter().getConversions()
						.convertOnWriteSingle(val).get();
			}
			DatastoreNativeTypes.bindValueToGqlBuilder(builder, tags.get(i), boundVal);
		}
		return builder.build();
	}



	// Convenience class to hold a grouping of GQL, tags, and parameter values.
	private class ParsedQueryWithTagsAndValues {

		final List<String> tagsOrdered;

		final Object[] rawParams;

		final List<Object> params;

		final String finalGql;

		ParsedQueryWithTagsAndValues(List<String> initialTags, Object[] rawParams) {
			this.params = new ArrayList<>(Arrays.asList(rawParams));
			this.rawParams = rawParams;
			this.tagsOrdered = new ArrayList<>(initialTags);

			SpelQueryContext.EvaluatingSpelQueryContext spelQueryContext = getEvaluatingSpelQueryContext();

			SpelEvaluator spelEvaluator = spelQueryContext.parse(getGqlResolvedEntityClassName(),
					GqlDatastoreQuery.this.queryMethod.getParameters());
			Map<String, Object> results = spelEvaluator.evaluate(this.rawParams);
			this.finalGql = spelEvaluator.getQueryString();

			for (Map.Entry<String, Object> entry : results.entrySet()) {
				this.params.add(entry.getValue());
				// Cloud Datastore requires the tag name without the
				this.tagsOrdered.add(entry.getKey().substring(1));
			}
		}

		private String getGqlResolvedEntityClassName() {
			if (GqlDatastoreQuery.this.gqlResolvedEntityClassName == null) {
				Matcher matcher = CLASS_NAME_PATTERN.matcher(GqlDatastoreQuery.this.originalGql);
				String result = GqlDatastoreQuery.this.originalGql;
				while (matcher.find()) {
					String matched = matcher.group();
					String className = matched.substring(1, matched.length() - 1);
					try {
						Class entityClass = Class.forName(className);
						DatastorePersistentEntity datastorePersistentEntity = GqlDatastoreQuery.this.datastoreMappingContext
								.getPersistentEntity(entityClass);
						if (datastorePersistentEntity == null) {
							throw new DatastoreDataException(
									"The class used in the GQL statement is not a Cloud Datastore persistent entity: "
											+ className);
						}
						result = result.replace(matched, datastorePersistentEntity.kindName());
					}
					catch (ClassNotFoundException ex) {
						throw new DatastoreDataException(
								"The class name does not refer to an available entity type: "
										+ className);
					}
				}
				GqlDatastoreQuery.this.gqlResolvedEntityClassName = result;
			}
			return GqlDatastoreQuery.this.gqlResolvedEntityClassName;
		}

		private SpelQueryContext.EvaluatingSpelQueryContext getEvaluatingSpelQueryContext() {
			if (GqlDatastoreQuery.this.evaluatingSpelQueryContext == null) {
				Set<String> originalTags = new HashSet<>(getOriginalParamTags());

				GqlDatastoreQuery.this.evaluatingSpelQueryContext = SpelQueryContext.EvaluatingSpelQueryContext
						.of((counter, spelExpression) -> {
							String newTag;
							do {
								counter++;
								newTag = "@SpELtag" + counter;
							}
							while (originalTags.contains(newTag));
							originalTags.add(newTag);
							return newTag;
						}, (prefix, newTag) -> newTag)
						.withEvaluationContextProvider(GqlDatastoreQuery.this.evaluationContextProvider);
			}
			return GqlDatastoreQuery.this.evaluatingSpelQueryContext;
		}
	}
}
