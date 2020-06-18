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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.datastore.BaseEntity;
import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.GqlQuery;
import com.google.cloud.datastore.GqlQuery.Builder;
import com.google.cloud.datastore.Key;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreOperations;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreResultsIterable;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreNativeTypes;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DiscriminatorField;
import org.springframework.cloud.gcp.data.datastore.core.util.ValueUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.SpelEvaluator;
import org.springframework.data.repository.query.SpelQueryContext;
import org.springframework.util.StringUtils;

import static org.springframework.core.annotation.AnnotationUtils.getAnnotation;

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
	 * @param evaluationContextProvider the provider used to evaluate SpEL expressions in
	 *     queries.
	 * @param datastoreMappingContext used for getting metadata about entities.
	 */
	public GqlDatastoreQuery(Class<T> type, DatastoreQueryMethod queryMethod,
							DatastoreOperations datastoreTemplate, String gql,
							QueryMethodEvaluationContextProvider evaluationContextProvider,
							DatastoreMappingContext datastoreMappingContext) {
		super(queryMethod, datastoreTemplate, datastoreMappingContext, type);
		this.evaluationContextProvider = evaluationContextProvider;
		this.originalGql = StringUtils.trimTrailingCharacter(gql.trim(), ';');
		setOriginalParamTags();
		setEvaluatingSpelQueryContext();
		setGqlResolvedEntityClassName();
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
		if (getAnnotation(this.entityType.getSuperclass(), DiscriminatorField.class) != null) {
			throw new DatastoreDataException("Can't append discrimination condition");
		}

		ParsedQueryWithTagsAndValues parsedQueryWithTagsAndValues =
				new ParsedQueryWithTagsAndValues(this.originalParamTags, parameters);

		GqlQuery query = parsedQueryWithTagsAndValues.bindArgsToGqlQuery();

		Class returnedItemType = this.queryMethod.getReturnedObjectType();

		boolean isNonEntityReturnType = isNonEntityReturnedType(returnedItemType);

		DatastoreResultsIterable found = isNonEntityReturnType
				? this.datastoreOperations.queryIterable(query, GqlDatastoreQuery::getNonEntityObjectFromRow)
				: this.datastoreOperations.queryKeysOrEntities(query, this.entityType);

		Object result;
		if (isPageQuery() || isSliceQuery()) {
			result = buildPageOrSlice(parameters, parsedQueryWithTagsAndValues, found);
		}
		else if (this.queryMethod.isCollectionQuery()) {
			result = convertCollectionResult(returnedItemType, found);
		}
		else {
			result = convertSingularResult(returnedItemType, isNonEntityReturnType, found);
		}

		return result;
	}

	private Object buildPageOrSlice(Object[] parameters, ParsedQueryWithTagsAndValues parsedQueryWithTagsAndValues,
			DatastoreResultsIterable found) {
		Pageable pageableParam =
				new ParametersParameterAccessor(getQueryMethod().getParameters(), parameters).getPageable();
		List resultsList = found == null ? Collections.emptyList()
				: (List) StreamSupport.stream(found.spliterator(), false).collect(Collectors.toList());

		Cursor cursor = found != null ? found.getCursor() : null;
		Slice result = isPageQuery()
				? buildPage(pageableParam, parsedQueryWithTagsAndValues, cursor, resultsList)
				: buildSlice(pageableParam, parsedQueryWithTagsAndValues, cursor, resultsList);
		return processRawObjectForProjection(result);
	}

	private Slice buildSlice(Pageable pageableParam, ParsedQueryWithTagsAndValues parsedQueryWithTagsAndValues,
			Cursor cursor, List resultsList) {
		GqlQuery nextQuery = parsedQueryWithTagsAndValues.bindArgsToGqlQuery(cursor, 1);
		DatastoreResultsIterable<?> next = this.datastoreOperations.queryKeysOrEntities(nextQuery, this.entityType);

		Pageable pageable = DatastorePageable.from(pageableParam, cursor, null);
		return new SliceImpl(resultsList, pageable, next.iterator().hasNext());
	}

	private Page buildPage(Pageable pageableParam, ParsedQueryWithTagsAndValues parsedQueryWithTagsAndValues,
			Cursor cursor, List resultsList) {
		Long count = pageableParam instanceof DatastorePageable
				? ((DatastorePageable) pageableParam).getTotalCount()
				: null;
		if (count == null) {
			GqlQuery nextQuery = parsedQueryWithTagsAndValues.bindArgsToGqlQueryNoLimit();
			DatastoreResultsIterable<?> next = this.datastoreOperations.queryKeysOrEntities(nextQuery,
					this.entityType);
			count = StreamSupport.stream(next.spliterator(), false).count();
		}

		Pageable pageable = DatastorePageable.from(pageableParam, cursor, count);
		return new PageImpl(resultsList, pageable, count);
	}

	private Object convertCollectionResult(Class returnedItemType, Iterable rawResult) {
		Object result = this.datastoreOperations.getDatastoreEntityConverter()
				.getConversions().convertOnRead(
						rawResult, this.queryMethod.getCollectionReturnType(), returnedItemType);
		return processRawObjectForProjection(result);
	}

	private Object convertSingularResult(Class returnedItemType,
			boolean isNonEntityReturnType, Iterable rawResult) {
		if (rawResult == null) {
			return null;
		}
		Iterator iterator = rawResult.iterator();
		if (this.queryMethod.isExistsQuery()) {
			return iterator.hasNext();
		}

		if (this.queryMethod.isCountQuery()) {
			return StreamSupport.stream(rawResult.spliterator(), false).count();
		}

		if (!iterator.hasNext()) {
			return null;
		}
		Object result = iterator.next();
		if (iterator.hasNext()) {
			throw new DatastoreDataException(
					"The query method returns a singular object but "
							+ "the query returned more than one result.");
		}
		return isNonEntityReturnType
				? this.datastoreOperations.getDatastoreEntityConverter().getConversions()
						.convertOnRead(result, null, returnedItemType)
				: this.queryMethod.getResultProcessor().processResult(result);
	}

	boolean isNonEntityReturnedType(Class returnedType) {
		return this.datastoreOperations.getDatastoreEntityConverter().getConversions()
				.getDatastoreCompatibleType(returnedType).isPresent();
	}

	private void setOriginalParamTags() {
		this.originalParamTags = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		Parameters parameters = getQueryMethod().getParameters();
		for (int i = 0; i < parameters.getNumberOfParameters(); i++) {
			Parameter param = parameters.getParameter(i);
			if (Pageable.class.isAssignableFrom(param.getType()) || Sort.class.isAssignableFrom(param.getType())) {
				continue;
			}
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

	private void setGqlResolvedEntityClassName() {
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
		this.gqlResolvedEntityClassName = result;
	}

	private void setEvaluatingSpelQueryContext() {
		Set<String> originalTags = new HashSet<>(GqlDatastoreQuery.this.originalParamTags);

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


	// Convenience class to hold a grouping of GQL, tags, and parameter values.
	private class ParsedQueryWithTagsAndValues {

		static final String LIMIT_CLAUSE = " LIMIT @limit";
		static final String LIMIT_TAG_NAME = "limit";
		static final String OFFSET_CLAUSE = " OFFSET @offset";
		static final String OFFSET_TAG_NAME = "offset";
		static final String ORDER_BY = " ORDER BY ";
		List<String> tagsOrdered;

		final Object[] rawParams;

		List<Object> params;

		private final String noLimitQuery;

		String finalGql;

		int cursorPosition;

		int limitPosition;

		ParsedQueryWithTagsAndValues(List<String> initialTags, Object[] rawParams) {
			this.params = Arrays.stream(rawParams).filter(e -> !(e instanceof Pageable || e instanceof Sort))
					.collect(Collectors.toList());
			this.rawParams = rawParams;
			this.tagsOrdered = new ArrayList<>(initialTags);

			SpelEvaluator spelEvaluator = GqlDatastoreQuery.this.evaluatingSpelQueryContext.parse(
					GqlDatastoreQuery.this.gqlResolvedEntityClassName,
					GqlDatastoreQuery.this.queryMethod.getParameters());
			Map<String, Object> results = spelEvaluator.evaluate(this.rawParams);
			this.finalGql = spelEvaluator.getQueryString();

			for (Map.Entry<String, Object> entry : results.entrySet()) {
				this.params.add(entry.getValue());
				// Cloud Datastore requires the tag name without the @
				this.tagsOrdered.add(entry.getKey().substring(1));
			}

			ParameterAccessor paramAccessor =
					new ParametersParameterAccessor(getQueryMethod().getParameters(), rawParams);
			Sort sort = paramAccessor.getSort();

			this.finalGql = addSort(this.finalGql, sort);

			this.noLimitQuery = this.finalGql;
			Pageable pageable = paramAccessor.getPageable();
			if (!pageable.equals(Pageable.unpaged())) {
				this.finalGql += LIMIT_CLAUSE;
				this.tagsOrdered.add(LIMIT_TAG_NAME);
				this.limitPosition = this.params.size();
				this.params.add(pageable.getPageSize());

				this.finalGql += OFFSET_CLAUSE;
				this.tagsOrdered.add(OFFSET_TAG_NAME);
				this.cursorPosition = this.params.size();
				if (pageable instanceof DatastorePageable && ((DatastorePageable) pageable).toCursor() != null) {
					this.params.add(((DatastorePageable) pageable).toCursor());
				}
				else {
					this.params.add(pageable.getOffset());
				}
			}
		}

		private GqlQuery<? extends BaseEntity> bindArgsToGqlQuery(Cursor newCursor, int newLimit) {
			this.params.set(this.cursorPosition, newCursor);
			this.params.set(this.limitPosition, newLimit);

			return bindArgsToGqlQuery();
		}

		private GqlQuery<? extends BaseEntity> bindArgsToGqlQueryNoLimit() {
			this.finalGql = this.noLimitQuery;
			this.tagsOrdered = this.tagsOrdered.subList(0, this.limitPosition);
			this.params = this.params.subList(0, this.limitPosition);

			return bindArgsToGqlQuery();
		}

		private GqlQuery<? extends BaseEntity> bindArgsToGqlQuery() {
			Builder builder = GqlQuery.newGqlQueryBuilder(this.finalGql);
			builder.setAllowLiteral(true);
			if (this.tagsOrdered.size() != this.params.size()) {
				throw new DatastoreDataException("Annotated GQL Query Method "
						+ GqlDatastoreQuery.this.queryMethod.getName() + " has " + this.tagsOrdered.size()
						+ " tags but a different number of parameter values: " + this.params.size());
			}
			for (int i = 0; i < this.tagsOrdered.size(); i++) {
				Object val = this.params.get(i);
				Object boundVal;
				if (val instanceof Cursor) {
					boundVal = val;
				}
				else if (ValueUtil.isCollectionLike(val.getClass())) {
					boundVal = convertCollectionParamToCompatibleArray((List) ValueUtil.toListIfArray(val));
				}
				else {
					boundVal = GqlDatastoreQuery.this.datastoreOperations.getDatastoreEntityConverter().getConversions()
							.convertOnWriteSingle(convertEntitiesToKeys(val)).get();
				}
				DatastoreNativeTypes.bindValueToGqlBuilder(builder, this.tagsOrdered.get(i), boundVal);
			}
			return builder.build();
		}

		private String addSort(String finalGql, Sort sort) {
			if (sort.equals(Sort.unsorted())) {
				return finalGql;
			}
			//similar to Spring Data JPA, we don't map passed sort properties to persistent properties names
			// in @Query annotated methods
			String orderString = sort.stream().map(order -> order.getProperty() + " " + order.getDirection())
					.collect(Collectors.joining(", "));
			return finalGql + ORDER_BY + orderString;
		}

		private Object convertEntitiesToKeys(Object o) {
			if (GqlDatastoreQuery.this.datastoreMappingContext.hasPersistentEntityFor(o.getClass())) {
				return GqlDatastoreQuery.this.datastoreOperations.getKey(o);
			}
			return o;
		}
	}
}

