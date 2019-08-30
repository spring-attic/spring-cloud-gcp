/*
 * Copyright 2019-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.firestore.util;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 * Converter from a gRPC async calls to Reactor primitives ({@link Mono}, {@link Flux}).
 *
 * @author Dmitry Solomakha
 * @author Chengyuan Zhao
 * @since 1.2
 */
public final class ObservableReactiveUtil {

	private ObservableReactiveUtil() {
	}

	/**
	 * Invokes a lambda that in turn issues a remote call, directing the response to a
	 * {@link Mono} stream.
	 * @param remoteCall lambda capable of invoking the correct remote call, making use of the
	 *     {@link Mono}-converting {@link StreamObserver} implementation.
	 * @param <ResponseT> type of remote call response
	 * @return {@link Mono} containing the response of the unary call.
	 */
	public static <ResponseT> Mono<ResponseT> unaryCall(
			Consumer<StreamObserver<ResponseT>> remoteCall) {
		return Mono.create(sink -> remoteCall.accept(new UnaryStreamObserver(sink)));
	}

	/**
	 * Invokes a lambda that issues a streaming call and directs the response to a
	 * {@link Flux} stream.
	 *
	 * @param remoteCall call to make
	 * @param <ResponseT> response type
	 * @return {@link Flux} of response objects resulting from the streaming call.
	 */
	public static <ResponseT> Flux<ResponseT> streamingCall(
			Consumer<StreamObserver<ResponseT>> remoteCall) {

		return Flux.create(sink -> {
			StreamingObserver observer = new StreamingObserver(sink);
			remoteCall.accept(observer);
			sink.onRequest(demand -> observer.request(demand));
		});
	}

	/**
	 * Opens a bi-directional streaming call.
	 * @param initialCall opens a bidirectional gRPC connection with the provided {@link StreamObserver}.
	 * @param inputs stream of objects to send to the server
	 * @param responseHandler processes responses streamed from the server
	 * @param <ResponseT> type of objects streamed from the server
	 * @param <RequestT> type of objects  q  streamed to the server
	 * @param <T> type of input objects that will be wrapped in {@link RequestT}
	 * @return A {@link Flux} of server responses
	 */
	public static <ResponseT, RequestT, T> Flux<T> streamingBidirectionalCall(
			Function<StreamObserver<ResponseT>, StreamObserver<RequestT>> initialCall,
			Flux<List<T>> inputs,
			BiFunction<List<T>, ResponseT, RequestT> responseHandler) {
		AtomicReference<StreamObserver<RequestT>> requestStream = new AtomicReference<>();

		Flux<ResponseT> responsesFlux =
				ObservableReactiveUtil
						.<ResponseT>streamingCall(obs -> requestStream.set(initialCall.apply(obs)))
						.cache(1);

		return inputs
				.flatMap(input ->
						responsesFlux.next()
								.map(mostRecentResponse -> responseHandler.apply(input, mostRecentResponse))
								.doOnNext(nextRequest -> requestStream.get().onNext(nextRequest))
								.thenReturn(input))
				.doOnComplete(() -> requestStream.get().onCompleted())
				.delayUntil(input -> responsesFlux)
				.flatMap(entityList -> Flux.fromIterable(entityList));
	}

	static class StreamingObserver<RequestT, ResponseT>
			implements ClientResponseObserver<RequestT, ResponseT> {
		ClientCallStreamObserver<RequestT> rsObserver;

		FluxSink<ResponseT> sink;

		StreamingObserver(FluxSink<ResponseT> sink) {
			this.sink = sink;
		}

		@Override
		public void onNext(ResponseT value) {
			this.sink.next(value);
		}

		@Override
		public void onError(Throwable throwable) {
			this.sink.error(throwable);
		}

		@Override
		public void onCompleted() {
			this.sink.complete();
		}

		@Override
		public void beforeStart(ClientCallStreamObserver<RequestT> requestStream) {
			this.rsObserver = requestStream;
			requestStream.disableAutoInboundFlowControl();
			this.sink.onCancel(() -> requestStream.cancel("Flux requested cancel.", null));
		}

		void request(long n) {
			this.rsObserver.request(n > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) n);
		}
	}

	/**
	 * Forwards the result of a unary gRPC call to a {@link MonoSink}.
	 *
	 * <p>
	 * Unary gRPC calls expect a single response or an error, so completion of the call
	 * without an emitted value is an error condition.
	 *
	 * @param <ResponseT> type of expected gRPC call response value.
	 */
	private static class UnaryStreamObserver<ResponseT> implements StreamObserver<ResponseT> {

		private boolean terminalEventReceived;

		private final MonoSink sink;

		UnaryStreamObserver(MonoSink sink) {
			this.sink = sink;
		}

		@Override
		public void onNext(ResponseT response) {
			this.terminalEventReceived = true;
			this.sink.success(response);
		}

		@Override
		public void onError(Throwable throwable) {
			this.terminalEventReceived = true;
			this.sink.error(throwable);
		}

		@Override
		public void onCompleted() {
			if (!this.terminalEventReceived) {
				this.sink.error(
						new RuntimeException("Unary gRPC call completed without yielding a value or an error"));
			}
		}
	}
}
