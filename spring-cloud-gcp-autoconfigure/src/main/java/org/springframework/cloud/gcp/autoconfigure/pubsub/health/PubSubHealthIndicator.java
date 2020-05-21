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

package org.springframework.cloud.gcp.autoconfigure.pubsub.health;

import java.util.UUID;

import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.google.api.gax.rpc.StatusCode.Code;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.util.Assert;

/**
 * Default implemenation of
 * {@link org.springframework.boot.actuate.health.HealthIndicator} for Pub/Sub. Validates
 * if connection is successful by looking for a random generated subscription name that
 * won't be found. If no subscription is found we know the client is able to connect to
 * GCP Pub/Sub APIs.
 *
 * @author Vinicius Carvalho
 *
 * @since 1.2.2
 */
public class PubSubHealthIndicator extends AbstractHealthIndicator {

	private final PubSubTemplate pubSubTemplate;

	public PubSubHealthIndicator(PubSubTemplate pubSubTemplate) {
		super("Failed to connect to Pub/Sub APIs. Check your credentials and verify you have proper access to the service.");
		Assert.notNull(pubSubTemplate, "PubSubTemplate can't be null");
		this.pubSubTemplate = pubSubTemplate;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		try {
			this.pubSubTemplate.pull("subscription-" + UUID.randomUUID().toString(), 1, true);
		}
		catch (ApiException aex) {
			Code errorCode = aex.getStatusCode().getCode();
			if (errorCode == StatusCode.Code.NOT_FOUND || errorCode == Code.PERMISSION_DENIED) {
				builder.up();
			}
			else {
				builder.withException(aex).down();
			}
		}
		catch (Exception e) {
			builder.withException(e).down();
		}
	}

}
