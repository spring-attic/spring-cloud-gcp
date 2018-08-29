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

package org.springframework.cloud.gcp.autoconfigure.datastore;

import java.lang.annotation.Annotation;

import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.cloud.gcp.data.datastore.repository.config.DatastoreRepositoryConfigurationExtension;
import org.springframework.cloud.gcp.data.datastore.repository.config.EnableDatastoreRepositories;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * Used to auto-configure Spring Data Cloud Datastore Repositories.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class DatastoreRepositoriesAutoConfigureRegistrar
		extends AbstractRepositoryConfigurationSourceSupport {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableDatastoreRepositories.class;
	}

	@Override
	protected Class<?> getConfiguration() {
		return EnableDatastoreRepositoriesConfiguration.class;
	}

	@Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
		return new DatastoreRepositoryConfigurationExtension();
	}

	@EnableDatastoreRepositories
	private static class EnableDatastoreRepositoriesConfiguration {

	}
}
