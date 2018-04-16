/*
 *  Copyright 2018 original author or authors.
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

package com.example;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;

import com.google.cloud.storage.Storage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gcp.storage.integration.GcsRemoteFileTemplate;
import org.springframework.cloud.gcp.storage.integration.GcsSessionFactory;
import org.springframework.cloud.gcp.storage.integration.inbound.GcsInboundFileSynchronizer;
import org.springframework.cloud.gcp.storage.integration.inbound.GcsInboundFileSynchronizingMessageSource;
import org.springframework.cloud.gcp.storage.integration.inbound.GcsStreamingMessageSource;
import org.springframework.cloud.gcp.storage.integration.outbound.GcsMessageHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.messaging.MessageHandler;

/**
 * @author João André Martins
 */
@SpringBootApplication
public class GcsSpringIntegrationApplication {

	@Value("${gcs-bucket-name}")
	private String gcsBucketName;

	@Value("${gcs-write-bucket}")
	private String gcsWriteBucket;

	@Value("${local-directory}")
	private String localDirectory;

	private static final Log LOGGER = LogFactory.getLog(GcsSpringIntegrationApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(GcsSpringIntegrationApplication.class, args);
	}

	@Bean
	@InboundChannelAdapter(channel = "new-file-channel", poller = @Poller(fixedDelay = "5000"))
	public MessageSource<File> synchronizerAdapter(Storage gcs) {
		GcsInboundFileSynchronizer synchronizer = new GcsInboundFileSynchronizer(gcs);
		synchronizer.setRemoteDirectory(this.gcsBucketName);

		GcsInboundFileSynchronizingMessageSource synchAdapter =
				new GcsInboundFileSynchronizingMessageSource(synchronizer);
		synchAdapter.setLocalDirectory(Paths.get(this.localDirectory).toFile());

		return synchAdapter;
	}

	@Bean
	@ServiceActivator(inputChannel = "new-file-channel")
	public MessageHandler handleNewFileFromSynchronizer() {
		return message -> {
			File file = (File) message.getPayload();
			LOGGER.info("File " + file.getName() + " received by the non-streaming inbound "
					+ "channel adapter.");
		};
	}

	@Bean
	@InboundChannelAdapter(channel = "copy-channel", poller = @Poller(fixedDelay = "5000"))
	public MessageSource<InputStream> streamingAdapter(Storage gcs) {
		GcsStreamingMessageSource adapter = new GcsStreamingMessageSource(
				new GcsRemoteFileTemplate(new GcsSessionFactory(gcs)));
		adapter.setRemoteDirectory(this.gcsBucketName);
		return adapter;
	}

	@Bean
	@ServiceActivator(inputChannel = "copy-channel")
	public MessageHandler outboundChannelAdapter(Storage gcs) {
		GcsMessageHandler outboundChannelAdapter =
				new GcsMessageHandler(new GcsSessionFactory(gcs));
		outboundChannelAdapter.setRemoteDirectoryExpression(
				new ValueExpression<>(this.gcsWriteBucket));

		return outboundChannelAdapter;
	}
}
