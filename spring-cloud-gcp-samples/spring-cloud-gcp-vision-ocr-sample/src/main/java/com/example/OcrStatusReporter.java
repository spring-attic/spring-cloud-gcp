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

package com.example;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.cloud.gcp.vision.DocumentOcrResultSet;
import org.springframework.util.concurrent.ListenableFuture;

public class OcrStatusReporter {

	private final Map<String, OcrOperationStatus> pendingOcrOperations;

	public OcrStatusReporter() {
		this.pendingOcrOperations = new HashMap<>();
	}

	public void registerFuture(
			String documentPath, ListenableFuture<DocumentOcrResultSet> resultFuture) {

		pendingOcrOperations.put(
				documentPath, new OcrOperationStatus(documentPath, resultFuture));
	}

	public Map<String, OcrOperationStatus> getDocumentOcrStatuses() {
		return pendingOcrOperations;
	}

	public static final class OcrOperationStatus {
		final String gcsLocation;
		final ListenableFuture<DocumentOcrResultSet> ocrResultFuture;

		public OcrOperationStatus(
				String gcsLocation,
				ListenableFuture<DocumentOcrResultSet> ocrResultFuture) {
			this.gcsLocation = gcsLocation;
			this.ocrResultFuture = ocrResultFuture;
		}

		public String getGcsLocation() {
			return gcsLocation;
		}

		public boolean isDone() {
			return ocrResultFuture.isDone();
		}

		public DocumentOcrResultSet getResultSet() throws ExecutionException, InterruptedException {
			return ocrResultFuture.get();
		}
	}
}
