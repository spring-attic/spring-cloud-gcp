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

package org.springframework.cloud.gcp.data.spanner.repository.support;

import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntityInformation;
import org.springframework.cloud.gcp.data.spanner.repository.query.SpannerQueryLookupStrategy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Ray Tsang
 * @author Chengyuan Zhao
 */
public class SpannerRepositoryFactory extends RepositoryFactorySupport
		implements ApplicationContextAware {

	private static final SpelExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

	private final SpannerMappingContext spannerMappingContext;

	private final SpannerOperations spannerOperations;

	private ApplicationContext applicationContext;

	/**
	 * Constructor
	 * @param spannerMappingContext the mapping context used to get mapping metadata for
	 * entity types.
	 * @param spannerOperations the spanner operations object used by Spanner
	 * repositories.
	 */
	public SpannerRepositoryFactory(SpannerMappingContext spannerMappingContext,
			SpannerOperations spannerOperations) {
		Assert.notNull(spannerMappingContext,
				"A valid Spanner mapping context is required.");
		Assert.notNull(spannerOperations,
				"A valid Spanner operations object is required.");
		this.spannerMappingContext = spannerMappingContext;
		this.spannerOperations = spannerOperations;
	}

	@Override
	public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		SpannerPersistentEntity<?> entity = this.spannerMappingContext
				.getPersistentEntity(domainClass);

		if (entity == null) {
			throw new MappingException(String.format(
					"Could not lookup mapping metadata for domain class %s!",
					domainClass.getName()));
		}

		return new SpannerPersistentEntityInformation(entity);
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation metadata) {
		return getTargetRepositoryViaReflection(metadata, this.spannerOperations,
				metadata.getDomainType());
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SimpleSpannerRepository.class;
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable Key key,
			EvaluationContextProvider evaluationContextProvider) {

		return Optional.of(new SpannerQueryLookupStrategy(this.spannerMappingContext,
				this.spannerOperations,
				delegateContextProvider(evaluationContextProvider), EXPRESSION_PARSER));
	}

	private EvaluationContextProvider delegateContextProvider(
			EvaluationContextProvider evaluationContextProvider) {
		return new EvaluationContextProvider() {
			@Override
			public <T extends Parameters<?, ?>> EvaluationContext getEvaluationContext(
					T parameters, Object[] parameterValues) {
				StandardEvaluationContext evaluationContext = (StandardEvaluationContext) evaluationContextProvider
						.getEvaluationContext(parameters, parameterValues);
				evaluationContext
						.setRootObject(SpannerRepositoryFactory.this.applicationContext);
				evaluationContext.addPropertyAccessor(new BeanFactoryAccessor());
				evaluationContext.setBeanResolver(new BeanFactoryResolver(
						SpannerRepositoryFactory.this.applicationContext));
				return evaluationContext;
			}
		};
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}
}
