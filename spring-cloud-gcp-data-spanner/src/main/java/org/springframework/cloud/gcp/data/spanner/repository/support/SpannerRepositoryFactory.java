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

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.PersistentEntityInformation;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.Assert;

/**
 * @author Ray Tsang
 * @author Chengyuan Zhao
 */
public class SpannerRepositoryFactory extends RepositoryFactorySupport {

	private final SpannerMappingContext spannerMappingContext;

	private final SpannerOperations spannerOperations;

	/**
	 * Constructor
	 * @param spannerMappingContext the mapping context used to get mapping metadata for
	 * entity types.
	 * @param spannerOperations the spanner operations object used by Spanner repositories.
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

		return new PersistentEntityInformation<>((SpannerPersistentEntity<T>) entity);
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation metadata) {
		return getTargetRepositoryViaReflection(metadata, this.spannerOperations,
				metadata.getDomainType());
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SpannerRepositoryImpl.class;
	}
}
