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

package com.example;

import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.Job;

import org.springframework.cloud.gcp.bigquery.core.BigQueryTemplate;
import org.springframework.cloud.gcp.bigquery.integration.BigQuerySpringMessageHeaders;
import org.springframework.cloud.gcp.bigquery.integration.outbound.BigQueryFileMessageHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Sample configuration for using BigQuery with Spring Integration.
 *
 * @author Daniel Zou
 */
@Configuration
public class BigQuerySampleConfiguration {

	@Bean
	public DirectChannel bigQueryWriteDataChannel() {
		return new DirectChannel();
	}

	@Bean
	public DirectChannel bigQueryJobReplyChannel() {
		return new DirectChannel();
	}

	@Bean
	@ServiceActivator(inputChannel = "bigQueryWriteDataChannel")
	public MessageHandler messageSender(BigQueryTemplate bigQueryTemplate) {
		BigQueryFileMessageHandler messageHandler = new BigQueryFileMessageHandler(bigQueryTemplate);
		messageHandler.setFormatOptions(FormatOptions.csv());
		messageHandler.setOutputChannel(bigQueryJobReplyChannel());
		return messageHandler;
	}

	@Bean
	public GatewayProxyFactoryBean gatewayProxyFactoryBean() {
		GatewayProxyFactoryBean factoryBean = new GatewayProxyFactoryBean(BigQueryFileGateway.class);
		factoryBean.setDefaultRequestChannel(bigQueryWriteDataChannel());
		factoryBean.setDefaultReplyChannel(bigQueryJobReplyChannel());
		// Ensures that BigQueryFileGateway does not return double-wrapped ListenableFutures
		factoryBean.setAsyncExecutor(null);
		return factoryBean;
	}

	/**
	 * Spring Integration gateway which allows sending data to load to BigQuery through a
	 * channel.
	 */
	@MessagingGateway
	public interface BigQueryFileGateway {
		ListenableFuture<Job> writeToBigQueryTable(
				byte[] csvData, @Header(BigQuerySpringMessageHeaders.TABLE_NAME) String tableName);
	}

}
