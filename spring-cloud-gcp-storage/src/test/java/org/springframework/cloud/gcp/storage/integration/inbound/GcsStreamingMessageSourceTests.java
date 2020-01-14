/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.storage.integration.inbound;

import java.io.InputStream;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.cloud.PageImpl;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.storage.integration.GcsSessionFactory;
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
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

/**
 * Tests for the streaming message source.
 *
 * @author João André Martins
 * @author Chengyuan Zhao
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GcsStreamingMessageSourceTests {

	@Autowired
	private PollableChannel unsortedChannel;

	@Autowired
	private PollableChannel sortedChannel;

	@Test
	public void testInboundStreamingChannelAdapter() {
		Message<?> message = this.unsortedChannel.receive(5000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(InputStream.class);
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("gamma");

		message = this.unsortedChannel.receive(5000);
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("beta");
		assertThat(message.getPayload()).isInstanceOf(InputStream.class);

		message = this.unsortedChannel.receive(5000);
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("alpha/alpha");
		assertThat(message.getPayload()).isInstanceOf(InputStream.class);

		message = this.unsortedChannel.receive(10);
		assertThat(message).isNull();
	}

	@Test
	public void testSortedInboundChannelAdapter() {
		// This uses the channel adapter with a custom comparator.
		// Files will be processed in ascending order by name: alpha/alpha, beta, gamma
		Message<?> message = this.sortedChannel.receive(5000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(InputStream.class);
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("alpha/alpha");

		message = this.sortedChannel.receive(5000);
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("beta");
		assertThat(message.getPayload()).isInstanceOf(InputStream.class);

		message = this.sortedChannel.receive(5000);
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("gamma");
		assertThat(message.getPayload()).isInstanceOf(InputStream.class);

		message = this.sortedChannel.receive(10);
		assertThat(message).isNull();
	}

	private static Blob createBlob(String bucket, String name) {
		Blob blob = mock(Blob.class);
		willAnswer((invocationOnMock) -> bucket).given(blob).getBucket();
		willAnswer((invocationOnMock) -> name).given(blob).getName();
		return blob;
	}

	/**
	 * Spring config for the tests.
	 */
	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public Storage gcsClient() {
			Storage gcs = mock(Storage.class);

			willAnswer((invocationOnMock) ->
				new PageImpl<>(null, null,
						Stream.of(
								createBlob("gcsbucket", "gamma"),
								createBlob("gcsbucket", "beta"),
								createBlob("gcsbucket", "alpha/alpha"))
								.collect(Collectors.toList())))
					.given(gcs).list(eq("gcsbucket"));

			willAnswer((invocationOnMock) -> mock(ReadChannel.class))
					.given(gcs).reader(eq("gcsbucket"), eq("alpha/alpha"));
			willAnswer((invocationOnMock) -> mock(ReadChannel.class))
					.given(gcs).reader(eq("gcsbucket"), eq("beta"));
			willAnswer((invocationOnMock) -> mock(ReadChannel.class))
					.given(gcs).reader(eq("gcsbucket"), eq("gamma"));

			return gcs;
		}

		@Bean
		@InboundChannelAdapter(value = "unsortedChannel", poller = @Poller(fixedDelay = "100"))
		public MessageSource<InputStream> unsortedChannelAdapter(Storage gcs) {
			GcsStreamingMessageSource adapter =
					new GcsStreamingMessageSource(new RemoteFileTemplate<>(new GcsSessionFactory(gcs)));
			adapter.setRemoteDirectory("gcsbucket");
			adapter.setFilter(new AcceptOnceFileListFilter<>());

			return adapter;
		}

		@Bean
		@InboundChannelAdapter(value = "sortedChannel", poller = @Poller(fixedDelay = "100"))
		public MessageSource<InputStream> sortedChannelAdapter(Storage gcs) {
			GcsStreamingMessageSource adapter =
					new GcsStreamingMessageSource(
							new RemoteFileTemplate<>(new GcsSessionFactory(gcs)),
							Comparator.comparing(blob -> blob.getName()));

			adapter.setRemoteDirectory("gcsbucket");
			adapter.setFilter(new AcceptOnceFileListFilter<>());

			return adapter;
		}

		@Bean
		public PollableChannel unsortedChannel() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel sortedChannel() {
			return new QueueChannel();
		}
	}
}
