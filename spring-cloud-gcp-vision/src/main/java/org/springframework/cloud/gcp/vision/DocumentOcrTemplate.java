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
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.vision.v1.AnnotateFileResponse;
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
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

public class DocumentOcrTemplate {

	private static final Set<String> SUPPORTED_FILE_FORMATS =
			Stream.of("application/pdf", "image/tiff").collect(Collectors.toSet());

	private static final Pattern OUTPUT_PAGE_PATTERN = Pattern.compile("output-(\\d+)-to-\\d+.json");

	private static final String OUTPUT_FOLDER_FORMAT = "ocr_results/%s/";

	private final ImageAnnotatorClient imageAnnotatorClient;

	private final Storage storage;

	public DocumentOcrTemplate(
			ImageAnnotatorClient imageAnnotatorClient,
			Storage storage) {
		this.imageAnnotatorClient = imageAnnotatorClient;
		this.storage = storage;
	}

	public List<DocumentOcrMetadata> runOcrForBucket(String inputBucketName, String outputBucketName)
			throws IOException {

		ArrayList<DocumentOcrMetadata> ocrMetadataList = new ArrayList<>();

		for (Blob blob : this.storage.list(inputBucketName).getValues()) {
			if (SUPPORTED_FILE_FORMATS.contains(blob.getContentType())) {
				GoogleStorageLocation inputFileLocation = GoogleStorageLocation
						.forFile(inputBucketName, blob.getName());

				String outputFolderPath = String.format(OUTPUT_FOLDER_FORMAT, blob.getName());
				GoogleStorageLocation outputFolder = GoogleStorageLocation
						.forFolder(outputBucketName, outputFolderPath);

				DocumentOcrMetadata ocrMetadata = runOcrForDocument(inputFileLocation, outputFolder);
				ocrMetadataList.add(ocrMetadata);
			}
		}

		return ocrMetadataList;
	}

	public DocumentOcrMetadata runOcrForDocument(
			GoogleStorageLocation document, GoogleStorageLocation outputLocation)
			throws IOException {

		Assert.isTrue(
				document.isFile(),
				"The GCS document location provided must be a file: " + document);

		Assert.isTrue(
				!outputLocation.isFile(),
				"The GCS output location provided must be a bucket or folder location: " + outputLocation);

		GcsSource gcsSource = GcsSource.newBuilder()
				.setUri(document.getUriString())
				.build();

		String contentType = getContentType(document);
		InputConfig inputConfig = InputConfig.newBuilder()
				.setMimeType(contentType)
				.setGcsSource(gcsSource)
				.build();

		GcsDestination gcsDestination = GcsDestination.newBuilder()
				.setUri(outputLocation.getUriString())
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

		return new DocumentOcrMetadata(
				document.getUriString(),
				outputLocation.getUriString(),
				convertToSpringFuture(result));
	}

	public TextAnnotation parseOcrOutputFile(String bucketName, String pathToFile)
			throws InvalidProtocolBufferException {
		Blob jsonBlob = this.storage.get(BlobId.of(bucketName, pathToFile));
		return parseJsonBlob(jsonBlob);
	}



	static TextAnnotation parseJsonBlob(Blob blob) throws InvalidProtocolBufferException {
		AnnotateFileResponse.Builder annotateFileResponseBuilder = AnnotateFileResponse.newBuilder();
		String jsonContent = new String(blob.getContent());
		JsonFormat.parser().merge(jsonContent, annotateFileResponseBuilder);

		AnnotateFileResponse annotateFileResponse = annotateFileResponseBuilder.build();

		return annotateFileResponse.getResponses(0).getFullTextAnnotation();
	}

	private String getContentType(GoogleStorageLocation googleStorageLocation) throws IOException {
		Blob blob = this.storage.get(BlobId.of(googleStorageLocation.getBucketName(), googleStorageLocation
				.getGcsPath()));
		return blob.getContentType();
	}

	private static ListenableFuture<DocumentOcrResult> convertToSpringFuture(
			OperationFuture<AsyncBatchAnnotateFilesResponse, OperationMetadata> grpcFuture) {

		SettableListenableFuture<DocumentOcrResult> result = new SettableListenableFuture<>();

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
}
