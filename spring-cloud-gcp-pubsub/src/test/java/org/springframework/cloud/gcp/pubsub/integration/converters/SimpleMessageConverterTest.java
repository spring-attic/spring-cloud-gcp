package org.springframework.cloud.gcp.pubsub.integration.converters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.pubsub.v1.PubsubMessage;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.support.GenericMessage;

@RunWith(JUnit4.class)
public class SimpleMessageConverterTest {

	@Test
	public void testFromMessage() {
		SimpleMessageConverter converter = new SimpleMessageConverter();
		Message<?> message = new GenericMessage<Object>("test payload");
		PubsubMessage pubsubMessage = (PubsubMessage) converter.fromMessage(message,
				PubsubMessage.class);
		assertEquals("test payload", pubsubMessage.getData().toStringUtf8());
	}

	@Test(expected = MessageConversionException.class)
	public void testFromMessage_notPubsubMessage() {
		new SimpleMessageConverter().fromMessage(new GenericMessage<Object>("payload"),
				String.class);
	}

	@Test
	public void testToMessage() {
		SimpleMessageConverter converter = new SimpleMessageConverter();
		String payload = "test payload";
		Map<String, Object> headers = new HashMap<>();
		headers.put("headerKey1", "headerValue1");
		headers.put("headerKey2", "headerValue2");
		Message<?> message = converter.toMessage(payload, new MessageHeaders(headers));
		assertEquals("test payload", message.getPayload());
		assertTrue(message.getHeaders().get("headerKey1").equals("headerValue1"));
		assertTrue(message.getHeaders().get("headerKey2").equals("headerValue2"));
	}

	@Test
	public void testToMessage_noHeaders() {
		SimpleMessageConverter converter = new SimpleMessageConverter();
		String payload = "test payload";
		Message<?> message = converter.toMessage(payload,
				new MessageHeaders(new HashMap<>()));
		assertEquals("test payload", message.getPayload());
	}

	@Test(expected = MessageConversionException.class)
	public void testToMessage_notString() {
		new SimpleMessageConverter().toMessage(new HashMap<>(),
				new MessageHeaders(new HashMap<>()));
	}
}
