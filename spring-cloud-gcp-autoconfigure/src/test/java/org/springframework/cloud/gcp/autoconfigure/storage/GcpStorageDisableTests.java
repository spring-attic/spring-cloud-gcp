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

package org.springframework.cloud.gcp.autoconfigure.storage;

import com.google.cloud.storage.Storage;
import org.junit.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Verifies that GCP Storage may be disabled via the property:
 * "spring.cloud.gcp.storage.enabled=false"
 *
 * @author Daniel Zou
 */
public class GcpStorageDisableTests {

	ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GcpStorageAutoConfiguration.class))
			.withPropertyValues("spring.cloud.gcp.storage.enabled=false");

	@Test
	public void testStorageBeanIsNotProvided() {
		this.contextRunner.run(context -> {
			Throwable thrown = catchThrowable(() -> context.getBean(Storage.class));
			assertThat(thrown).isInstanceOf(NoSuchBeanDefinitionException.class);
		});
	}
}
