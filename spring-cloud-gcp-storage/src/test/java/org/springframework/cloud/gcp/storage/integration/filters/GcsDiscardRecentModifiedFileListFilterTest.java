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

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.cloud.storage.BlobInfo;
import org.junit.Assert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GcsDiscardRecentModifiedFileListFilterTest {

	@Test
	public void testFileLessThanMinimumAgeIsFilteredOut() {
		GcsDiscardRecentModifiedFileListFilter filter = new GcsDiscardRecentModifiedFileListFilter(Duration.ofSeconds(60));
		AtomicBoolean callbackTriggered = new AtomicBoolean(false);
		filter.addDiscardCallback(blobInfo -> callbackTriggered.set(true));

		BlobInfo blobInfo = mock(BlobInfo.class);
		when(blobInfo.getUpdateTime())
				.thenReturn(ZonedDateTime.now(ZoneId.systemDefault()).toInstant().toEpochMilli());

		assertThat(filter.filterFiles(new BlobInfo[] { blobInfo })).hasSize(0);
		assertThat(filter.accept(blobInfo)).isFalse();
		assertThat(callbackTriggered).isTrue();
		assertThat(filter.supportsSingleFileFiltering()).isTrue();
	}

	@Test
	public void testFileOlderThanMinimumAgeIsReturned() {
		GcsDiscardRecentModifiedFileListFilter filter = new GcsDiscardRecentModifiedFileListFilter(Duration.ofSeconds(60));
		filter.addDiscardCallback(blobInfo -> Assert.fail("Not expected"));

		BlobInfo blobInfo = mock(BlobInfo.class);
		when(blobInfo.getUpdateTime())
				.thenReturn(ZonedDateTime.now(ZoneId.systemDefault()).minusMinutes(3).toInstant().toEpochMilli());

		assertThat(filter.filterFiles(new BlobInfo[] { blobInfo })).hasSize(1);
		assertThat(filter.accept(blobInfo)).isTrue();
	}
}
