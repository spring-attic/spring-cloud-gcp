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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
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

import org.springframework.cloud.gcp.storage.GoogleStorageLocation;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * Template providing convenient operations for interfacing with Google Cloud Vision's
 * Document OCR feature, which allows you to run OCR algorithms on documents (PDF or TIFF format)
 * stored on Google Cloud Storage.
 *
 * @author Daniel Zou
 */
public class DocumentOcrTemplate {

	private static final Feature DOCUMENT_OCR_FEATURE = Feature.newBuilder()
			.setType(Type.DOCUMENT_TEXT_DETECTION)
			.build();

	private final ImageAnnotatorClient imageAnnotatorClient;

	private final Storage storage;

	private final Executor executor;

	private final int jsonOutputBatchSize;

	public DocumentOcrTemplate(
			ImageAnnotatorClient imageAnnotatorClient,
			Storage storage,
			Executor executor,
			int jsonOutputBatchSize) {
		this.imageAnnotatorClient = imageAnnotatorClient;
		this.storage = storage;
		this.executor = executor;
		this.jsonOutputBatchSize = jsonOutputBatchSize;
	}

	/**
	 * Runs OCR processing for a specified {@code document} and generates OCR output files
	 * under the path specified by {@code outputFilePathPrefix}.
	 *
	 * <p>
	 * For example, if you specify an {@code outputFilePathPrefix} of
	 * "gs://bucket_name/ocr_results/myDoc_", all the output files of OCR processing will be
	 * saved under prefix, such as:
	 *
	 * <ul>
	 * <li>gs://bucket_name/ocr_results/myDoc_output-1-to-5.json
	 * <li>gs://bucket_name/ocr_results/myDoc_output-6-to-10.json
	 * <li>gs://bucket_name/ocr_results/myDoc_output-11-to-15.json
	 * </ul>
	 *
	 * <p>
	 * Note: OCR processing operations may take several minutes to complete, so it may not be
	 * advisable to block on the completion of the operation. One may use the returned
	 * {@link ListenableFuture} to register callbacks or track the status of the operation.
	 *
	 * @param document The {@link GoogleStorageLocation} of the document to run OCR processing
	 * @param outputFilePathPrefix The {@link GoogleStorageLocation} of a file, folder, or a
	 *     bucket describing the path for which all output files shall be saved under
	 *
	 * @return A {@link ListenableFuture} allowing you to register callbacks or wait for the
	 * completion of the operation.
	 */
	public ListenableFuture<DocumentOcrResultSet> runOcrForDocument(
			GoogleStorageLocation document,
			GoogleStorageLocation outputFilePathPrefix) {

		Assert.isTrue(
				document.isFile(),
				"Provided document location is not a valid file location: " + document);

		GcsSource gcsSource = GcsSource.newBuilder()
				.setUri(document.uriString())
				.build();

		String contentType = extractContentType(document);
		InputConfig inputConfig = InputConfig.newBuilder()
				.setMimeType(contentType)
				.setGcsSource(gcsSource)
				.build();

		GcsDestination gcsDestination = GcsDestination.newBuilder()
				.setUri(outputFilePathPrefix.uriString())
				.build();

		OutputConfig outputConfig = OutputConfig.newBuilder()
				.setGcsDestination(gcsDestination)
				.setBatchSize(this.jsonOutputBatchSize)
				.build();

		AsyncAnnotateFileRequest request = AsyncAnnotateFileRequest.newBuilder()
				.addFeatures(DOCUMENT_OCR_FEATURE)
				.setInputConfig(inputConfig)
				.setOutputConfig(outputConfig)
				.build();

		OperationFuture<AsyncBatchAnnotateFilesResponse, OperationMetadata> result =
				imageAnnotatorClient.asyncBatchAnnotateFilesAsync(Collections.singletonList(request));

		return extractOcrResultFuture(result);
	}

	/**
	 * Parses the OCR output files who have the specified {@code jsonFilesetPrefix}. This
	 * method assumes that all of the OCR output files with the prefix are a part of the same
	 * document.
	 *
	 * @param jsonOutputFilePathPrefix the folder location containing all of the JSON files of
	 *     OCR output
	 * @return A {@link DocumentOcrResultSet} describing the OCR content of a document
	 */
	public DocumentOcrResultSet readOcrOutputFileSet(GoogleStorageLocation jsonOutputFilePathPrefix) {
		String nonNullPrefix = (jsonOutputFilePathPrefix.getBlobName() == null)
				? ""
				: jsonOutputFilePathPrefix.getBlobName();

		Page<Blob> blobsInFolder = this.storage.list(
				jsonOutputFilePathPrefix.getBucketName(),
				BlobListOption.currentDirectory(),
				BlobListOption.prefix(nonNullPrefix));

		List<Blob> blobPages =
				StreamSupport.stream(blobsInFolder.getValues().spliterator(), false)
						.filter(blob -> blob.getContentType().equals("application/octet-stream"))
						.collect(Collectors.toList());

		return new DocumentOcrResultSet(blobPages);
	}

	/**
	 * Parses a single JSON output file and returns the list of pages stored in the file.
	 *
	 * <p>
	 * Each page of the document is represented as a {@link TextAnnotation} which contains the
	 * parsed OCR data.
	 *
	 * @param jsonFile the location of the JSON output file
	 * @return the list of {@link TextAnnotation} containing the OCR results
	 * @throws InvalidProtocolBufferException if the JSON file cannot be deserialized into a
	 *     {@link TextAnnotation} object
	 */
	public DocumentOcrResultSet readOcrOutputFile(GoogleStorageLocation jsonFile) {
		if (!jsonFile.isFile()) {
			throw new IllegalArgumentException(
					"Provided jsonOutputFile location is not a valid file location: " + jsonFile);
		}

		Blob jsonOutputBlob = this.storage.get(
				BlobId.of(jsonFile.getBucketName(), jsonFile.getBlobName()));
		return new DocumentOcrResultSet(Collections.singletonList(jsonOutputBlob));
	}

	private ListenableFuture<DocumentOcrResultSet> extractOcrResultFuture(
			OperationFuture<AsyncBatchAnnotateFilesResponse, OperationMetadata> grpcFuture) {

		SettableListenableFuture<DocumentOcrResultSet> result = new SettableListenableFuture<>();

		ApiFutures.addCallback(grpcFuture, new ApiFutureCallback<AsyncBatchAnnotateFilesResponse>() {
			@Override
			public void onFailure(Throwable throwable) {
				result.setException(throwable);
			}

			@Override
			public void onSuccess(
					AsyncBatchAnnotateFilesResponse asyncBatchAnnotateFilesResponse) {

				String outputLocationUri = asyncBatchAnnotateFilesResponse.getResponsesList().get(0)
						.getOutputConfig()
						.getGcsDestination()
						.getUri();

				GoogleStorageLocation outputFolderLocation = new GoogleStorageLocation(outputLocationUri);
				result.set(readOcrOutputFileSet(outputFolderLocation));
			}
		}, this.executor);

		return result;
	}

	private String extractContentType(GoogleStorageLocation document) {
		Blob documentBlob = this.storage.get(
				BlobId.of(document.getBucketName(), document.getBlobName()));
		if (documentBlob == null) {
			throw new IllegalArgumentException(
					"Provided document does not exist: " + document);
		}

		return documentBlob.getContentType();
	}
}
