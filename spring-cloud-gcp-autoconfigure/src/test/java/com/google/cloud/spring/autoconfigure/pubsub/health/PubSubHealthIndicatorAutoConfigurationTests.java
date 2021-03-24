/*
 * Copyright 2018-2020 the original author or authors.
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

package com.google.cloud.spring.autoconfigure.pubsub.health;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.grpc.GrpcStatusCode;
import com.google.api.gax.rpc.ApiException;
import com.google.auth.Credentials;
import com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;
import org.junit.Test;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.concurrent.ListenableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for Pub/Sub Health Indicator autoconfiguration.
 *
 * @author Elena Felder
 * @author Patrik Hörlin
 */
public class PubSubHealthIndicatorAutoConfigurationTests {

	private static final Pattern UUID_PATTERN =
			Pattern.compile("spring-cloud-gcp-healthcheck-[a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8}");

	private ApplicationContextRunner baseContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PubSubHealthIndicatorAutoConfiguration.class,
					GcpPubSubAutoConfiguration.class))
			.withBean(GcpProjectIdProvider.class,  () -> () -> "fake project")
			.withBean(CredentialsProvider.class, () -> () -> mock(Credentials.class));

	@SuppressWarnings("unchecked")
	@Test
	public void healthIndicatorPresent_defaults() throws Exception {
		PubSubTemplate mockPubSubTemplate = mock(PubSubTemplate.class);
		ListenableFuture<List<AcknowledgeablePubsubMessage>> future = mock(ListenableFuture.class);

		when(future.get(anyLong(), any())).thenReturn(Collections.emptyList());
		when(mockPubSubTemplate.pullAsync(anyString(), anyInt(), anyBoolean())).thenReturn(future);

		this.baseContextRunner
				.withBean("pubSubTemplate", PubSubTemplate.class, () -> mockPubSubTemplate)
				.run(ctx -> {
					PubSubHealthIndicator healthIndicator = ctx.getBean(PubSubHealthIndicator.class);
					assertThat(healthIndicator).isNotNull();
					assertThat(healthIndicator.getSubscription()).matches(UUID_PATTERN);
					assertThat(healthIndicator.getTimeoutMillis()).isEqualTo(1000);
					assertThat(healthIndicator.isAcknowledgeMessages()).isFalse();
					assertThat(healthIndicator.isSpecifiedSubscription()).isFalse();
					verify(mockPubSubTemplate).pullAsync(healthIndicator.getSubscription(), 1, true);
					verify(future).get(healthIndicator.getTimeoutMillis(), TimeUnit.MILLISECONDS);
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void healthIndicatorPresent_customConfig() throws Exception {
		PubSubTemplate mockPubSubTemplate = mock(PubSubTemplate.class);
		ListenableFuture<List<AcknowledgeablePubsubMessage>> future = mock(ListenableFuture.class);

		when(future.get(anyLong(), any())).thenReturn(Collections.emptyList());
		when(mockPubSubTemplate.pullAsync(anyString(), anyInt(), anyBoolean())).thenReturn(future);

		this.baseContextRunner
				.withBean("pubSubTemplate", PubSubTemplate.class, () -> mockPubSubTemplate)
				.withPropertyValues(
						"management.health.pubsub.enabled=true",
						"spring.cloud.gcp.pubsub.health.subscription=test",
						"spring.cloud.gcp.pubsub.health.timeout-millis=1500",
						"spring.cloud.gcp.pubsub.health.acknowledgeMessages=true")
				.run(ctx -> {
					PubSubHealthIndicator healthIndicator = ctx.getBean(PubSubHealthIndicator.class);
					assertThat(healthIndicator).isNotNull();
					assertThat(healthIndicator.getSubscription()).isEqualTo("test");
					assertThat(healthIndicator.getTimeoutMillis()).isEqualTo(1500);
					assertThat(healthIndicator.isAcknowledgeMessages()).isTrue();
					assertThat(healthIndicator.isSpecifiedSubscription()).isTrue();
					verify(mockPubSubTemplate).pullAsync(healthIndicator.getSubscription(), 1, true);
					verify(future).get(healthIndicator.getTimeoutMillis(), TimeUnit.MILLISECONDS);
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void compositeHealthIndicatorPresentMultiplePubSubTemplate() throws Exception {
		PubSubTemplate mockPubSubTemplate1 = mock(PubSubTemplate.class);
		PubSubTemplate mockPubSubTemplate2 = mock(PubSubTemplate.class);
		ListenableFuture<List<AcknowledgeablePubsubMessage>> future = mock(ListenableFuture.class);

		when(future.get(anyLong(), any())).thenReturn(Collections.emptyList());
		when(mockPubSubTemplate1.pullAsync(anyString(), anyInt(), anyBoolean())).thenReturn(future);
		when(mockPubSubTemplate2.pullAsync(anyString(), anyInt(), anyBoolean())).thenReturn(future);

		this.baseContextRunner
				.withBean("pubSubTemplate1", PubSubTemplate.class, () -> mockPubSubTemplate1)
				.withBean("pubSubTemplate2", PubSubTemplate.class, () -> mockPubSubTemplate2)
				.withPropertyValues(
						"management.health.pubsub.enabled=true",
						"spring.cloud.gcp.pubsub.health.subscription=test",
						"spring.cloud.gcp.pubsub.health.timeout-millis=1500",
						"spring.cloud.gcp.pubsub.health.acknowledgeMessages=true")
				.run(ctx -> {
					assertThatThrownBy(() -> ctx.getBean(PubSubHealthIndicator.class))
							.isInstanceOf(NoSuchBeanDefinitionException.class);
					CompositeHealthContributor healthContributor = ctx.getBean("pubSubHealthContributor", CompositeHealthContributor.class);
					assertThat(healthContributor).isNotNull();
					assertThat(healthContributor.stream()).hasSize(2);
					assertThat(healthContributor.stream().map(c -> c.getName()))
							.containsExactlyInAnyOrder("pubSubTemplate1", "pubSubTemplate2");
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void apiExceptionWhenValidating_userSubscriptionSpecified_healthAutoConfigurationFails() throws Exception {
		PubSubHealthIndicatorProperties properties = new PubSubHealthIndicatorProperties();
		PubSubHealthIndicatorAutoConfiguration p = new PubSubHealthIndicatorAutoConfiguration(properties);
		properties.setSubscription("test");

		PubSubTemplate mockPubSubTemplate = mock(PubSubTemplate.class);
		ListenableFuture<List<AcknowledgeablePubsubMessage>> future = mock(ListenableFuture.class);
		Exception e = new ApiException(new IllegalStateException("Illegal State"), GrpcStatusCode.of(io.grpc.Status.Code.NOT_FOUND), false);

		when(mockPubSubTemplate.pullAsync(anyString(), anyInt(), anyBoolean())).thenReturn(future);
		doThrow(new ExecutionException(e)).when(future).get(anyLong(), any());

		Map<String, PubSubTemplate> pubSubTemplates = Collections.singletonMap("pubSubTemplate", mockPubSubTemplate);
		assertThatThrownBy(() -> p.pubSubHealthContributor(pubSubTemplates))
				.isInstanceOf(BeanInitializationException.class);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void apiExceptionWhenValidating_userSubscriptionNotSpecified_healthAutoConfigurationSucceeds() throws Exception {
		PubSubHealthIndicatorProperties properties = new PubSubHealthIndicatorProperties();
		PubSubHealthIndicatorAutoConfiguration p = new PubSubHealthIndicatorAutoConfiguration(properties);

		PubSubTemplate mockPubSubTemplate = mock(PubSubTemplate.class);
		ListenableFuture<List<AcknowledgeablePubsubMessage>> future = mock(ListenableFuture.class);
		Exception e = new ApiException(new IllegalStateException("Illegal State"), GrpcStatusCode.of(io.grpc.Status.Code.NOT_FOUND), false);

		when(mockPubSubTemplate.pullAsync(anyString(), anyInt(), anyBoolean())).thenReturn(future);
		doThrow(new ExecutionException(e)).when(future).get(anyLong(), any());

		Map<String, PubSubTemplate> pubSubTemplates = Collections.singletonMap("pubSubTemplate", mockPubSubTemplate);
		assertThatCode(() -> p.pubSubHealthContributor(pubSubTemplates)).doesNotThrowAnyException();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void runtimeExceptionWhenValidating_healthAutoConfigurationFails() throws Exception {
		PubSubHealthIndicatorProperties properties = new PubSubHealthIndicatorProperties();
		PubSubHealthIndicatorAutoConfiguration p = new PubSubHealthIndicatorAutoConfiguration(properties);

		PubSubTemplate mockPubSubTemplate = mock(PubSubTemplate.class);
		ListenableFuture<List<AcknowledgeablePubsubMessage>> future = mock(ListenableFuture.class);
		Exception e = new RuntimeException("Runtime exception");

		when(mockPubSubTemplate.pullAsync(anyString(), anyInt(), anyBoolean())).thenReturn(future);
		doThrow(e).when(future).get(anyLong(), any());

		Map<String, PubSubTemplate> pubSubTemplates = Collections.singletonMap("pubSubTemplate", mockPubSubTemplate);
		assertThatThrownBy(() -> p.pubSubHealthContributor(pubSubTemplates)).isInstanceOf(BeanInitializationException.class);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void interruptedExceptionWhenValidating_healthAutoConfigurationFails() throws Exception {
		PubSubHealthIndicatorProperties properties = new PubSubHealthIndicatorProperties();
		PubSubHealthIndicatorAutoConfiguration p = new PubSubHealthIndicatorAutoConfiguration(properties);

		PubSubTemplate mockPubSubTemplate = mock(PubSubTemplate.class);
		ListenableFuture<List<AcknowledgeablePubsubMessage>> future = mock(ListenableFuture.class);
		InterruptedException e = new InterruptedException("interrupted");

		when(mockPubSubTemplate.pullAsync(anyString(), anyInt(), anyBoolean())).thenReturn(future);
		doThrow(e).when(future).get(anyLong(), any());

		Map<String, PubSubTemplate> pubSubTemplates = Collections.singletonMap("pubSubTemplate", mockPubSubTemplate);
		assertThatThrownBy(() -> p.pubSubHealthContributor(pubSubTemplates)).isInstanceOf(BeanInitializationException.class);
	}

	@Test
	public void healthCheckConfigurationBacksOffWhenHealthIndicatorBeanPresent() {
		PubSubHealthIndicator userHealthIndicator = mock(PubSubHealthIndicator.class);

		this.baseContextRunner
				.withBean("pubSubTemplate1", PubSubTemplate.class, () -> mock(PubSubTemplate.class))
				.withBean("pubSubTemplate2", PubSubTemplate.class, () -> mock(PubSubTemplate.class))
				.withBean(PubSubHealthIndicator.class, () -> userHealthIndicator)
				.withPropertyValues("management.health.pubsub.enabled=true")
				.run(ctx -> {
					assertThat(ctx).doesNotHaveBean("pubSubHealthContributor");
					assertThat(ctx).hasSingleBean(PubSubHealthIndicator.class);
					assertThat(ctx.getBean(PubSubHealthIndicator.class)).isEqualTo(userHealthIndicator);
				});
	}

	@Test
	public void healthIndicatorDisabledWhenPubSubTurnedOff() {
		this.baseContextRunner
				.withPropertyValues(
						"management.health.pubsub.enabled=true",
						"spring.cloud.gcp.pubsub.enabled=false")
				.run(ctx -> {
					assertThat(ctx.getBeansOfType(PubSubHealthIndicator.class)).isEmpty();
				});
	}
}
