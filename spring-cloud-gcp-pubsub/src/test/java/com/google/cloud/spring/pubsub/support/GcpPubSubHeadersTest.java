/*
 * Copyright 2021-2021 the original author or authors.
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

package com.google.cloud.spring.pubsub.support;

import java.util.Collections;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class GcpPubSubHeadersTest {

	@Test
	public void getOriginalMessage_emptyHeaders() {
		Message<String> m = new GenericMessage<>("batman");
		assertThat(GcpPubSubHeaders.getOriginalMessage(m)).isEmpty();
	}

	@Test
	public void getOriginalMessage_wrongType() {
		Message<String> m = new GenericMessage<>("batman",
				Collections.singletonMap(GcpPubSubHeaders.ORIGINAL_MESSAGE, 101));
		assertThat(GcpPubSubHeaders.getOriginalMessage(m)).isEmpty();
	}

	@Test
	public void getOriginalMessage() {
		Message<String> m = new GenericMessage<>("batman",
				Collections.singletonMap(GcpPubSubHeaders.ORIGINAL_MESSAGE,
						mock(BasicAcknowledgeablePubsubMessage.class)));
		assertThat(GcpPubSubHeaders.getOriginalMessage(m))
				.isNotEmpty()
				.get()
				.isInstanceOf(BasicAcknowledgeablePubsubMessage.class);
	}
}
