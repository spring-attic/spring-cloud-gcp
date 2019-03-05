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

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Represents the parsed OCR output for a document.
 *
 * @author Daniel Zou
 */
public class DocumentOcrResultSet {

	private final Storage storageClient;

	private final List<Blob> pageBlobs;

	public DocumentOcrResultSet(List<Blob> pages, Storage storageClient) {
		this.pageBlobs = pages;
		this.storageClient = storageClient;
	}

	/**
	 * Returns the number of pages of the document.
	 *
	 * @return number of pages in the document
	 */
	public int getPageCount() {
		return this.pageBlobs.size();
	}

	/**
	 * Retrieves the parsed OCR information of the page at index {@code pageNumber} of the
	 * document. All page numbers are 0-indexed.
	 *
	 * <p>This returns a TextAnnotation object which is Google Cloud Vision's representation of a
	 * page of a document. For more information on reading this object, see:
	 * https://cloud.google.com/vision/docs/reference/rpc/google.cloud.vision.v1#google.cloud.vision.v1.TextAnnotation
	 *
	 * @param pageNumber the zero-indexed page number of the document
	 * @return the {@link TextAnnotation} representing the page of the document
	 * @throws InvalidProtocolBufferException if the OCR information for the page failed to be
	 *     parsed
	 */
	public TextAnnotation getPage(int pageNumber) throws InvalidProtocolBufferException {
		Blob pageBlob = this.pageBlobs.get(pageNumber);
		return DocumentOcrTemplate.parseJsonBlob(pageBlob);
	}

	/**
	 * Returns an {@link Iterator} over all the OCR pages of the document.
	 *
	 * @return iterator of {@link TextAnnotation} describing OCR content of each page in the
	 * document.
	 */
	public Iterator<TextAnnotation> getAllPages() {
		return new Iterator<TextAnnotation>() {

			int currentPage = 0;

			@Override
			public boolean hasNext() {
				return currentPage < getPageCount();
			}

			@Override
			public TextAnnotation next() {
				if (!hasNext()) {
					throw new NoSuchElementException("No more pages left in DocumentOcrResultSet.");
				}

				try {
					TextAnnotation result = getPage(currentPage);
					currentPage++;
					return result;
				}
				catch (InvalidProtocolBufferException e) {
					throw new RuntimeException("Failed to process over document result set.", e);
				}
			}
		};
	}
}
