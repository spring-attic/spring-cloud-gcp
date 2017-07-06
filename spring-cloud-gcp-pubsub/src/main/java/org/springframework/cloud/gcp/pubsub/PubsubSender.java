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

package org.springframework.cloud.gcp.pubsub;

import java.util.Map;

import org.springframework.messaging.Message;

/**
 * @author Vinicius Carvalho
 */
public interface PubsubSender<S, M, F> {

	S send(String destination, Object payload);

	S send(String destination, Object payload, Map<String, Object> headers);

	S send(String destination, Message<?> message);

	M sendAll(String destination, F messages);
}
