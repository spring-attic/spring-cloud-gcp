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
import com.google.longrunning.OperationsClient;
import com.google.longrunning.OperationsSettings;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gcp.core.Credentials;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.vision.DocumentOcrTemplateTests.DocumentOcrTemplateTestsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.concurrent.ListenableFuture;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { DocumentOcrTemplateTestsConfiguration.class })
public class DocumentOcrTemplateTests {

	@Autowired
	public Storage storage;

	@Autowired
	public DocumentOcrTemplate documentOcrTemplate;

	@Test
	public void testDocumentOcr() throws IOException, ExecutionException, InterruptedException {
		DocumentOcrMetadata documentOcrMetadatas =
				documentOcrTemplate.runOcrForDocument(
						"gs://my-pdfs-bucket-888/testpdf.pdf", "gs://");

		documentOcrMetadatas.getFuture().get();

		// // Initiate a blocking wait
		// for (DocumentOcrMetadata metadata : documentOcrMetadatas) {
		// 	metadata.getDocumentOcrResultFuture().get();
		// }

		System.out.println("we are officially done.");
	}

	@Configuration
	public static class DocumentOcrTemplateTestsConfiguration {

		@Bean
		public DocumentOcrTemplate documentOcrTemplate(
				ImageAnnotatorClient imageAnnotatorClient,
				Storage storage) {
			return new DocumentOcrTemplate(imageAnnotatorClient, storage);
		}

		@Bean
		public ImageAnnotatorClient imageAnnotatorClient(
				CredentialsProvider credentialsProvider) throws IOException {
			ImageAnnotatorSettings clientSettings = ImageAnnotatorSettings.newBuilder()
					.setCredentialsProvider(credentialsProvider)
					.build();
			return ImageAnnotatorClient.create(clientSettings);
		}

		@Bean
		public static Storage storage(
				CredentialsProvider credentialsProvider,
				GcpProjectIdProvider projectIdProvider) throws IOException {
			return StorageOptions.newBuilder()
					.setCredentials(credentialsProvider.getCredentials())
					.setProjectId(projectIdProvider.getProjectId()).build().getService();
		}

		@Bean
		public GcpProjectIdProvider gcpProjectIdProvider() {
			return new DefaultGcpProjectIdProvider();
		}

		@Bean
		public CredentialsProvider credentialsProvider() {
			try {
				return new DefaultCredentialsProvider(Credentials::new);
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}
}
