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

package org.springframework.cloud.gcp.pubsub.integration.outbound;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.cloud.gcp.pubsub.core.publisher.PubSubPublisherOperations;
import org.springframework.cloud.gcp.pubsub.integration.PubSubHeaderMapper;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * Outbound channel adapter to publish messages to Google Cloud Pub/Sub.
 *
 * <p>It delegates Google Cloud Pub/Sub interaction to
 * {@link org.springframework.cloud.gcp.pubsub.core.PubSubTemplate}.
 *
 * @author João André Martins
 * @author Mike Eltsufin
 */
public class PubSubMessageHandler extends AbstractMessageHandler {

	private static final long DEFAULT_PUBLISH_TIMEOUT = 10000;

	private final PubSubPublisherOperations pubSubPublisherOperations;

	private Expression topicExpression;

	private boolean sync;

	private EvaluationContext evaluationContext;

	private Expression publishTimeoutExpression = new ValueExpression<>(DEFAULT_PUBLISH_TIMEOUT);

	private ListenableFutureCallback<String> publishCallback;

	private HeaderMapper<Map<String, String>> headerMapper = new PubSubHeaderMapper();

	public PubSubMessageHandler(PubSubPublisherOperations pubSubPublisherOperations, String topic) {
		Assert.notNull(pubSubPublisherOperations, "Pub/Sub publisher template can't be null.");
		Assert.hasText(topic, "Pub/Sub topic can't be null or empty.");

		this.pubSubPublisherOperations = pubSubPublisherOperations;
		this.topicExpression = new LiteralExpression(topic);
	}

	public boolean isSync() {
		return this.sync;
	}

	/**
	 * Set publish method to be synchronous or asynchronous.
	 *
	 * <p>Publish is asynchronous be default.
	 * @param sync true for synchronous, false for asynchronous
	 */
	public void setSync(boolean sync) {
		this.sync = sync;
	}

	public Expression getPublishTimeoutExpression() {
		return this.publishTimeoutExpression;
	}

	/**
	 * Set the SpEL expression to evaluate a timeout in milliseconds for a synchronous publish call
	 * to Google Cloud Pub/Sub.
	 * @param publishTimeoutExpression the {@link Expression} for the publish timeout in
	 *                                 milliseconds
	 */
	public void setPublishTimeoutExpression(Expression publishTimeoutExpression) {
		Assert.notNull(publishTimeoutExpression, "Publish timeout expression can't be null.");
		this.publishTimeoutExpression = publishTimeoutExpression;
	}

	/**
	 * Set the SpEL expression to evaluate a timeout in milliseconds for a synchronous publish call
	 * to Google Cloud Pub/Sub from a string.
	 * @param publishTimeoutExpression a string with an expression for the publish timeout in
	 *                                milliseconds
	 */
	public void setPublishTimeoutExpressionString(String publishTimeoutExpression) {
		Assert.notNull(publishTimeoutExpression, "Publish timeout expression can't be null.");
		setPublishTimeoutExpression(EXPRESSION_PARSER.parseExpression(publishTimeoutExpression));
	}

	/**
	 * Set the timeout in milliseconds for a synchronous publish call to Google Cloud Pub/Sub.
	 * @param timeoutMillis timeout in milliseconds
	 */
	public void setPublishTimeout(long timeoutMillis) {
		setPublishTimeoutExpression(new ValueExpression<>(timeoutMillis));
	}

	protected ListenableFutureCallback<String> getPublishCallback() {
		return this.publishCallback;
	}

	/**
	 * Set the callback to be activated when the publish call resolves.
	 * @param publishCallback callback for the publish future
	 */
	public void setPublishCallback(ListenableFutureCallback<String> publishCallback) {
		this.publishCallback = publishCallback;
	}

	public Expression getTopicExpression() {
		return this.topicExpression;
	}

	/**
	 * Set the SpEL expression for the topic this adapter sends messages to.
	 * @param topicExpression the SpEL expression representing the topic name
	 */
	public void setTopicExpression(Expression topicExpression) {
		this.topicExpression = topicExpression;
	}

	/**
	 * Set the topic where this adapter sends messages to.
	 * @param topic topic name
	 */
	public void setTopic(String topic) {
		Assert.hasText(topic, "The topic can't be null or empty");
		this.topicExpression = new LiteralExpression(topic);
	}

	/**
	 * Set the topic expression string that is evaluated into an actual expression.
	 * @param topicExpressionString topic expression string
	 */
	public void setTopicExpressionString(String topicExpressionString) {
		this.topicExpression = this.EXPRESSION_PARSER.parseExpression(topicExpressionString);
	}

	/**
	 * Set the header mapper to map headers from {@link Message} into outbound
	 * {@link com.google.pubsub.v1.PubsubMessage}.
	 * @param headerMapper the header mapper
	 */
	public void setHeaderMapper(HeaderMapper<Map<String, String>> headerMapper) {
		Assert.notNull(headerMapper, "The header mapper can't be null.");
		this.headerMapper = headerMapper;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object payload = message.getPayload();
		String topic = message.getHeaders().containsKey(GcpPubSubHeaders.TOPIC)
				? message.getHeaders().get(GcpPubSubHeaders.TOPIC, String.class)
				: this.topicExpression.getValue(this.evaluationContext, message, String.class);

		ListenableFuture<String> pubsubFuture;

		Map<String, String> headers = new HashMap<>();
		this.headerMapper.fromHeaders(message.getHeaders(), headers);

		pubsubFuture = this.pubSubPublisherOperations.publish(topic, payload, headers);

		if (this.publishCallback != null) {
			pubsubFuture.addCallback(this.publishCallback);
		}

		if (this.sync) {
			Long timeout = this.publishTimeoutExpression.getValue(
					this.evaluationContext, message, Long.class);
			if (timeout == null || timeout < 0) {
				pubsubFuture.get();
			}
			else {
				pubsubFuture.get(timeout, TimeUnit.MILLISECONDS);
			}
		}
	}

	@Override
	protected void onInit()  {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}
}
