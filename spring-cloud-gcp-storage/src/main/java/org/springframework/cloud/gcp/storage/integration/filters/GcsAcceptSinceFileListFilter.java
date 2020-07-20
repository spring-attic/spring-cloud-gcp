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

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.cloud.storage.BlobInfo;

import org.springframework.integration.file.filters.DiscardAwareFileListFilter;

/**
 * The {@link GcsAcceptSinceFileListFilter} is a filter which excludes all files
 * that are older than a specific point in time.
 *
 * <p>More specifically, it excludes all files whose {@link BlobInfo#getUpdateTime()} is
 * less than {@link #millisSince}.
 *
 * <p>{@link #millisSince} defaults to Instant.now() (UTC) in millis, but an alternative {@link Instant}
 * can be provided via the constructor.</p>
 *
 * <p>When {@link #discardCallback} is provided, it called for all the rejected files.
 *
 * @author Hosain Al Ahmad
 */
public class GcsAcceptSinceFileListFilter implements DiscardAwareFileListFilter<BlobInfo> {

	private final long millisSince;

	@Nullable
	private Consumer<BlobInfo> discardCallback;

	public GcsAcceptSinceFileListFilter() {
		millisSince = Instant.now(Clock.systemUTC()).toEpochMilli();
	}

	public GcsAcceptSinceFileListFilter(Instant instant) {
		millisSince = instant.toEpochMilli();
	}

	@Override
	public void addDiscardCallback(@Nullable Consumer<BlobInfo> discardCallback) {
		this.discardCallback = discardCallback;
	}

	@Override
	public List<BlobInfo> filterFiles(BlobInfo[] blobInfos) {
		List<BlobInfo> list = new ArrayList<>();
		for (BlobInfo file : blobInfos) {
			if (accept(file)) {
				list.add(file);
			}
		}
		return list;
	}

	@Override
	public boolean accept(BlobInfo file) {
		if (fileUpdateTimeOnOrAfterPointInTime(file)) {
			return true;
		}
		else if (this.discardCallback != null) {
			this.discardCallback.accept(file);
		}
		return false;
	}

	private boolean fileUpdateTimeOnOrAfterPointInTime(BlobInfo file) {
		return file.getUpdateTime() >= millisSince;
	}

	@Override
	public boolean supportsSingleFileFiltering() {
		return true;
	}
}
