/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.vision;

import com.google.cloud.storage.Storage;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.cloud.gcp.storage.GoogleStorageLocation;
import org.springframework.test.context.junit4.SpringRunner;

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
		this.documentOcrTemplate = new DocumentOcrTemplate(
				imageAnnotatorClient,
				storage,
				Runnable::run,
				10);
	}

	@Test
	public void testValidateGcsFileInputs() {
		GoogleStorageLocation folder = GoogleStorageLocation.forFolder(
				"bucket", "path/to/folder/");

		assertThatThrownBy(() -> this.documentOcrTemplate.runOcrForDocument(folder, folder))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Provided document location is not a valid file location");

		assertThatThrownBy(() -> this.documentOcrTemplate.readOcrOutputFile(folder))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Provided jsonOutputFile location is not a valid file location");
	}
}
