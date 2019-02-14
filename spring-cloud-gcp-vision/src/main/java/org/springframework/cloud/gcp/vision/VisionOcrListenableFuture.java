/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.vision;

import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutureToListenableFuture;
import com.google.api.core.ApiFutures;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.vision.v1.AsyncBatchAnnotateFilesResponse;
import com.google.cloud.vision.v1.OperationMetadata;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SuccessCallback;

public class VisionOcrListenableFuture implements ListenableFuture<DocumentOcrResult> {

	private final OperationFuture<AsyncBatchAnnotateFilesResponse, OperationMetadata> operationFuture;

	VisionOcrListenableFuture(
			OperationFuture<AsyncBatchAnnotateFilesResponse, OperationMetadata> operationFuture,
			) {
		this.operationFuture = operationFuture;
	}

	@Override
	public void addCallback(
			ListenableFutureCallback<? super DocumentOcrResult> listenableFutureCallback) {

		ApiFutures.addCallback(
				operationFuture,
				new ApiFutureCallback<AsyncBatchAnnotateFilesResponse>() {
					@Override
					public void onFailure(Throwable throwable) {
						listenableFutureCallback.onFailure(throwable);
					}

					@Override
					public void onSuccess(
							AsyncBatchAnnotateFilesResponse asyncBatchAnnotateFilesResponse) {
						listenableFutureCallback.onSuccess(asyncBatchAnnotateFilesResponse);
					}
				},
				Runnable::run);



	}

	@Override
	public void addCallback(
			SuccessCallback<? super DocumentOcrResult> successCallback, FailureCallback failureCallback) {

	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return operationFuture.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return operationFuture.isCancelled();
	}

	@Override
	public boolean isDone() {
		return operationFuture.isDone();
	}

	@Override
	public DocumentOcrResult get() throws InterruptedException, ExecutionException {
		return null;
	}

	@Override
	public DocumentOcrResult get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return null;
	}
}
