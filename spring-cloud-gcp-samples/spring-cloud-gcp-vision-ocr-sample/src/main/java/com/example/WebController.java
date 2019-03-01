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

package com.example;

import com.google.api.client.util.ByteStreams;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.storage.GoogleStorageLocation;
import org.springframework.cloud.gcp.storage.GoogleStorageResource;
import org.springframework.cloud.gcp.vision.DocumentOcrResultSet;
import org.springframework.cloud.gcp.vision.DocumentOcrTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.ui.ModelMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class WebController {

	@Value("${application.ocr-bucket}")
	private String ocrBucket;

	@Autowired
	private Storage storage;

	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private DocumentOcrTemplate documentOcrTemplate;

	private final OcrStatusReporter ocrStatusReporter;

	public WebController() {
		this.ocrStatusReporter = new OcrStatusReporter();
	}

	@GetMapping("/")
	public ModelAndView renderIndex(ModelMap map) {
		map.put("ocrBucket", ocrBucket);
		return new ModelAndView("index", map);
	}

	@GetMapping("/status")
	public ModelAndView renderStatusPage(ModelMap map) {
		map.put("ocrStatuses", ocrStatusReporter.getDocumentOcrStatuses().values());
		return new ModelAndView("status", map);
	}

	@GetMapping("/viewDocument")
	public ModelAndView renderViewDocumentPage(
			@RequestParam("gcsDocumentUrl") String gcsDocumentUrl, ModelMap map)
			throws ExecutionException, InterruptedException, InvalidProtocolBufferException {

		TextAnnotation textAnnotation =
				ocrStatusReporter.getDocumentOcrStatuses()
						.get(gcsDocumentUrl)
						.getResultSet()
						.getPage(0);

		String[] firstWordsTokens = textAnnotation.getText().split(" ", 50);

		map.put("gcsDocumentUrl", gcsDocumentUrl);
		map.put("text", String.join(" ", firstWordsTokens));

		return new ModelAndView("viewDocument", map);
	}

	@PostMapping("/submitDocument")
	public void submitDocument(@RequestParam("documentUrl") String documentUrl) throws IOException {

		// Upload the document to the GCS bucket
		Resource documentResource = resourceLoader.getResource(documentUrl);
		BlobId outputBlobId = BlobId.of(ocrBucket, documentResource.getFilename());
		BlobInfo blobInfo =
				BlobInfo.newBuilder(outputBlobId)
						.setContentType(getFileType(documentResource))
						.build();

		try (WriteChannel writer = storage.writer(blobInfo)) {
			ByteStreams.copy(documentResource.getInputStream(), Channels.newOutputStream(writer));
		}

		// Run the OCR algorithm on the document
		GoogleStorageLocation documentLocation =
				GoogleStorageLocation.forFile(outputBlobId.getBucket(), outputBlobId.getName());

		GoogleStorageLocation outputLocation = GoogleStorageLocation.forFolder(
				outputBlobId.getBucket(), "ocr_results/" + documentLocation.getBlobName());

		ListenableFuture<DocumentOcrResultSet> result =
				documentOcrTemplate.runOcrForDocument(
						documentLocation, outputLocation, 0);

		ocrStatusReporter.registerFuture(documentLocation.uriString(), result);
	}

	private static String getFileType(Resource documentResource) {
		int extensionIdx = documentResource.getFilename().lastIndexOf(".");
		String fileType = documentResource.getFilename().substring(extensionIdx);

		switch (fileType) {
			case ".tif":
				return "image/tiff";
			default:
				return "application/pdf";
		}
	}
}
