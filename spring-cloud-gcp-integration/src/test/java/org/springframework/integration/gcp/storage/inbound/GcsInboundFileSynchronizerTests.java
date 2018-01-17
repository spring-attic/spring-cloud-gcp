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

package org.springframework.integration.gcp.storage.inbound;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.cloud.PageImpl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

/**
 * @author João André Martins
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GcsInboundFileSynchronizerTests {

	@Autowired
	private Storage gcs;

	private static final Log LOGGER = LogFactory.getLog(GcsInboundFileSynchronizerTests.class);

	@After
	@Before
	public void cleanUp() throws IOException {
		Path testDirectory = Paths.get("test");

		if (Files.exists(testDirectory)) {
			if (Files.isDirectory(testDirectory)) {
				Files.list(testDirectory).forEach(path -> {
					try {
						Files.delete(path);
					}
					catch (IOException ioe) {
						LOGGER.info("Error deleting test file.", ioe);
					}
				});
			}

			Files.delete(testDirectory);
		}
	}

	@Test
	public void testCopyFiles() throws Exception {
		File localDirectory = new File("test");
		GcsInboundFileSynchronizer synchronizer = new GcsInboundFileSynchronizer(this.gcs);
		synchronizer.setRemoteDirectory("test-bucket");
		synchronizer.setBeanFactory(mock(BeanFactory.class));

		GcsInboundFileSynchronizingMessageSource adapter = new GcsInboundFileSynchronizingMessageSource(synchronizer);
		adapter.setAutoCreateLocalDirectory(true);
		adapter.setLocalDirectory(localDirectory);
		adapter.setBeanFactory(mock(BeanFactory.class));

		adapter.setLocalFilter(new AcceptOnceFileListFilter<>());

		adapter.afterPropertiesSet();

		Message<File> message = adapter.receive();
		assertThat(message.getPayload().getName()).isEqualTo("legend of heroes");
		assertThat(Files.readAllBytes(message.getPayload().toPath())).isEqualTo("estelle".getBytes());

		message = adapter.receive();
		assertThat(message.getPayload().getName()).isEqualTo("trails in the sky");
		assertThat(Files.readAllBytes(message.getPayload().toPath())).isEqualTo("joshua".getBytes());

		message = adapter.receive();
		assertThat(message).isNull();
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public Storage gcs() {
			Storage gcsMock = mock(Storage.class);

			Blob blob1 = mock(Blob.class);
			Blob blob2 = mock(Blob.class);

			willAnswer(invocation -> "legend of heroes").given(blob1).getName();
			willAnswer(invocation -> "trails in the sky").given(blob2).getName();

			willAnswer(invocation -> "estelle".getBytes()).given(gcsMock)
					.readAllBytes(eq("test-bucket"), eq("legend of heroes"));
			willAnswer(invocation -> "joshua".getBytes()).given(gcsMock)
					.readAllBytes(eq("test-bucket"), eq("trails in the sky"));

			willAnswer(invocation -> new PageImpl<>(null, null,
					Stream.of(blob1, blob2)
							.collect(Collectors.toList())))
					.given(gcsMock).list("test-bucket");

			return gcsMock;
		}
	}
}
