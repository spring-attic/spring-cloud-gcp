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

package org.springframework.cloud.gcp.pubsub.integration.outbound;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

import org.springframework.cloud.gcp.pubsub.core.PubSubOperations;
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
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * Outbound channel adapter to publish messages to Google Cloud Pub/Sub.
 *
 * <p>It delegates Google Cloud Pub/Sub interaction to {@link PubSubOperations}. It also converts
 * the {@link Message} payload into a {@link PubsubMessage} accepted by the Google Cloud Pub/Sub
 * Client Library. It supports synchronous and asynchronous sending. If a {@link MessageConverter}
 * to {@code byte[]} is not specified, the message payload should be either {@link PubsubMessage},
 * {@code byte[]}, or {@link com.google.cloud.ByteArray}.
 *
 * @author João André Martins
 * @author Mike Eltsufin
 */
public class PubSubMessageHandler extends AbstractMessageHandler {

	private static final long DEFAULT_PUBLISH_TIMEOUT = 10000;

	private final PubSubOperations pubSubTemplate;

	private MessageConverter messageConverter;

	private Expression topicExpression;

	private boolean sync;

	private EvaluationContext evaluationContext;

	private Expression publishTimeoutExpression = new ValueExpression<>(DEFAULT_PUBLISH_TIMEOUT);

	private ListenableFutureCallback<String> publishCallback;

	private HeaderMapper<Map<String, String>> headerMapper = new PubSubHeaderMapper();

	public PubSubMessageHandler(PubSubOperations pubSubTemplate, String topic) {
		Assert.notNull(pubSubTemplate, "Pub/Sub template cannot be null.");
		Assert.notNull(topic, "Pub/Sub topic cannot be null.");
		this.pubSubTemplate = pubSubTemplate;
		this.topicExpression = new LiteralExpression(topic);
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object payload = message.getPayload();
		String topic = message.getHeaders().containsKey(GcpPubSubHeaders.TOPIC)
				? message.getHeaders().get(GcpPubSubHeaders.TOPIC, String.class)
				: this.topicExpression.getValue(this.evaluationContext, message, String.class);

		ListenableFuture<String> pubsubFuture;

		if (payload instanceof PubsubMessage) {
			pubsubFuture = this.pubSubTemplate.publish(topic, (PubsubMessage) payload);
		}
		else {
			ByteString pubsubPayload;

			if (this.messageConverter != null) {
				pubsubPayload = ByteString.copyFrom(
						(byte[]) this.messageConverter.fromMessage(message, byte[].class));
			}
			else if (payload instanceof byte[]) {
				pubsubPayload = ByteString.copyFrom((byte[]) payload);
			}
			else if (payload instanceof ByteString) {
				pubsubPayload = (ByteString) payload;
			}
			else {
				throw new MessageConversionException("Unable to convert payload of type "
						+ payload.getClass().getName() + " to byte[] for sending to Pub/Sub."
						+ " Try specifying a message converter.");
			}

			Map<String, String> headers = new HashMap<>();
			this.headerMapper.fromHeaders(message.getHeaders(), headers);

			pubsubFuture = this.pubSubTemplate.publish(topic, pubsubPayload, headers);
		}

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

	/**
	 * Set the {@link MessageConverter} to convert an outgoing message payload to a {@code byte[]}
	 * payload for sending to Pub/Sub. If the payload is already of type {@link PubsubMessage},
	 * {@code byte[]} or {@link ByteString}, the converter doesn't have to be set.
	 * @param messageConverter converts {@link Message} to a payload of the outgoing message
	 * to Pub/Sub.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
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

	public Expression getTopicExpression() {
		return this.topicExpression;
	}

	/**
	 * Set the SpEL expression for the topic this adapter sends messages to.
	 * @param topicExpression SpEL expression representing the topic name
	 */
	public void setTopicExpression(Expression topicExpression) {
		this.topicExpression = topicExpression;
	}

	/**
	 * Set the topic where this adapter sends messages to.
	 * @param topic topic name
	 */
	public void setTopic(String topic) {
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
	 * {@link PubsubMessage}.
	 * @param headerMapper the header mapper
	 */
	public void setHeaderMapper(HeaderMapper<Map<String, String>> headerMapper) {
		Assert.notNull(headerMapper, "The header mapper can't be null.");
		this.headerMapper = headerMapper;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}
}
