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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeMap;
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
 * Represents the parsed OCR content for an document in the provided range of pages.
 *
 * @author Daniel Zou
 */
public class DocumentOcrResultSet {

	private static final Pattern OUTPUT_PAGE_PATTERN = Pattern.compile("output-(\\d+)-to-(\\d+)\\.json");

	private final TreeMap<Integer, OcrPageRange> ocrPageRanges;

	private final TreeMap<Integer, List<TextAnnotation>> documentPagesCache;

	private final int minPage;

	private final int maxPage;

	DocumentOcrResultSet(Collection<Blob> pages) {
		this.ocrPageRanges = new TreeMap<>();
		this.documentPagesCache = new TreeMap<>();

		int tmpMin = Integer.MAX_VALUE;
		int tmpMax = Integer.MIN_VALUE;
		for (Blob blob : pages) {
			OcrPageRange pageRange = extractPageRange(blob);
			ocrPageRanges.put(pageRange.startPage, pageRange);

			tmpMin = Math.min(pageRange.startPage, tmpMin);
			tmpMax = Math.max(pageRange.endPage, tmpMax);
		}

		this.minPage = tmpMin;
		this.maxPage = tmpMax;
	}

	/**
	 * Returns the minimum page number in the result set.
	 *
	 * @return the lowest page number in the result set.
	 */
	public int getMinPage() {
		return this.minPage;
	}

	/**
	 * Returns the maximum page number in the result set.
	 *
	 * @return the highest page number in the result set.
	 */
	public int getMaxPage() {
		return this.maxPage;
	}


	/**
	 * Retrieves the parsed OCR information of the page at index {@code pageNumber} of the
	 * document. The page number must be a value between {@link DocumentOcrResultSet#getMinPage()}
	 * and {@link DocumentOcrResultSet#getMaxPage()}.
	 *
	 * <p>
	 * This returns a TextAnnotation object which is Google Cloud Vision's representation of a
	 * page of a document. For more information on reading this object, see:
	 * https://cloud.google.com/vision/docs/reference/rpc/google.cloud.vision.v1#google.cloud.vision.v1.TextAnnotation
	 *
	 * @param pageNumber the page number of the document
	 * @return the {@link TextAnnotation} representing the page of the document
	 * @throws InvalidProtocolBufferException if the OCR information for the page failed to be
	 *     parsed
	 *
	 */
	public TextAnnotation getPage(int pageNumber) throws InvalidProtocolBufferException {
		if (pageNumber < this.minPage || pageNumber > this.maxPage) {
			throw new IndexOutOfBoundsException("Page number out of bounds: " + pageNumber);
		}

		int pageRangeFloorKey = this.ocrPageRanges.floorKey(pageNumber);
		OcrPageRange pageRange = this.ocrPageRanges.get(pageRangeFloorKey);
		if (pageNumber > pageRange.endPage) {
			throw new IndexOutOfBoundsException(
					"Page number not found in result set: " + pageNumber + ". "
							+ "Could not find page in closest JSON output file: " + pageRange.blob.getName());
		}

		List<TextAnnotation> documentPages;
		if (documentPagesCache.containsKey(pageRangeFloorKey)) {
			documentPages = documentPagesCache.get(pageRangeFloorKey);
		}
		else {
			documentPages = parseJsonBlob(pageRange.blob);
			documentPagesCache.put(pageRangeFloorKey, documentPages);
		}

		int offsetIdx = pageNumber - pageRange.startPage;
		return documentPages.get(offsetIdx);
	}

	/**
	 * Returns an {@link Iterator} over all the OCR pages of the document.
	 *
	 * @return iterator of {@link TextAnnotation} describing OCR content of each page in the
	 * document.
	 */
	public Iterator<TextAnnotation> getAllPages() {
		return new Iterator<TextAnnotation>() {

			private final Iterator<OcrPageRange> pageRangeIterator = ocrPageRanges.values().iterator();

			private int offset = 0;

			private List<TextAnnotation> currentPageRange = Collections.EMPTY_LIST;

			@Override
			public boolean hasNext() {
				return pageRangeIterator.hasNext() || offset < currentPageRange.size();
			}

			@Override
			public TextAnnotation next() {
				if (!hasNext()) {
					throw new NoSuchElementException("No more pages left in DocumentOcrResultSet.");
				}

				if (offset >= currentPageRange.size()) {
					OcrPageRange pageRange = pageRangeIterator.next();
					offset = 0;

					try {
						currentPageRange = parseJsonBlob(pageRange.blob);
					}
					catch (InvalidProtocolBufferException e) {
						throw new RuntimeException(
								"Failed to parse OCR output from JSON output file "
										+ pageRange.blob.getName(),
								e);
					}
				}

				TextAnnotation result = currentPageRange.get(offset);
				offset++;
				return result;
			}
		};
	}

	private static List<TextAnnotation> parseJsonBlob(Blob blob)
			throws InvalidProtocolBufferException {

		AnnotateFileResponse.Builder annotateFileResponseBuilder = AnnotateFileResponse.newBuilder();
		String jsonContent = new String(blob.getContent());
		JsonFormat.parser().merge(jsonContent, annotateFileResponseBuilder);

		AnnotateFileResponse annotateFileResponse = annotateFileResponseBuilder.build();

		return annotateFileResponse.getResponsesList().stream()
				.map(AnnotateImageResponse::getFullTextAnnotation)
				.collect(Collectors.toList());
	}

	private static OcrPageRange extractPageRange(Blob blob) {
		Matcher matcher = OUTPUT_PAGE_PATTERN.matcher(blob.getName());
		boolean success = matcher.find();

		if (success) {
			int startPage = Integer.parseInt(matcher.group(1));
			int endPage = Integer.parseInt(matcher.group(2));
			return new OcrPageRange(blob, startPage, endPage);
		}
		else {
			throw new IllegalArgumentException(
					"Cannot create a DocumentOcrResultSet with blob: " + blob.getName()
							+ " Blob name does not contain suffix with the form: output-#-to-#.json");
		}
	}

	private static class OcrPageRange {
		private final Blob blob;

		private final int startPage;

		private final int endPage;

		OcrPageRange(Blob blob, int startPage, int endPage) {
			this.blob = blob;
			this.startPage = startPage;
			this.endPage = endPage;
		}
	}
}
