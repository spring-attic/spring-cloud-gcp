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

package org.springframework.cloud.gcp.data.datastore.repository.support;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntityInformation;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.Assert;

/**
 * Repository factory for Datastore.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class DatastoreRepositoryFactory extends RepositoryFactorySupport {

	private final DatastoreMappingContext datastoreMappingContext;

	private final DatastoreTemplate datastoreTemplate;

	/**
	 * Constructor
	 * @param datastoreMappingContext the mapping context used to get mapping metadata for
	 * entity types.
	 * @param datastoreTemplate the Datastore operations object used by Datastore
	 * repositories.
	 */
	DatastoreRepositoryFactory(DatastoreMappingContext datastoreMappingContext,
			DatastoreTemplate datastoreTemplate) {
		Assert.notNull(datastoreMappingContext,
				"A non-null Datastore mapping context is required.");
		Assert.notNull(datastoreTemplate,
				"A non-null Datastore template object is required.");
		this.datastoreMappingContext = datastoreMappingContext;
		this.datastoreTemplate = datastoreTemplate;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		DatastorePersistentEntity entity = this.datastoreMappingContext
				.getPersistentEntity(domainClass);

		if (entity == null) {
			throw new MappingException(
					"Could not lookup mapping metadata for domain class: "
							+ domainClass.getName());
		}

		return new DatastorePersistentEntityInformation<>(entity);
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation metadata) {
		return getTargetRepositoryViaReflection(metadata, this.datastoreTemplate,
				metadata.getDomainType());
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SimpleDatastoreRepository.class;
	}
}
