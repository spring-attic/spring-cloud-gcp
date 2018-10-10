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
import org.springframework.integration.file.FileHeaders;
import org.springframework.messaging.MessageHandler;

/**
 * @author João André Martins
 * @author Mike Eltsufin
 */
@SpringBootApplication
public class GcsSpringIntegrationApplication {

	@Value("${gcs-read-bucket}")
	private String gcsReadBucket;

	@Value("${gcs-write-bucket}")
	private String gcsWriteBucket;

	@Value("${gcs-local-directory}")
	private String localDirectory;

	private static final Log LOGGER = LogFactory.getLog(GcsSpringIntegrationApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(GcsSpringIntegrationApplication.class, args);
	}

	/**
	 * A file synchronizer that knows how to connect to the remote file system (GCS) and scan
	 * it for new files and then download the files.
	 */
	@Bean
	public GcsInboundFileSynchronizer gcsInboundFileSynchronizer(Storage gcs) {
		GcsInboundFileSynchronizer synchronizer = new GcsInboundFileSynchronizer(gcs);
		synchronizer.setRemoteDirectory(this.gcsReadBucket);

		return synchronizer;
	}

	/**
	 * An inbound channel adapter that polls the GCS bucket for new files and copies them to
	 * the local filesystem. The resulting message source produces messages containing handles
	 * to local files.
	 */
	@Bean
	@InboundChannelAdapter(channel = "new-file-channel", poller = @Poller(fixedDelay = "5000"))
	public MessageSource<File> synchronizerAdapter(GcsInboundFileSynchronizer synchronizer) {
		GcsInboundFileSynchronizingMessageSource syncAdapter = new GcsInboundFileSynchronizingMessageSource(
				synchronizer);
		syncAdapter.setLocalDirectory(Paths.get(this.localDirectory).toFile());

		return syncAdapter;
	}

	/**
	 * A service activator that receives messages produced by the {@code synchronizerAdapter}
	 * and simply outputs the file name of each to the console.
	 */
	@Bean
	@ServiceActivator(inputChannel = "new-file-channel")
	public MessageHandler handleNewFileFromSynchronizer() {
		return message -> {
			File file = (File) message.getPayload();
			LOGGER.info("File " + file.getName() + " received by the non-streaming inbound "
					+ "channel adapter.");
		};
	}

	/**
	 * An inbound channel adapter that polls the remote directory and produces messages with
	 * {@code InputStream} payload that can be used to read the data from remote files.
	 */
	@Bean
	@InboundChannelAdapter(channel = "copy-channel", poller = @Poller(fixedDelay = "5000"))
	public MessageSource<InputStream> streamingAdapter(Storage gcs) {
		GcsStreamingMessageSource adapter = new GcsStreamingMessageSource(
				new GcsRemoteFileTemplate(new GcsSessionFactory(gcs)));
		adapter.setRemoteDirectory(this.gcsReadBucket);
		return adapter;
	}

	/**
	 * A service activator that connects to a channel with messages containing
	 * {@code InputStream} payloads and copies the file data to a remote directory on GCS.
	 */
	@Bean
	@ServiceActivator(inputChannel = "copy-channel")
	public MessageHandler outboundChannelAdapter(Storage gcs) {
		GcsMessageHandler outboundChannelAdapter = new GcsMessageHandler(new GcsSessionFactory(gcs));
		outboundChannelAdapter.setRemoteDirectoryExpression(
				new ValueExpression<>(this.gcsWriteBucket));
		outboundChannelAdapter
				.setFileNameGenerator(message -> message.getHeaders().get(FileHeaders.REMOTE_FILE, String.class));

		return outboundChannelAdapter;
	}
}
