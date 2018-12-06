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

package org.springframework.cloud.gcp.vision;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.vision.v1.AsyncAnnotateFileRequest;
import com.google.cloud.vision.v1.AsyncBatchAnnotateFilesRequest;
import com.google.cloud.vision.v1.AsyncBatchAnnotateFilesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.GcsDestination;
import com.google.cloud.vision.v1.GcsSource;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.InputConfig;
import com.google.cloud.vision.v1.OutputConfig;

import org.springframework.util.Assert;

public class CloudVisionFilesTemplate {

	private final ImageAnnotatorClient imageAnnotatorClient;

	private final Storage storage;

	public CloudVisionFilesTemplate(ImageAnnotatorClient imageAnnotatorClient, Storage storage) {
		Assert.notNull(imageAnnotatorClient, "imageAnnotatorClient must not be null.");
		Assert.notNull(storage, "storage must not be null");

		this.imageAnnotatorClient = imageAnnotatorClient;
		this.storage = storage;
	}

	private static String getBlobUri(Blob blob) {
		String gcsUriFormat = "gs://%s/%s";
		return String.format(gcsUriFormat, blob.getBucket(), blob.getName());
	}

	private static AsyncAnnotateFileRequest buildAsyncRequest(Blob blob) {
		System.out.println("Creating request for: " + blob.getName());

		String outputDest = String.format("gs://%s/outputfolder/", blob.getBucket());

		return AsyncAnnotateFileRequest.newBuilder()
				.setInputConfig(InputConfig.newBuilder()
						.setGcsSource(GcsSource.newBuilder().setUri(getBlobUri(blob)).build())
						.setMimeType("application/pdf")
						.build())
				.setOutputConfig(OutputConfig.newBuilder()
						.setGcsDestination(GcsDestination.newBuilder().setUri(outputDest).build())
						.build())
				.addFeatures(Feature.newBuilder().setType(Type.DOCUMENT_TEXT_DETECTION))
				.build();
	}

	public void asyncBatchAnalyzeFiles(String inputBucketName, String outputBucketName)
			throws ExecutionException, InterruptedException {
		List<AsyncAnnotateFileRequest> requests = StreamSupport
				.stream(this.storage.list(inputBucketName).iterateAll().spliterator(), false)
				.filter(blob -> blob.getName().endsWith(".pdf"))
				.map(blob -> buildAsyncRequest(blob))
				.collect(Collectors.toList());

		AsyncBatchAnnotateFilesRequest req = AsyncBatchAnnotateFilesRequest.newBuilder()
				.addAllRequests(requests)
				.build();

		AsyncBatchAnnotateFilesResponse response = this.imageAnnotatorClient.asyncBatchAnnotateFilesAsync(req).get();
	}
}
