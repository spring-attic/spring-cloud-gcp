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

package org.springframework.integration.gcp.storage.outbound;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.gcp.storage.GcsSessionFactory;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author João André Martins
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GcsMessageHandlerTests {

	private static Storage GCS;

	@Autowired
	@Qualifier("siGcsTestChannel")
	private MessageChannel channel;

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testNewFiles() throws IOException {
		File testFile = this.temporaryFolder.newFile("benfica");

		BlobInfo expectedCreateBlobInfo =
				BlobInfo.newBuilder(BlobId.of("testGcsBucket", "benfica.writing")).build();
		WriteChannel writeChannel = mock(WriteChannel.class);
		willAnswer(invocationOnMock -> writeChannel).given(GCS)
				.writer(eq(expectedCreateBlobInfo));
		willAnswer(invocationOnMock -> 10).given(writeChannel).write(isA(ByteBuffer.class));

		CopyWriter copyWriter = mock(CopyWriter.class);
		ArgumentCaptor<Storage.CopyRequest> copyRequestCaptor = ArgumentCaptor.forClass(Storage.CopyRequest.class);
		willAnswer(invocationOnMock -> copyWriter).given(GCS).copy(isA(Storage.CopyRequest.class));

		willAnswer(invocationOnMock -> true).given(GCS)
				.delete(eq(BlobId.of("testGcsBucket", "benfica.writing")));

		this.channel.send(new GenericMessage<Object>(testFile));

		verify(GCS, times(1)).writer(eq(expectedCreateBlobInfo));
		verify(GCS, times(1)).copy(copyRequestCaptor.capture());
		verify(GCS, times(1)).delete(eq(BlobId.of("testGcsBucket", "benfica.writing")));

		Storage.CopyRequest expectedCopyRequest = copyRequestCaptor.getValue();
		assertThat(expectedCopyRequest.getSource()).isEqualTo(BlobId.of("testGcsBucket", "benfica.writing"));
		assertThat(expectedCopyRequest.getTarget().getBlobId()).isEqualTo(BlobId.of("testGcsBucket", "benfica"));
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public Storage gcsClient() {
			GCS = mock(Storage.class);

			return GCS;
		}

		@Bean
		@ServiceActivator(inputChannel = "siGcsTestChannel")
		public MessageHandler outboundAdapter(Storage gcs) {
			GcsMessageHandler adapter = new GcsMessageHandler(new GcsSessionFactory(gcs));
			adapter.setRemoteDirectoryExpression(new ValueExpression<>("testGcsBucket"));

			return adapter;
		}

		@Bean
		public MessageChannel siGcsTestChannel() {
			return new DirectChannel();
		}
	}
}
