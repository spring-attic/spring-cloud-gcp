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

package org.springframework.cloud.gcp.autoconfigure.core.environment;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.cloud.gcp.core.GcpEnvironment;
import org.springframework.cloud.gcp.core.GcpEnvironmentProvider;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OnGcpEnvironmentConditionTests {

	@Mock
	AnnotatedTypeMetadata mockMetadata;

	@Mock
	ConditionContext mockContext;

	@Mock
	ConfigurableListableBeanFactory mockBeanFactory;

	@Mock
	GcpEnvironmentProvider mockGcpEnvironmentProvider;

	@Before
	public void setUp() {
		when(this.mockContext.getBeanFactory()).thenReturn(this.mockBeanFactory);
		when(this.mockBeanFactory.getBean(GcpEnvironmentProvider.class)).thenReturn(this.mockGcpEnvironmentProvider);
	}

	@Test
	public void testNoEnvironmentsMatchWhenMissingEnvironmentProvider() {
		OnGcpEnvironmentCondition onGcpEnvironmentCondition = new OnGcpEnvironmentCondition();

		when(this.mockBeanFactory.getBean(GcpEnvironmentProvider.class))
				.thenThrow(new NoSuchBeanDefinitionException("no environment"));
		ConditionOutcome outcome = onGcpEnvironmentCondition.getMatchOutcome(this.mockContext, this.mockMetadata);
		assertFalse(outcome.isMatch());
		assertThat(outcome.getMessage()).startsWith("GcpEnvironmentProvider not found");
	}

	@Test (expected = ClassCastException.class)
	public void testExceptionThrownWhenWrongAttributeType() {
		setUpAnnotationValue("invalid environment value");
		OnGcpEnvironmentCondition onGcpEnvironmentCondition = new OnGcpEnvironmentCondition();
		onGcpEnvironmentCondition.getMatchOutcome(this.mockContext, this.mockMetadata);
	}

	@Test
	public void testNegativeOutcome() {
		setUpAnnotationValue(GcpEnvironment.COMPUTE_ENGINE);
		when(this.mockGcpEnvironmentProvider.isCurrentEnvironment(GcpEnvironment.COMPUTE_ENGINE)).thenReturn(false);

		OnGcpEnvironmentCondition onGcpEnvironmentCondition = new OnGcpEnvironmentCondition();
		ConditionOutcome outcome = onGcpEnvironmentCondition.getMatchOutcome(this.mockContext, this.mockMetadata);

		assertFalse(outcome.isMatch());
		assertThat(outcome.getMessage()).isEqualTo("Application is not running on COMPUTE_ENGINE");
	}

	@Test
	public void testPositiveOutcome() {
		setUpAnnotationValue(GcpEnvironment.COMPUTE_ENGINE);
		when(this.mockGcpEnvironmentProvider.isCurrentEnvironment(GcpEnvironment.COMPUTE_ENGINE)).thenReturn(true);

		OnGcpEnvironmentCondition onGcpEnvironmentCondition = new OnGcpEnvironmentCondition();
		ConditionOutcome outcome = onGcpEnvironmentCondition.getMatchOutcome(this.mockContext, this.mockMetadata);

		assertTrue(outcome.isMatch());
		assertThat(outcome.getMessage()).isEqualTo("Application is running on COMPUTE_ENGINE");
	}

	private void setUpAnnotationValue(Object environment) {
		when(this.mockMetadata.getAnnotationAttributes(ConditionalOnGcpEnvironment.class.getName())).thenReturn(
				ImmutableMap.of("value", environment)
		);
	}
}
