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

import java.util.ArrayList;
import java.util.Collections;

import com.google.cloud.storage.Blob;
import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class DocumentOcrResultSetTests {

	@Test
	public void testValidateBlobNames() {
		Blob blob = Mockito.mock(Blob.class);
		when(blob.getName()).thenReturn("output.json");

		assertThatThrownBy(() -> new DocumentOcrResultSet(Collections.singletonList(blob)))
				.hasMessageContaining("Cannot create a DocumentOcrResultSet with blob: ")
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testBlobRangeChecks() {
		Blob blob1 = Mockito.mock(Blob.class);
		when(blob1.getName()).thenReturn("blob-output-1-to-3.json");

		Blob blob2 = Mockito.mock(Blob.class);
		when(blob2.getName()).thenReturn("blob-output-4-to-6.json");

		ArrayList<Blob> blobs = new ArrayList<>();
		blobs.add(blob1);
		blobs.add(blob2);

		DocumentOcrResultSet documentOcrResultSet = new DocumentOcrResultSet(blobs);

		assertThat(documentOcrResultSet.getMinPage()).isEqualTo(1);
		assertThat(documentOcrResultSet.getMaxPage()).isEqualTo(6);

		assertThatThrownBy(() -> documentOcrResultSet.getPage(0))
				.hasMessageContaining("Page number out of bounds")
				.isInstanceOf(IndexOutOfBoundsException.class);

		assertThatThrownBy(() -> documentOcrResultSet.getPage(8))
				.hasMessageContaining("Page number out of bounds")
				.isInstanceOf(IndexOutOfBoundsException.class);
	}

}
