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

package com.google.cloud.spring.autoconfigure.pubsub.health;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.google.api.gax.rpc.StatusCode.Code;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Default implementation of
 * {@link org.springframework.boot.actuate.health.HealthIndicator} for Pub/Sub. Validates
 * if connection is successful by pulling messages from the pubSubTemplate using
 * {@link PubSubTemplate#pullAsync(String, Integer, Boolean)}.
 *
 * <p>If a custom subscription has been specified, this health indicator will signal "up"
 * if messages are successfully pulled and (optionally) acknowledged <b>or</b> if a
 * successful pull is performed but no messages are returned from Pub/Sub.</p>
 *
 * <p>If no subscription has been specified, this health indicator will pull messages from a random subscription
 * that is expected not to exist. It will signal "up" if it is able to connect to GCP Pub/Sub APIs,
 * i.e. the pull results in a response of {@link StatusCode.Code#NOT_FOUND} or
 * {@link StatusCode.Code#PERMISSION_DENIED}.</p>
 *
 * <p>Note that messages pulled from the subscription will not be acknowledged, unless you
 * set the {@code acknowledgeMessages} option to "true". However, take care not to configure
 * a subscription that has a business impact, or leave the custom subscription out completely.
 *
 * @author Vinicius Carvalho
 * @author Patrik Hörlin
 *
 * @since 1.2.2
 */
public class PubSubHealthIndicator extends AbstractHealthIndicator {

	/**
	 * Template used when performing health check calls.
	 */
	private final PubSubTemplate pubSubTemplate;

	/**
	 * Indicates whether a user subscription has been configured.
	 */
	private final boolean specifiedSubscription;

	/**
	 * Subscription used when health checking.
	 */
	private final String subscription;

	/**
	 * Timeout when performing health check.
	 */
	private final long timeoutMillis;

	/**
	 * Whether pulled messages should be acknowledged.
	 */
	private final boolean acknowledgeMessages;

	public PubSubHealthIndicator(PubSubTemplate pubSubTemplate, String healthCheckSubscription, long timeoutMillis, boolean acknowledgeMessages) {
		super("Failed to connect to Pub/Sub APIs. Check your credentials and verify you have proper access to the service.");
		Assert.notNull(pubSubTemplate, "pubSubTemplate can't be null");
		this.pubSubTemplate = pubSubTemplate;
		this.specifiedSubscription = StringUtils.hasText(healthCheckSubscription);
		if (this.specifiedSubscription) {
			this.subscription = healthCheckSubscription;
		}
		else {
			this.subscription = "spring-cloud-gcp-healthcheck-" + UUID.randomUUID().toString();
		}
		this.timeoutMillis = timeoutMillis;
		this.acknowledgeMessages = acknowledgeMessages;
	}

	void validateHealthCheck() {
		doHealthCheck(
				() -> { },
				this::validationFailed,
				this::validationFailed);
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) {
		doHealthCheck(
				builder::up,
				builder::down,
				e -> builder.withException(e).unknown());
	}

	private void doHealthCheck(Runnable up, Consumer<Throwable> down, Consumer<Throwable> unknown) {
		try {
			pullMessage();
			up.run();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			unknown.accept(e);
		}
		catch (ExecutionException e) {
			if (isHealthyException(e)) {
				// ignore expected exceptions
				up.run();
			}
			else {
				down.accept(e);
			}
		}
		catch (TimeoutException e) {
			unknown.accept(e);
		}
		catch (Exception e) {
			down.accept(e);
		}
	}

	private void pullMessage() throws InterruptedException, ExecutionException, TimeoutException {
		ListenableFuture<List<AcknowledgeablePubsubMessage>> future = pubSubTemplate.pullAsync(this.subscription, 1, true);
		List<AcknowledgeablePubsubMessage> messages = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
		if (this.acknowledgeMessages) {
			messages.forEach(AcknowledgeablePubsubMessage::ack);
		}
	}

	boolean isHealthyException(ExecutionException e) {
		return !this.specifiedSubscription && isHealthyResponseForUnspecifiedSubscription(e);
	}

	private boolean isHealthyResponseForUnspecifiedSubscription(ExecutionException e) {
		Throwable t = e.getCause();
		if (t instanceof ApiException) {
			ApiException aex = (ApiException) t;
			Code errorCode = aex.getStatusCode().getCode();
			return errorCode == StatusCode.Code.NOT_FOUND || errorCode == Code.PERMISSION_DENIED;
		}
		return false;
	}

	private void validationFailed(Throwable e) {
		throw new BeanInitializationException("Validation of health indicator failed", e);
	}

	boolean isSpecifiedSubscription() {
		return specifiedSubscription;
	}

	String getSubscription() {
		return subscription;
	}

	long getTimeoutMillis() {
		return timeoutMillis;
	}

	boolean isAcknowledgeMessages() {
		return acknowledgeMessages;
	}
}
