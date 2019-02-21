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
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
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
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
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

	private static final Set<String> SUPPORTED_FILE_FORMATS =
			Stream.of("application/pdf", "image/tiff").collect(Collectors.toSet());

	private static final Pattern OUTPUT_PAGE_PATTERN = Pattern.compile("output-(\\d+)-to-\\d+\\.json");

	private static final String OUTPUT_FOLDER_FORMAT = "ocr_results/%s/";

	private final ImageAnnotatorClient imageAnnotatorClient;

	private final Storage storage;

	public DocumentOcrTemplate(
			ImageAnnotatorClient imageAnnotatorClient,
			Storage storage) {
		this.imageAnnotatorClient = imageAnnotatorClient;
		this.storage = storage;
	}

	/**
	 * Runs OCR on all of the PDF/TIFF documents in the {@code inputBucket}. The OCR output files will
	 * be saved in the {@code outputBucket} to a folder named ocr_results/[[document_file_name]]/.
	 * One JSON output file is produced for each page of the document.
	 *
	 * @param inputBucketName the bucket containing the documents to process
	 * @param outputBucketName the bucket in which the OCR output files is to be written
	 * @return A list of {@link DocumentOcrMetadata} describing the status of the files being processed
	 * @throws IOException if I/O errors occurred with finding or reading the input files
	 */
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

	/**
	 * Runs OCR processing for a specified {@code document} and outputs the results
	 * to {@code outputFolder}.
	 */
	public DocumentOcrMetadata runOcrForDocument(
			GoogleStorageLocation document, GoogleStorageLocation outputFolder)
			throws IOException {

		Assert.isTrue(
				document.isFile(),
				"The Google Storage document location provided must be a "
						+ "file. Invalid location provided: " + document);

		Assert.isTrue(
				!outputFolder.isFile(),
				"The Google Storage output location provided must be a "
						+ "bucket or folder location. Invalid location provided: " + outputFolder);

		GcsSource gcsSource = GcsSource.newBuilder()
				.setUri(document.uriString())
				.build();

		String contentType = getContentType(document);
		InputConfig inputConfig = InputConfig.newBuilder()
				.setMimeType(contentType)
				.setGcsSource(gcsSource)
				.build();

		GcsDestination gcsDestination = GcsDestination.newBuilder()
				.setUri(outputFolder.uriString())
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

		return new DocumentOcrMetadata(document, outputFolder, convertToSpringFuture(result));
	}

	/**
	 * Parses the OCR output file at the Google Storage location specified by {@code jsonOutputFile}.
	 *
	 * <p>OCR output files are produced after running the Google Cloud Vision APIs on a document.
	 *
	 * @param jsonOutputFile the location of the JSON file containing the OCR output data
	 * @return the {@link TextAnnotation} describing the OCR output contents
	 * @throws InvalidProtocolBufferException if {@code jsonOutputFile} is not a JSON representation
	 *    of {@link TextAnnotation}.
	 */
	public TextAnnotation parseOcrOutputFile(GoogleStorageLocation jsonOutputFile)
			throws InvalidProtocolBufferException {
		if (!jsonOutputFile.isFile()) {
			throw new IllegalArgumentException(
					"Provided JSON output file is not a valid file location: " + jsonOutputFile);
		}

		Blob jsonBlob = this.storage.get(BlobId.of(jsonOutputFile.getBucketName(), jsonOutputFile.getPath()));
		return parseJsonBlob(jsonBlob);
	}

	/**
	 * Parses the OCR output files in a specified folder {@code jsonOutputFolder}. This method assumes
	 * that all of the OCR output files in the folder are a part of the same document.
	 *
	 * @param jsonOutputFolder the folder location containing all of the JSON files of OCR output
	 * @return A {@link DocumentOcrResult} describing the OCR content of a document
	 */
	public DocumentOcrResult parseOcrOutputFileSet(GoogleStorageLocation jsonOutputFolder) {
		if (jsonOutputFolder.isFile()) {
			throw new IllegalArgumentException(
					"Provided JSON output folder is not a folder location: " + jsonOutputFolder);
		}

		Page<Blob> blobsInFolder = this.storage.list(
				jsonOutputFolder.getBucketName(),
				BlobListOption.currentDirectory(),
				BlobListOption.prefix(jsonOutputFolder.getPath()));

		List<Blob> blobPages =
				StreamSupport.stream(blobsInFolder.getValues().spliterator(), false)
						.filter(blob -> OUTPUT_PAGE_PATTERN.matcher(blob.getName()).find())
						.sorted(Comparator.comparingInt(blob -> extractPageNumber(blob)))
						.collect(Collectors.toList());

		return new DocumentOcrResult(blobPages, this.storage);
	}

	static TextAnnotation parseJsonBlob(Blob blob) throws InvalidProtocolBufferException {
		AnnotateFileResponse.Builder annotateFileResponseBuilder = AnnotateFileResponse.newBuilder();
		String jsonContent = new String(blob.getContent());
		JsonFormat.parser().merge(jsonContent, annotateFileResponseBuilder);

		AnnotateFileResponse annotateFileResponse = annotateFileResponseBuilder.build();

		return annotateFileResponse.getResponses(0).getFullTextAnnotation();
	}

	private ListenableFuture<DocumentOcrResult> convertToSpringFuture(
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

				String outputFolderUri = asyncBatchAnnotateFilesResponse.getResponsesList().get(0)
						.getOutputConfig()
						.getGcsDestination()
						.getUri();

				GoogleStorageLocation outputFolderLocation = new GoogleStorageLocation(outputFolderUri);
				result.set(parseOcrOutputFileSet(outputFolderLocation));
			}
		}, Runnable::run);

		return result;
	}

	private String getContentType(GoogleStorageLocation googleStorageLocation) throws IOException {
		Blob blob = this.storage.get(BlobId.of(googleStorageLocation.getBucketName(), googleStorageLocation
				.getPath()));
		return blob.getContentType();
	}

	private static int extractPageNumber(Blob blob) {
		Matcher matcher = OUTPUT_PAGE_PATTERN.matcher(blob.getName());
		matcher.find();
		return Integer.parseInt(matcher.group(1));
	}
}
