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
import com.google.api.core.ApiFutures;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.storage.Storage;
import com.google.cloud.vision.v1.AsyncAnnotateFileRequest;
import com.google.cloud.vision.v1.AsyncBatchAnnotateFilesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.GcsDestination;
import com.google.cloud.vision.v1.GcsSource;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.InputConfig;

import com.google.cloud.vision.v1.OperationMetadata;
import com.google.cloud.vision.v1.OutputConfig;
import com.google.longrunning.OperationsClient;
import java.io.IOException;
import java.util.Collections;
import org.springframework.cloud.gcp.storage.GoogleStorageResource;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

public class DocumentOcrTemplate {

	private final ImageAnnotatorClient imageAnnotatorClient;
	private final OperationsClient operationsClient;
	private final Storage storage;

	public DocumentOcrTemplate(
			ImageAnnotatorClient imageAnnotatorClient,
			Storage storage) {
		this.imageAnnotatorClient = imageAnnotatorClient;
		this.operationsClient = imageAnnotatorClient.getOperationsClient();
		this.storage = storage;
	}

	public ListenableFuture<Void> runOcr(String gcsSourcePath, String jsonOutputFileNamePrefix)
			throws IOException {

		// GCS input configuration for the document
		GcsSource gcsSource = GcsSource.newBuilder()
				.setUri(gcsSourcePath)
				.build();

		String contentType = getContentType(gcsSourcePath);
		InputConfig inputConfig = InputConfig.newBuilder()
				.setMimeType(contentType)
				.setGcsSource(gcsSource)
				.build();

		// GCS configuration for the JSON output file.
		GcsDestination gcsDestination = GcsDestination.newBuilder()
				.setUri(jsonOutputFileNamePrefix)
				.build();

		OutputConfig outputConfig = OutputConfig.newBuilder()
				.setBatchSize(1)
				.setGcsDestination(gcsDestination)
				.build();

		Feature feature = Feature.newBuilder()
				.setType(Type.DOCUMENT_TEXT_DETECTION)
				.build();

		AsyncAnnotateFileRequest request = AsyncAnnotateFileRequest.newBuilder()
				.addFeatures(feature)
				.setInputConfig(inputConfig)
				.setOutputConfig(outputConfig)
				.build();

		OperationFuture<AsyncBatchAnnotateFilesResponse, OperationMetadata> result =
				imageAnnotatorClient.asyncBatchAnnotateFilesAsync(Collections.singletonList(request));

		return convertGrpcFuture(result);
	}

	private static ListenableFuture<Void> convertGrpcFuture(
			OperationFuture<AsyncBatchAnnotateFilesResponse, OperationMetadata> grpcFuture) {

		SettableListenableFuture<Void> result = new SettableListenableFuture<>();

		ApiFutures.addCallback(grpcFuture, new ApiFutureCallback<AsyncBatchAnnotateFilesResponse>() {
			@Override
			public void onFailure(Throwable throwable) {
				result.setException(throwable);
			}

			@Override
			public void onSuccess(
					AsyncBatchAnnotateFilesResponse asyncBatchAnnotateFilesResponse) {
				result.set(null);
			}
		}, Runnable::run);

		return result;
	}

	private String getContentType(String gcsSourcePath) throws IOException {
		GoogleStorageResource googleStorageResource = new GoogleStorageResource(
				this.storage, gcsSourcePath, false);

		return googleStorageResource.getBlob().getContentType();
	}
}
