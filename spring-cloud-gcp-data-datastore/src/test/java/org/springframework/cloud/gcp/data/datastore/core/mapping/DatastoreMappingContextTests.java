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

package org.springframework.cloud.gcp.data.datastore.core.mapping;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for the `DatastoreMappingContext`.
 *
 * @author Chengyuan Zhao
 */
public class DatastoreMappingContextTests {

	@Test
	public void testApplicationContextPassing() {
		DatastorePersistentEntityImpl mockEntity = mock(
				DatastorePersistentEntityImpl.class);
		DatastoreMappingContext context = createDatastoreMappingContextWith(mockEntity);
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		context.setApplicationContext(applicationContext);

		context.createPersistentEntity(ClassTypeInformation.from(Object.class));

		verify(mockEntity, times(1)).setApplicationContext(eq(applicationContext));
	}

	@Test
	public void testApplicationContextIsNotSet() {
		DatastorePersistentEntityImpl mockEntity = mock(
				DatastorePersistentEntityImpl.class);
		DatastoreMappingContext context = createDatastoreMappingContextWith(mockEntity);

		context.createPersistentEntity(ClassTypeInformation.from(Object.class));

		verifyZeroInteractions(mockEntity);
	}

	private DatastoreMappingContext createDatastoreMappingContextWith(
			DatastorePersistentEntityImpl mockEntity) {
		return new DatastoreMappingContext() {
			@Override
			@SuppressWarnings("unchecked")
			protected DatastorePersistentEntityImpl constructPersistentEntity(
					TypeInformation typeInformation) {
				return mockEntity;
			}
		};
	}

}
