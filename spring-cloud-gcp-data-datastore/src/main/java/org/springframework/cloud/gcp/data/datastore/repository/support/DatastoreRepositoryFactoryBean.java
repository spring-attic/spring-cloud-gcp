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

import org.springframework.beans.BeansException;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * Factory bean for creating factories that create Datastore repositories.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class DatastoreRepositoryFactoryBean<S, ID>
		extends RepositoryFactoryBeanSupport<DatastoreRepository<S, ID>, S, ID>
		implements ApplicationContextAware {

	private DatastoreMappingContext datastoreMappingContext;

	private DatastoreTemplate datastoreTemplate;

	private ApplicationContext applicationContext;

	/**
	 * Creates a new {@link DatastoreRepositoryFactoryBean} for the given repository
	 * interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 */
	DatastoreRepositoryFactoryBean(
			Class<DatastoreRepository<S, ID>> repositoryInterface) {
		super(repositoryInterface);
	}

	public void setDatastoreTemplate(DatastoreTemplate datastoreTemplate) {
		this.datastoreTemplate = datastoreTemplate;
	}

	public void setDatastoreMappingContext(DatastoreMappingContext mappingContext) {
		super.setMappingContext(mappingContext);
		this.datastoreMappingContext = mappingContext;
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory() {
		DatastoreRepositoryFactory datastoreRepositoryFactory = new DatastoreRepositoryFactory(
				this.datastoreMappingContext, this.datastoreTemplate);
		datastoreRepositoryFactory.setApplicationContext(this.applicationContext);
		return datastoreRepositoryFactory;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}
}
