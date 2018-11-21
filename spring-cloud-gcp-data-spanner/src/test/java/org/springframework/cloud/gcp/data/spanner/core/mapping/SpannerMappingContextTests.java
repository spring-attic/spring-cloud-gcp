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

package org.springframework.cloud.gcp.data.spanner.core.mapping;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.context.ApplicationContext;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
public class SpannerMappingContextTests {

	@Test
	public void testNullSetFieldNamingStrategy() {
		SpannerMappingContext context = new SpannerMappingContext();

		context.setFieldNamingStrategy(null);
		assertEquals(PropertyNameFieldNamingStrategy.INSTANCE,
				context.getFieldNamingStrategy());
	}

	@Test
	public void testSetFieldNamingStrategy() {
		SpannerMappingContext context = new SpannerMappingContext();
		FieldNamingStrategy strat = mock(FieldNamingStrategy.class);
		context.setFieldNamingStrategy(strat);
		assertSame(strat, context.getFieldNamingStrategy());
	}

	@Test
	public void testApplicationContextPassing() {
		SpannerPersistentEntityImpl mockEntity = mock(SpannerPersistentEntityImpl.class);
		SpannerMappingContext context = createSpannerMappingContextWith(mockEntity);
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		context.setApplicationContext(applicationContext);

		context.createPersistentEntity(ClassTypeInformation.from(Object.class));

		verify(mockEntity, times(1)).setApplicationContext(eq(applicationContext));
	}


	@Test
	public void testApplicationContextIsNotSet() {
		SpannerPersistentEntityImpl mockEntity = mock(SpannerPersistentEntityImpl.class);
		SpannerMappingContext context = createSpannerMappingContextWith(mockEntity);

		context.createPersistentEntity(ClassTypeInformation.from(Object.class));

		verifyZeroInteractions(mockEntity);
	}


	private SpannerMappingContext createSpannerMappingContextWith(
			SpannerPersistentEntityImpl mockEntity) {
		return new SpannerMappingContext() {
			@Override
			@SuppressWarnings("unchecked")
			protected  SpannerPersistentEntityImpl constructPersistentEntity(
					TypeInformation typeInformation) {
				return mockEntity;
			}
		};
	}

}
