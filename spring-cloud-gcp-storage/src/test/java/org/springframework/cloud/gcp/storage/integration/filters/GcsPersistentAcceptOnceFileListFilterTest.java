/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.cloud.gcp.storage.integration.filters;

import com.google.cloud.storage.BlobInfo;
import org.junit.Test;

import org.springframework.integration.metadata.ConcurrentMetadataStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for GcsPersistentAcceptOnceFileListFilter.
 *
 * @author Lukas Gemela
 */
public class GcsPersistentAcceptOnceFileListFilterTest {

	@Test
	public void modified_blobInfoIsNull_shouldReturnMinusOne() {
		assertThat(new GcsPersistentAcceptOnceFileListFilter(mock(ConcurrentMetadataStore.class), "").modified(null))
				.isEqualTo(-1);
	}

	@Test
	public void modified_updateTimeIsNull_shouldReturnMinusOne() {
		BlobInfo blobInfo = mock(BlobInfo.class);
		when(blobInfo.getUpdateTime()).thenReturn(null);

		assertThat(
				new GcsPersistentAcceptOnceFileListFilter(mock(ConcurrentMetadataStore.class), "").modified(blobInfo))
						.isEqualTo(-1);
	}
}
