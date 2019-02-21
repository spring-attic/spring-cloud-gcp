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

import static org.mockito.Mockito.when;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.Storage.BucketListOption;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.vision.v1.AsyncBatchAnnotateFilesResponse;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.cloud.vision.v1.OperationMetadata;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.longrunning.OperationsClient;
import com.google.longrunning.OperationsSettings;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gcp.core.Credentials;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.concurrent.ListenableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(SpringRunner.class)
public class DocumentOcrTemplateTests {

	private Storage storage;

	private ImageAnnotatorClient imageAnnotatorClient;

	private DocumentOcrTemplate documentOcrTemplate;

	@Before
	public void setupDocumentTemplateMocks() {
		this.storage = Mockito.mock(Storage.class);
		this.imageAnnotatorClient = Mockito.mock(ImageAnnotatorClient.class);
		this.documentOcrTemplate = new DocumentOcrTemplate(imageAnnotatorClient, storage);
	}

	@Test
	public void testRejectInvalidLocationInputs() {
		assertThatThrownBy(
				() -> documentOcrTemplate.runOcrForDocument(
						GoogleStorageLocation.forFile("bucket", "document"),
						GoogleStorageLocation.forFile("bucket", "otherDocument")))
				.hasMessageContaining(
						"The Google Storage output location provided must be a bucket or folder location.");

		assertThatThrownBy(
				() -> documentOcrTemplate.runOcrForDocument(
						GoogleStorageLocation.forFolder("bucket", "folder"),
						GoogleStorageLocation.forFolder("bucket", "folder2")))
				.hasMessageContaining(
						"The Google Storage document location provided must be a file.");

		assertThatThrownBy(
				() -> documentOcrTemplate.parseOcrOutputFile(
						GoogleStorageLocation.forFolder("bucket", "folder")))
				.hasMessageContaining(
						"Provided JSON output file is not a valid file location");

		assertThatThrownBy(
				() -> documentOcrTemplate.parseOcrOutputFileSet(
						GoogleStorageLocation.forFile("bucket", "file")))
				.hasMessageContaining(
						"Provided JSON output folder is not a folder location");
	}

	@Test
	public void testProcessDocumentsInBucket() throws IOException {
		DocumentOcrMetadata ocrMetadata = documentOcrTemplate.runOcrForDocument(
				GoogleStorageLocation.forFile("bucket", "folder/file.pdf"),
				GoogleStorageLocation.forFolder("bucket", "outputFolder"));

		when

	}
}
