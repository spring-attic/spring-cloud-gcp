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
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * @author Ray Tsang
 * @author Chengyuan Zhao
 */
public class SpannerRepositoryFactoryBean extends RepositoryFactoryBeanSupport {

	private SpannerMappingContext spannerMappingContext;

	private SpannerOperations spannerOperations;

	/**
	 * Creates a new {@link SpannerRepositoryFactoryBean} for the given repository interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 */
	public SpannerRepositoryFactoryBean(Class<?> repositoryInterface) {
		super(repositoryInterface);
	}

	public void setSpannerOperations(SpannerOperations operations) {
		this.spannerOperations = operations;
	}

	public void setSpannerMappingContext(SpannerMappingContext mappingContext) {
		super.setMappingContext(mappingContext);
		this.spannerMappingContext = mappingContext;
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory() {
		return new SpannerRepositoryFactory(this.spannerMappingContext,
				this.spannerOperations);
	}
}
