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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.pubsub.v1.PubsubMessage;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dmitry Solomakha
 * @author Doug Hoard
 *
 * @since  1.1
 */

public class JacksonPubSubMessageConverterTests {

	private JacksonPubSubMessageConverter converter = new JacksonPubSubMessageConverter(new ObjectMapper());

	@Test
	public void testString() throws JSONException {
		String str = "test 123";

		PubsubMessage pubsubMessage = this.converter.toPubSubMessage(str, null);

		JSONAssert.assertEquals(
				"\"test 123\"",
				pubsubMessage.getData().toStringUtf8(),
				true);

		Object o = this.converter.fromPubSubMessage(pubsubMessage, String.class);

		assertThat(o).as("verify that deserialized object is equal to the original one").isEqualTo(str);
	}

	@Test
	public void testPojo() throws JSONException {
		Contact contact = new Contact("Thomas", "Edison", 8817);

		PubsubMessage pubsubMessage = this.converter.toPubSubMessage(contact, null);

		JSONAssert.assertEquals(
				"{\"firstName\":\"Thomas\",\"lastName\":\"Edison\",\"zip\":8817}",
				pubsubMessage.getData().toStringUtf8(),
				true);

		Object o = this.converter.fromPubSubMessage(pubsubMessage, Contact.class);

		assertThat(o).as("verify that deserialized object is equal to the original one").isEqualTo(contact);
	}

	@Test
	public void testToPubSubMessageWithNullPayload() throws JSONException {
		PubsubMessage pubsubMessage = this.converter.toPubSubMessage(null, null);
		Assert.assertNotNull(pubsubMessage);
	}

	static class Contact {
		String firstName;

		String lastName;

		int zip;

		public String getFirstName() {
			return this.firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return this.lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public int getZip() {
			return this.zip;
		}

		public void setZip(int zip) {
			this.zip = zip;
		}

		Contact() {
		}

		Contact(String firstName, String lastName, int zip) {
			this.firstName = firstName;
			this.lastName = lastName;
			this.zip = zip;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {

				return false;
			}
			Contact contact = (Contact) o;
			return this.zip == contact.zip &&
					java.util.Objects.equals(this.firstName, contact.firstName) &&
					java.util.Objects.equals(this.lastName, contact.lastName);
		}

		@Override
		public int hashCode() {
			return java.util.Objects.hash(this.firstName, this.lastName, this.zip);
		}
	}
}
