/*
 * Copyright 2019-2020 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.google.api.gax.grpc.GrpcStatusCode;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;
import io.grpc.Status.Code;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.actuate.health.Status;
import org.springframework.util.concurrent.ListenableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the Pub/Sub Health Indicator.
 *
 * @author Vinicius Carvalho
 * @author Patrik Hörlin
 */
@ExtendWith(MockitoExtension.class)
class PubSubHealthIndicatorTests {

	@Mock
	private PubSubTemplate pubSubTemplate;

	@Mock
	ListenableFuture<List<AcknowledgeablePubsubMessage>> future;

	@Test
	void healthUp_customSubscription() throws Exception {
		when(future.get(anyLong(), any())).thenReturn(Collections.emptyList());
		when(pubSubTemplate.pullAsync(anyString(), anyInt(), anyBoolean())).thenReturn(future);

		PubSubHealthIndicator healthIndicator = new PubSubHealthIndicator(pubSubTemplate, "test", 1000, true);
		assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.UP);
	}

	@Test
	void acknowledgeEnabled() throws Exception {
		AcknowledgeablePubsubMessage msg = mock(AcknowledgeablePubsubMessage.class);
		when(future.get(anyLong(), any())).thenReturn(Arrays.asList(msg));
		when(pubSubTemplate.pullAsync(anyString(), anyInt(), anyBoolean())).thenReturn(future);

		PubSubHealthIndicator healthIndicator = new PubSubHealthIndicator(pubSubTemplate, "test", 1000, true);
		healthIndicator.health();
		verify(msg).ack();
	}

	@Test
	void acknowledgeDisabled() throws Exception {
		AcknowledgeablePubsubMessage msg = mock(AcknowledgeablePubsubMessage.class);
		when(future.get(anyLong(), any())).thenReturn(Arrays.asList(msg));
		when(pubSubTemplate.pullAsync(anyString(), anyInt(), anyBoolean())).thenReturn(future);

		PubSubHealthIndicator healthIndicator = new PubSubHealthIndicator(pubSubTemplate, "test", 1000, false);
		healthIndicator.health();
		verify(msg, never()).ack();
	}

	void testHealth(Exception e, String customSubscription, Status expectedStatus) throws Exception {
		when(pubSubTemplate.pullAsync(anyString(), anyInt(), anyBoolean())).thenReturn(future);
		doThrow(e).when(future).get(anyLong(), any());

		PubSubHealthIndicator healthIndicator = new PubSubHealthIndicator(pubSubTemplate, customSubscription, 1000, true);
		assertThat(healthIndicator.health().getStatus()).isEqualTo(expectedStatus);
	}

	@ParameterizedTest
	@ValueSource(strings = {"NOT_FOUND", "PERMISSION_DENIED"})
	void randomSubscription_expectedError(String code) throws Exception {
		Exception e = new ExecutionException(
				new ApiException(null, GrpcStatusCode.of(io.grpc.Status.Code.valueOf(code)), false));
		testHealth(e, null, Status.UP);
	}

	@Test
	void randomSubscription_unexpectedErrorCode() throws Exception {
		Exception e = new ExecutionException(
				new ApiException(null, GrpcStatusCode.of(Code.ABORTED), false));
		testHealth(e, null, Status.DOWN);
	}

	@Test
	void randomSubscription_NotApiExcpetion() throws Exception {
		ExecutionException e = new ExecutionException("Exception", new IllegalArgumentException());
		testHealth(e, null, Status.DOWN);
	}

	@ParameterizedTest
	@ValueSource(strings = {"NOT_FOUND", "PERMISSION_DENIED"})
	void customSubscription_ApiException(String code) throws Exception {
		Exception e = new ExecutionException(
				new ApiException(null, GrpcStatusCode.of(io.grpc.Status.Code.valueOf(code)), false));
		testHealth(e, "testSubscription", Status.DOWN);
	}

	@Test
	void customSubscription_ExecutionException_NotApiException() throws Exception {
		ExecutionException e = new ExecutionException("Exception", new IllegalArgumentException());
		testHealth(e, "testSubscription", Status.DOWN);
	}

	@Test
	void customSubscription_InterruptedException() throws Exception {
		Exception e = new InterruptedException("Interrupted");
		testHealth(e, "testSubscription", Status.UNKNOWN);
	}

	@Test
	void customSubscription_TimeoutException() throws Exception {
		Exception e = new TimeoutException("Timed out waiting for result");
		testHealth(e, "testSubscription", Status.UNKNOWN);
	}

	@Test
	void customSubscription_RuntimeException() throws Exception {
		Exception e = new RuntimeException("Runtime error");
		testHealth(e, "testSubscription", Status.DOWN);
	}

	@Test
	void validateHealth() throws Exception {
		doThrow(new RuntimeException()).when(future).get(anyLong(), any());
		when(pubSubTemplate.pullAsync(anyString(), anyInt(), anyBoolean())).thenReturn(future);

		PubSubHealthIndicator healthIndicator = new PubSubHealthIndicator(pubSubTemplate, "test", 1000, true);
		assertThatThrownBy(() -> healthIndicator.validateHealthCheck()).isInstanceOf(BeanInitializationException.class);
	}
}
