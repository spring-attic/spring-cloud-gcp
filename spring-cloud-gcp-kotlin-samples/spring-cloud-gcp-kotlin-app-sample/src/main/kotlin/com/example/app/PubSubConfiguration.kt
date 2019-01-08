/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.app

import com.example.data.Person
import com.example.data.PersonRepository
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate
import org.springframework.cloud.gcp.pubsub.integration.AckMode
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.channel.DirectChannel
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.handler.annotation.Header
import org.springframework.cloud.gcp.pubsub.support.converter.JacksonPubSubMessageConverter
import com.fasterxml.jackson.databind.ObjectMapper



/**
 * Configuration to listen to Pub/Sub topic and register users to the database.
 *
 * @author Daniel Zou
 *
 * @since 1.1
 */
@Configuration
class PubSubConfiguration {

	private val LOGGER = LogFactory.getLog(PubSubConfiguration::class.java)

	private val REGISTRANT_SUBSCRIPTION = "registrations-sub"

	@Autowired
	private lateinit var personRepository: PersonRepository

	@Bean
	fun pubsubInputChannel() = DirectChannel()

	/**
	 * This bean enables serialization/deserialization of Java objects to JSON allowing you
	 * utilize JSON message payloads in Cloud Pub/Sub.
	 * @param objectMapper the object mapper to use
	 * @return a Jackson message converter
	 */
	@Bean
	fun jacksonPubSubMessageConverter(objectMapper: ObjectMapper): JacksonPubSubMessageConverter {
		return JacksonPubSubMessageConverter(objectMapper)
	}

	@Bean
	fun messageChannelAdapter(
			@Qualifier("pubsubInputChannel") inputChannel: MessageChannel,
			pubSubTemplate: PubSubTemplate): PubSubInboundChannelAdapter {

		val adapter = PubSubInboundChannelAdapter(pubSubTemplate, REGISTRANT_SUBSCRIPTION)
		adapter.outputChannel = inputChannel
		adapter.ackMode = AckMode.MANUAL
		adapter.payloadType = Person::class.java
		return adapter
	}

	@ServiceActivator(inputChannel = "pubsubInputChannel")
	fun messageReceiver(
			payload: Person,
			@Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) message: BasicAcknowledgeablePubsubMessage) {

		personRepository.save(payload)

		LOGGER.info("Message arrived! Payload: $payload")
		message.ack()
	}
}
