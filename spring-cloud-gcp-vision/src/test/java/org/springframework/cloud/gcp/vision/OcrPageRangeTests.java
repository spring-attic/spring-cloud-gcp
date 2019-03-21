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

import com.google.cloud.storage.Blob;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OcrPageRangeTests {

	private static final byte[] SINGLE_JSON_OUTPUT_PAGE = "{'responses':[{'fullTextAnnotation': {'text': 'hello_world'}}]}"
			.getBytes();

	@Test
	public void testParseCorrectPageRange() {
		Blob blob = Mockito.mock(Blob.class);
		when(blob.getName()).thenReturn("blob-output-8-to-12.json");
		when(blob.getContent()).thenReturn(SINGLE_JSON_OUTPUT_PAGE);

		OcrPageRange ocrPageRange = new OcrPageRange(blob);
		assertThat(ocrPageRange.getStartPage()).isEqualTo(8);
		assertThat(ocrPageRange.getEndPage()).isEqualTo(12);
	}

	@Test
	public void testBlobCaching() throws InvalidProtocolBufferException {
		Blob blob = Mockito.mock(Blob.class);
		when(blob.getName()).thenReturn("blob-output-1-to-1.json");
		when(blob.getContent()).thenReturn(SINGLE_JSON_OUTPUT_PAGE);

		OcrPageRange ocrPageRange = new OcrPageRange(blob);

		assertThat(ocrPageRange.getPage(1).getText()).isEqualTo("hello_world");
		assertThat(ocrPageRange.getPage(1).getText()).isEqualTo("hello_world");
		assertThat(ocrPageRange.getPages()).hasSize(1);

		/* Retrieved content of blob 3 times, but getContent() only called once due to caching. */
		verify(blob, times(1)).getContent();
	}
}
