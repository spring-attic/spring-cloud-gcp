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

import org.springframework.beans.BeansException;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * @author Ray Tsang
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class SpannerRepositoryFactoryBean<T extends Repository<S, ID>, S, ID> extends
		RepositoryFactoryBeanSupport<T, S, ID> implements
		ApplicationContextAware {

	private SpannerMappingContext spannerMappingContext;

	private SpannerTemplate spannerTemplate;

	private ApplicationContext applicationContext;

	/**
	 * Creates a new {@link SpannerRepositoryFactoryBean} for the given repository interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 */
	SpannerRepositoryFactoryBean(Class<T> repositoryInterface) {
		super(repositoryInterface);
	}

	public void setSpannerTemplate(SpannerTemplate spannerTemplate) {
		this.spannerTemplate = spannerTemplate;
	}

	public void setSpannerMappingContext(SpannerMappingContext mappingContext) {
		super.setMappingContext(mappingContext);
		this.spannerMappingContext = mappingContext;
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory() {
		SpannerRepositoryFactory spannerRepositoryFactory = new SpannerRepositoryFactory(
				this.spannerMappingContext,
				this.spannerTemplate);
		spannerRepositoryFactory.setApplicationContext(this.applicationContext);
		return spannerRepositoryFactory;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
