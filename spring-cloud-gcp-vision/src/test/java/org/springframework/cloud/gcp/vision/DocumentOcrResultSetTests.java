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

import java.util.Collections;

import com.google.cloud.storage.Blob;
import org.junit.Test;
import org.mockito.Mockito;

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
}
