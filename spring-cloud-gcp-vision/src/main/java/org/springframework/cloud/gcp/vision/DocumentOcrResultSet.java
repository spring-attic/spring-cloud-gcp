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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import com.google.cloud.storage.Blob;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Represents the parsed OCR content for an document in the provided range of pages.
 *
 * @author Daniel Zou
 */
public class DocumentOcrResultSet {

	private final TreeMap<Integer, OcrPageRange> ocrPageRanges;

	private final int minPage;

	private final int maxPage;

	DocumentOcrResultSet(Collection<Blob> pages) {
		this.ocrPageRanges = new TreeMap<>();

		for (Blob blob : pages) {
			OcrPageRange pageRange = new OcrPageRange(blob);
			ocrPageRanges.put(pageRange.getStartPage(), pageRange);
		}

		this.minPage = this.ocrPageRanges.firstEntry().getValue().getStartPage();
		this.maxPage = this.ocrPageRanges.lastEntry().getValue().getEndPage();
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

		OcrPageRange pageRange = ocrPageRanges.floorEntry(pageNumber).getValue();
		return pageRange.getPage(pageNumber);
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
						currentPageRange = pageRange.getPages();
					}
					catch (InvalidProtocolBufferException e) {
						throw new RuntimeException(
								"Failed to parse OCR output from JSON output file "
										+ pageRange.getBlob().getName(),
								e);
					}
				}

				TextAnnotation result = currentPageRange.get(offset);
				offset++;
				return result;
			}
		};
	}
}
