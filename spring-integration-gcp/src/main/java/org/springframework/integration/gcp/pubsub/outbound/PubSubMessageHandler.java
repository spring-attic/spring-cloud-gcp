/*
 *  Copyright 2017 original author or authors.
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

package org.springframework.integration.gcp.pubsub.outbound;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

import org.springframework.cloud.gcp.pubsub.core.PubSubOperations;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * Outbound channel adapter to publish messages to Google Cloud Pub/Sub.
 *
 * <p>It delegates Google Cloud Pub/Sub interaction to {@link PubSubOperations}. It also converts
 * the {@link Message} payload into a {@link PubsubMessage} accepted by the Google Cloud Pub/Sub
 * Client Library. It supports synchronous and asynchronous sending.
 *
 * @author João André Martins
 */
public class PubSubMessageHandler extends AbstractMessageHandler {

	private static final long DEFAULT_PUBLISH_TIMEOUT = 10000;

	private final PubSubOperations pubSubTemplate;

	private MessageConverter messageConverter = new StringMessageConverter();

	private String topic;

	private boolean sync;

	private EvaluationContext evaluationContext;

	private Expression publishTimeoutExpression = new ValueExpression<>(DEFAULT_PUBLISH_TIMEOUT);

	private ListenableFutureCallback<String> publishCallback;

	public PubSubMessageHandler(PubSubOperations pubSubTemplate, String topic) {
		this.pubSubTemplate = pubSubTemplate;
		this.topic = topic;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object payload = message.getPayload();

		if (payload instanceof PubsubMessage) {
			this.pubSubTemplate.publish(this.topic, (PubsubMessage) payload);
			return;
		}

		ByteString pubsubPayload;

		if (payload instanceof byte[]) {
			pubsubPayload = ByteString.copyFrom((byte[]) payload);
		}
		else if (payload instanceof ByteString) {
			pubsubPayload = (ByteString) payload;
		}
		else {
			pubsubPayload =	ByteString.copyFrom(
					(String) this.messageConverter.fromMessage(message, String.class),
					Charset.defaultCharset());
		}

		Map<String, String> headers = new HashMap<>();
		message.getHeaders().forEach(
				(key, value) -> headers.put(key, value.toString()));

		ListenableFuture<String> pubsubFuture =
				this.pubSubTemplate.publish(this.topic, pubsubPayload, headers);

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

	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter,
				"The specified message converter can't be null.");
		this.messageConverter = messageConverter;
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

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}
}
