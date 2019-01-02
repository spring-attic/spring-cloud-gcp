/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.autoconfigure.core.environment;

import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
import static org.mockito.Mockito.when;

/**
 * Tests for on-GCP environment conditions.
 *
 * @author Elena Felder
 *
 * @since 1.1
 */
@RunWith(MockitoJUnitRunner.class)
public class OnGcpEnvironmentConditionTests {

	/**
	 * used to check exception messages and types.
	 */
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

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

		this.expectedException.expect(NoSuchBeanDefinitionException.class);
		this.expectedException.expectMessage("No bean named 'no environment' available");

		OnGcpEnvironmentCondition onGcpEnvironmentCondition = new OnGcpEnvironmentCondition();

		setUpAnnotationValue(new GcpEnvironment[] { GcpEnvironment.UNKNOWN });
		when(this.mockBeanFactory.getBean(GcpEnvironmentProvider.class))
				.thenThrow(new NoSuchBeanDefinitionException("no environment"));
		onGcpEnvironmentCondition.getMatchOutcome(this.mockContext, this.mockMetadata);
	}

	@Test
	public void testExceptionThrownWhenWrongAttributeType() {

		this.expectedException.expect(ClassCastException.class);
		this.expectedException.expectMessage("java.lang.String cannot be cast to " +
				"[Lorg.springframework.cloud.gcp.core.GcpEnvironment;");

		setUpAnnotationValue("invalid type");
		OnGcpEnvironmentCondition onGcpEnvironmentCondition = new OnGcpEnvironmentCondition();
		onGcpEnvironmentCondition.getMatchOutcome(this.mockContext, this.mockMetadata);
	}

	@Test
	public void testNegativeOutcome() {
		setUpAnnotationValue(new GcpEnvironment[] { GcpEnvironment.COMPUTE_ENGINE });
		when(this.mockGcpEnvironmentProvider.getCurrentEnvironment()).thenReturn(GcpEnvironment.UNKNOWN);

		OnGcpEnvironmentCondition onGcpEnvironmentCondition = new OnGcpEnvironmentCondition();
		ConditionOutcome outcome = onGcpEnvironmentCondition.getMatchOutcome(this.mockContext, this.mockMetadata);

		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage()).isEqualTo("Application is not running on any of COMPUTE_ENGINE");
	}

	@Test
	public void testNegativeOutcomeForMultipleEnvironments() {
		setUpAnnotationValue(new GcpEnvironment[] { GcpEnvironment.COMPUTE_ENGINE, GcpEnvironment.KUBERNETES_ENGINE });
		when(this.mockGcpEnvironmentProvider.getCurrentEnvironment()).thenReturn(GcpEnvironment.UNKNOWN);

		OnGcpEnvironmentCondition onGcpEnvironmentCondition = new OnGcpEnvironmentCondition();
		ConditionOutcome outcome = onGcpEnvironmentCondition.getMatchOutcome(this.mockContext, this.mockMetadata);

		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage())
				.isEqualTo("Application is not running on any of COMPUTE_ENGINE, KUBERNETES_ENGINE");
	}

	@Test
	public void testPositiveOutcomeForMultipleEnvironments() {
		setUpAnnotationValue(new GcpEnvironment[] { GcpEnvironment.COMPUTE_ENGINE, GcpEnvironment.KUBERNETES_ENGINE });
		when(this.mockGcpEnvironmentProvider.getCurrentEnvironment()).thenReturn(GcpEnvironment.KUBERNETES_ENGINE);

		OnGcpEnvironmentCondition onGcpEnvironmentCondition = new OnGcpEnvironmentCondition();
		ConditionOutcome outcome = onGcpEnvironmentCondition.getMatchOutcome(this.mockContext, this.mockMetadata);

		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage()).isEqualTo("Application is running on KUBERNETES_ENGINE");
	}

	@Test
	public void testPositiveOutcome() {
		setUpAnnotationValue(new GcpEnvironment[] { GcpEnvironment.COMPUTE_ENGINE });
		when(this.mockGcpEnvironmentProvider.getCurrentEnvironment()).thenReturn(GcpEnvironment.COMPUTE_ENGINE);

		OnGcpEnvironmentCondition onGcpEnvironmentCondition = new OnGcpEnvironmentCondition();
		ConditionOutcome outcome = onGcpEnvironmentCondition.getMatchOutcome(this.mockContext, this.mockMetadata);

		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage()).isEqualTo("Application is running on COMPUTE_ENGINE");
	}

	private void setUpAnnotationValue(Object environments) {
		when(this.mockMetadata.getAnnotationAttributes(ConditionalOnGcpEnvironment.class.getName())).thenReturn(
				Collections.singletonMap("value", environments)
		);
	}
}
