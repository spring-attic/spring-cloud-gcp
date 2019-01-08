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

import java.io.IOException;
import java.io.InputStream;

import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesRequest;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.rpc.Status;
import io.grpc.Status.Code;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link CloudVisionTemplate}.
 *
 * @author Daniel Zou
 *
 * @since 1.1
 */
public class CloudVisionTemplateTests {

	/** Used to test exception messages and types. **/
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	// Resource representing a fake image blob
	private static final Resource FAKE_IMAGE = new ByteArrayResource("fake_image".getBytes());

	private ImageAnnotatorClient imageAnnotatorClient;

	private CloudVisionTemplate cloudVisionTemplate;

	@Before
	public void setupVisionTemplateMock() {
		this.imageAnnotatorClient = Mockito.mock(ImageAnnotatorClient.class);
		this.cloudVisionTemplate = new CloudVisionTemplate(this.imageAnnotatorClient);
	}

	@Test
	public void testEmptyClientResponseError() {
		when(this.imageAnnotatorClient.batchAnnotateImages(any(BatchAnnotateImagesRequest.class)))
				.thenReturn(BatchAnnotateImagesResponse.getDefaultInstance());

		this.expectedException.expect(CloudVisionException.class);
		this.expectedException.expectMessage(
				"Failed to receive valid response Vision APIs; empty response received.");

		this.cloudVisionTemplate.analyzeImage(FAKE_IMAGE, Type.TEXT_DETECTION);
	}

	@Test
	public void testExtractTextError() {
		AnnotateImageResponse response = AnnotateImageResponse.newBuilder()
				.setError(
						Status.newBuilder()
								.setCode(Code.INTERNAL.value())
								.setMessage("Error Message from Vision API."))
				.build();

		BatchAnnotateImagesResponse responseBatch = BatchAnnotateImagesResponse
				.newBuilder()
				.addResponses(response)
				.build();

		when(this.imageAnnotatorClient.batchAnnotateImages(any(BatchAnnotateImagesRequest.class)))
				.thenReturn(responseBatch);

		this.expectedException.expect(CloudVisionException.class);
		this.expectedException.expectMessage("Error Message from Vision API.");

		this.cloudVisionTemplate.extractTextFromImage(FAKE_IMAGE);
	}

	@Test
	public void testIOError() {
		this.expectedException.expect(CloudVisionException.class);
		this.expectedException.expectMessage("Failed to read image bytes from provided resource.");

		this.cloudVisionTemplate.analyzeImage(new BadResource(), Type.LABEL_DETECTION);
	}

	private static final class BadResource extends AbstractResource {
		@Override
		public String getDescription() {
			return "bad resource";
		}

		@Override
		public InputStream getInputStream() throws IOException {
			throw new IOException("Failed to open resource.");
		}
	}

}
