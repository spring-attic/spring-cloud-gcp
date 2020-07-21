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

import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.cloud.storage.BlobInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for GcsAcceptModifiedAfterFileListFilter.
 *
 * @author Hosain Al Ahmad
 */
class GcsAcceptModifiedAfterFileListFilterTest {

	@Test
	void addDiscardCallback() {
		GcsAcceptModifiedAfterFileListFilter filter = new GcsAcceptModifiedAfterFileListFilter();

		AtomicBoolean callbackTriggered = new AtomicBoolean(false);
		filter.addDiscardCallback(blobInfo -> callbackTriggered.set(true));

		BlobInfo blobInfo = mock(BlobInfo.class);
		when(blobInfo.getUpdateTime()).thenReturn(1L);

		filter.accept(blobInfo);

		assertThat(callbackTriggered.get())
				.isTrue();
	}

	@Test
	void filterFiles() {

		Instant now = Instant.now();

		BlobInfo oldBlob = mock(BlobInfo.class);
		when(oldBlob.getUpdateTime()).thenReturn(now.toEpochMilli() - 1);

		BlobInfo currentBlob = mock(BlobInfo.class);
		when(currentBlob.getUpdateTime()).thenReturn(now.toEpochMilli());

		BlobInfo newBlob = mock(BlobInfo.class);
		when(newBlob.getUpdateTime()).thenReturn(now.toEpochMilli() + 1);

		ArrayList<BlobInfo> expected = new ArrayList<>();
		expected.add(currentBlob);
		expected.add(newBlob);

		assertThat(
				new GcsAcceptModifiedAfterFileListFilter(now).filterFiles(new BlobInfo[] { oldBlob, currentBlob, newBlob }))
						.isEqualTo(expected);
	}

	@Test
	void supportsSingleFileFiltering() {
		assertThat(new GcsAcceptModifiedAfterFileListFilter().supportsSingleFileFiltering())
				.isTrue();
	}
}
