package org.springframework.cloud.gcp.stream.binder.pubsub;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.Test;

public class TempClientLibraryTest {

	/* This test fails on timeout: because client library retries indefinitely, the failed publish never calls
	* the onFailure callback. */
	@Test
	public void failedPublishCallsListener() throws IOException, InterruptedException {
		Publisher publisher = Publisher.newBuilder("projects/elfel-spring/topics/exampleTopic")
				.setEndpoint("cecicestnespasunendpoint:443")
				.build();

		PubsubMessage message = PubsubMessage.newBuilder()
				.setData(ByteString.copyFromUtf8("test message"))
				.build();

		AtomicBoolean errorSet = new AtomicBoolean(false);

		ApiFutures.addCallback(publisher.publish(message),
				new ApiFutureCallback<String>() {
					@Override
					public void onFailure(Throwable throwable) {
						System.out.println("Sending throwable to error channel: " + throwable);
						errorSet.set(true);
					}

					@Override
					public void onSuccess(String messageId) {
						System.out.println("Successfully published message " + messageId);
					}
				});

		Awaitility.await()
				.atMost(Duration.TWO_MINUTES)
				.until(() -> errorSet.get());

	}


	/* This test is identical to the one above, but with overridden retry settings.
	Therefore, it works as expected, invoking the onFailure callback. */
	@Test
	public void failedPublishCallsListener_overriddenRetrySettingsAllowsAsyncPublishToProperlyFail() throws IOException, InterruptedException {
		Publisher publisher = Publisher.newBuilder("projects/elfel-spring/topics/exampleTopic")
				.setEndpoint("cecicestnespasunendpoint:443")
				.setRetrySettings(
						RetrySettings.newBuilder()
								.setMaxAttempts(3)
								.setTotalTimeout(org.threeten.bp.Duration.ofSeconds(10))
								.setInitialRpcTimeout(org.threeten.bp.Duration.ofSeconds(10))
								.setMaxRpcTimeout(org.threeten.bp.Duration.ofSeconds(20)) // TODO: bad message; try 9; longer not shorter
								.build())
				.build();

		PubsubMessage message = PubsubMessage.newBuilder()
				.setData(ByteString.copyFromUtf8("test message"))
				.build();

		AtomicBoolean errorSet = new AtomicBoolean(false);

		ApiFutures.addCallback(publisher.publish(message),
				new ApiFutureCallback<String>() {
					@Override
					public void onFailure(Throwable throwable) {
						System.out.println("Sending throwable to error channel: " + throwable);
						errorSet.set(true);
					}

					@Override
					public void onSuccess(String messageId) {
						System.out.println("Successfully published message " + messageId);
					}
				});

		Awaitility.await()
				.atMost(Duration.TWO_MINUTES)
				.until(() -> errorSet.get());

	}

	@Test
	public void successfulPublishCallsListener() throws IOException, InterruptedException {
		Publisher publisher = Publisher.newBuilder("projects/elfel-spring/topics/exampleTopic")
				.build();

		PubsubMessage message = PubsubMessage.newBuilder()
				.setData(ByteString.copyFromUtf8("test successful message"))
				.build();

		AtomicBoolean successSet = new AtomicBoolean(false);

		ApiFutures.addCallback(publisher.publish(message),
				new ApiFutureCallback<String>() {
					@Override
					public void onFailure(Throwable throwable) {
						System.out.println("publish errored out: " + throwable);
					}

					@Override
					public void onSuccess(String messageId) {
						System.out.println("Successfully published message " + messageId);
						successSet.set(true);
					}
				});

		Awaitility.await()
				.atMost(Duration.FIVE_SECONDS)
				.until(() -> successSet.get());

	}
}
