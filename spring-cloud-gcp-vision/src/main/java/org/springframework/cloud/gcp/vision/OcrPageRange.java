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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.cloud.storage.Blob;
import com.google.cloud.vision.v1.AnnotateFileResponse;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

/**
 * Represents a range of pages of the OCR content of a document.
 *
 * @author Daniel Zou
 */
final class OcrPageRange {

	private static final Pattern OUTPUT_PAGE_PATTERN = Pattern.compile("output-(\\d+)-to-(\\d+)\\.json");

	private final Blob blob;

	private final int startPage;

	private final int endPage;

	private List<TextAnnotation> pages = null;

	OcrPageRange(Blob blob) {
		Matcher matcher = OUTPUT_PAGE_PATTERN.matcher(blob.getName());
		boolean success = matcher.find();

		if (success) {
			this.blob = blob;
			this.startPage = Integer.parseInt(matcher.group(1));
			this.endPage = Integer.parseInt(matcher.group(2));
		}
		else {
			throw new IllegalArgumentException(
					"Cannot create a DocumentOcrResultSet with blob: " + blob.getName()
							+ " Blob name does not contain suffix with the form: output-#-to-#.json");
		}
	}

	TextAnnotation getPage(int pageNumber) throws InvalidProtocolBufferException {
		if (pageNumber < startPage || pageNumber > endPage) {
			throw new IndexOutOfBoundsException(
					"Page number not found: " + pageNumber + ". Could not "
							+ "find page in closest JSON output file: " + blob.getName());
		}

		int offsetIdx = pageNumber - startPage;
		return getPages().get(offsetIdx);
	}

	List<TextAnnotation> getPages() throws InvalidProtocolBufferException {
		if (pages == null) {
			pages = parseJsonBlob(blob);
		}

		return pages;
	}

	public Blob getBlob() {
		return blob;
	}

	int getStartPage() {
		return startPage;
	}

	int getEndPage() {
		return endPage;
	}

	private static List<TextAnnotation> parseJsonBlob(Blob blob)
			throws InvalidProtocolBufferException {

		AnnotateFileResponse.Builder annotateFileResponseBuilder = AnnotateFileResponse.newBuilder();
		String jsonContent = new String(blob.getContent());
		JsonFormat.parser().ignoringUnknownFields().merge(jsonContent, annotateFileResponseBuilder);

		AnnotateFileResponse annotateFileResponse = annotateFileResponseBuilder.build();

		return annotateFileResponse.getResponsesList().stream()
				.map(AnnotateImageResponse::getFullTextAnnotation)
				.collect(Collectors.toList());
	}
}
