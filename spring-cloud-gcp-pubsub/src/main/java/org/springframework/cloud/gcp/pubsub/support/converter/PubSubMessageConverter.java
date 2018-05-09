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

package org.springframework.cloud.gcp.pubsub.support.converter;

import java.io.IOException;

import com.google.pubsub.v1.PubsubMessage;

/**
 * Interface for converters that can convert POJOs to and from PubSub messages.
 *
 * @author Chengyuan Zhao
 */
public interface PubSubMessageConverter {

	/**
	 * Convert a Java object to a byte array payload.
	 * @param object the object to convert
	 * @return a byte array ready to be sent with PubSub
	 */
	byte[] toPayload(Object object) throws IOException;

	/**
	 * Convert the payload of a given PubSub message to a desired Java type.
	 * @param message the message containing the payload of the object
	 * @param payloadType the desired type of the object
	 * @return the object converted from the message's payload
	 */
	<T> T fromMessage(PubsubMessage message, Class<T> payloadType) throws IOException;
}
