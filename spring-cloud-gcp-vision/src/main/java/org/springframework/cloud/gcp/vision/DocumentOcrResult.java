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

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.vision.v1.AnnotateFileResponse;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.util.ArrayList;
import java.util.List;

public class DocumentOcrResult {

	private final Storage storageClient;

	private final List<Blob> pageBlobs;

	public DocumentOcrResult(List<Blob> pages, Storage storageClient) {
		this.pageBlobs = pages;
		this.storageClient = storageClient;
	}

	public int getPageCount() {
		return this.pageBlobs.size();
	}

	public TextAnnotation getPage(int pageNumber) throws InvalidProtocolBufferException {
		Blob pageBlob = this.pageBlobs.get(pageNumber);
		return parseJsonBlob(pageBlob);
	}

	public List<TextAnnotation> getAllPages() throws InvalidProtocolBufferException {
		ArrayList<TextAnnotation> textAnnotationPages = new ArrayList<>();
		
		for (Blob blob : this.pageBlobs) {
			TextAnnotation textAnnotation = parseJsonBlob(blob);
			textAnnotationPages.add(textAnnotation);
		}

		return textAnnotationPages;
	}

	private static TextAnnotation parseJsonBlob(Blob blob) throws InvalidProtocolBufferException {
		AnnotateFileResponse.Builder annotateFileResponseBuilder = AnnotateFileResponse.newBuilder();
		String jsonContent = new String(blob.getContent());
		JsonFormat.parser().merge(jsonContent, annotateFileResponseBuilder);

		AnnotateFileResponse annotateFileResponse = annotateFileResponseBuilder.build();

		return annotateFileResponse.getResponses(0).getFullTextAnnotation();
	}
}
