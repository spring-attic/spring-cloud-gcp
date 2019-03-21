/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.data.datastore.repository.support;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for the Datastore Repository factory bean.
 *
 * @author Chengyuan Zhao
 */
public class DatastoreRepositoryFactoryBeanTests {

	private DatastoreRepositoryFactoryBean<Object, String> datastoreRepositoryFactoryBean;

	private DatastoreMappingContext datastoreMappingContext = new DatastoreMappingContext();

	private DatastoreTemplate datastoreTemplate = mock(DatastoreTemplate.class);

	@Before
	public void setUp() {
		this.datastoreRepositoryFactoryBean = new DatastoreRepositoryFactoryBean(
				DatastoreRepository.class);
		this.datastoreRepositoryFactoryBean
				.setDatastoreMappingContext(this.datastoreMappingContext);
		this.datastoreRepositoryFactoryBean.setDatastoreTemplate(this.datastoreTemplate);
	}

	@Test
	public void createRepositoryFactoryTest() {
		RepositoryFactorySupport factory = this.datastoreRepositoryFactoryBean
				.createRepositoryFactory();
		assertThat(factory.getClass()).isEqualTo(DatastoreRepositoryFactory.class);
	}
}
