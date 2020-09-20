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

package org.springframework.cloud.gcp.autoconfigure.firestore;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gcp.data.firestore.FirestoreReactiveRepository;
import org.springframework.cloud.gcp.data.firestore.repository.config.FirestoreRepositoryConfigurationExtension;
import org.springframework.cloud.gcp.data.firestore.repository.support.FirestoreRepositoryFactoryBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Automatically enables Firestore repositories support.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.2
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(FirestoreReactiveRepository.class)
@ConditionalOnMissingBean({ FirestoreRepositoryFactoryBean.class,
		FirestoreRepositoryConfigurationExtension.class })
@ConditionalOnProperty(value = "spring.cloud.gcp.firestore.enabled", matchIfMissing = true)
@Import({ FirestoreRepositoriesAutoConfigureRegistrar.class })
@AutoConfigureBefore(GcpFirestoreAutoConfiguration.class)
public class FirestoreRepositoriesAutoConfiguration {
}
