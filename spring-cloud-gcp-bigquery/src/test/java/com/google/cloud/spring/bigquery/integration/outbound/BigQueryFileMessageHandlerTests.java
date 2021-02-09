/*
 * Copyright 2017-2021 the original author or authors.
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

package com.google.cloud.spring.bigquery.integration.outbound;

import java.io.InputStream;
import java.util.Collections;

import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.spring.bigquery.core.BigQueryTemplate;
import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.expression.ValueExpression;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BigQueryFileMessageHandlerTests {

	private BigQueryTemplate bigQueryTemplate;

	private BigQueryFileMessageHandler messageHandler;

	@Before
	public void setup() {
		bigQueryTemplate = mock(BigQueryTemplate.class);
		SettableListenableFuture<Job> result = new SettableListenableFuture<>();
		result.set(mock(Job.class));
		when(bigQueryTemplate.writeDataToTable(any(), any(), any(), any()))
				.thenReturn(result);

		messageHandler = new BigQueryFileMessageHandler(bigQueryTemplate);
	}

	@Test
	public void testHandleMessage_async() {
		messageHandler.setTableName("testTable");
		messageHandler.setFormatOptions(FormatOptions.csv());
		messageHandler.setSync(false);
		messageHandler.setTableSchema(Schema.of());

		InputStream payload = mock(InputStream.class);
		Message<?> message = MessageBuilder.createMessage(
				payload, new MessageHeaders(Collections.emptyMap()));

		Object result = messageHandler.handleRequestMessage(message);

		verify(bigQueryTemplate).writeDataToTable("testTable", payload, FormatOptions.csv(), Schema.of());
		assertThat(result)
				.isNotNull()
				.isInstanceOf(ListenableFuture.class);
	}

	@Test
	public void testHandleMessage_sync() {
		messageHandler.setTableName("testTable");
		messageHandler.setFormatOptions(FormatOptions.csv());
		messageHandler.setSync(true);
		messageHandler.setTableSchemaExpression(new ValueExpression<>(Schema.of()));

		InputStream payload = mock(InputStream.class);
		Message<?> message = MessageBuilder.createMessage(
				payload, new MessageHeaders(Collections.emptyMap()));

		Object result = messageHandler.handleRequestMessage(message);

		verify(bigQueryTemplate).writeDataToTable("testTable", payload, FormatOptions.csv(), Schema.of());
		assertThat(result)
				.isNotNull()
				.isInstanceOf(Job.class);
	}
}
