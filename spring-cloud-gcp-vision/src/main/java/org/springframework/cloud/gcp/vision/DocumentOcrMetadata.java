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

import org.springframework.util.concurrent.ListenableFuture;

/**
 * Provides metadata information about the document OCR operation.
 *
 * @author Daniel Zou
 */
public class DocumentOcrMetadata {

	private final GoogleStorageLocation document;

	private final GoogleStorageLocation outputFolder;

	private final ListenableFuture<DocumentOcrResult> future;

	DocumentOcrMetadata(
			GoogleStorageLocation document,
			GoogleStorageLocation outputFolder,
			ListenableFuture<DocumentOcrResult> future) {
		this.document = document;
		this.outputFolder = outputFolder;
		this.future = future;
	}

	/**
	 * Returns the Google Cloud Storage URI for the document being processed.
	 */
	public GoogleStorageLocation getDocumentUri() {
		return document;
	}

	/**
	 * Returns the Google Cloud Storage URI for the folder containing the OCR output files
	 * for the document.
	 */
	public GoogleStorageLocation getOutputFolderUri() {
		return outputFolder;
	}

	/**
	 * Returns a {@link ListenableFuture} which allows the user to block on the operation
	 * or register event listeners for the operation to complete.
	 */
	public ListenableFuture<DocumentOcrResult> getFuture() {
		return future;
	}
}
