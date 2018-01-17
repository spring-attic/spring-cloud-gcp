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

import java.io.InputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.cloud.PageImpl;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.gcp.storage.GcsSessionFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
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
public class GcsStreamingMessageSourceTests {

	@Autowired
	private PollableChannel gcsChannel;

	@Test
	public void testInboundStreamingChannelAdapter() {
		Message<?> message = this.gcsChannel.receive(5000);

		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(InputStream.class);
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("folder1/gcsfilename");

		message = this.gcsChannel.receive(5000);
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("secondfilename");
		assertThat(message.getPayload()).isInstanceOf(InputStream.class);

		message = this.gcsChannel.receive(10);
		assertThat(message).isNull();
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public Storage gcsClient() {
			Blob blob1 = mock(Blob.class);
			Blob blob2 = mock(Blob.class);

			willAnswer(invocationOnMock -> "gcsbucket").given(blob1).getBucket();
			willAnswer(invocationOnMock -> "folder1/gcsfilename").given(blob1).getName();
			willAnswer(invocationOnMock -> "gcsbucket").given(blob2).getBucket();
			willAnswer(invocationOnMock -> "secondfilename").given(blob2).getName();

			Storage gcs = mock(Storage.class);

			willAnswer(invocationOnMock ->
				new PageImpl<>(null, null,
						Stream.of(blob1, blob2)
								.collect(Collectors.toList())))
					.given(gcs).list(eq("gcsbucket"));

			ReadChannel channel1 = mock(ReadChannel.class);
			ReadChannel channel2 = mock(ReadChannel.class);
			willAnswer(invocationOnMock -> channel1)
					.given(gcs).reader(eq("gcsbucket"), eq("folder1/gcsfilename"));
			willAnswer(invocationOnMock -> channel2)
					.given(gcs).reader(eq("gcsbucket"), eq("secondfilename"));

			return gcs;
		}

		@Bean
		@InboundChannelAdapter(value = "gcsChannel", poller = @Poller(fixedDelay = "100"))
		public MessageSource<InputStream> adapter(Storage gcs) {
			GcsStreamingMessageSource adapter =
					new GcsStreamingMessageSource(new RemoteFileTemplate<>(new GcsSessionFactory(gcs)));
			adapter.setRemoteDirectory("gcsbucket");
			adapter.setFilter(new AcceptOnceFileListFilter<>());

			return adapter;
		}

		@Bean
		public PollableChannel gcsChannel() {
			return new QueueChannel();
		}
	}
}
