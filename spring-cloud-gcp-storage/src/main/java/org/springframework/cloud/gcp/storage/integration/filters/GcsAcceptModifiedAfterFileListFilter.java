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
 * The {@link GcsAcceptModifiedAfterFileListFilter} is a filter which accepts all files
 * that were modified after a specified point in time.
 *
 * <p>More specifically, it accepts (includes) all files whose {@link BlobInfo#getUpdateTime()} is
 * after (greater than or equal to) the {@link #acceptAfterCutoffTimestamp}.
 *
 * <p>{@link #acceptAfterCutoffTimestamp} defaults to Instant.now() (UTC) in millis,
 * but an alternative {@link Instant} can be provided via the constructor.
 *
 * <p>When {@link #discardCallback} is provided, it is called for all the rejected files.
 *
 * @author Hosain Al Ahmad
 */
public class GcsAcceptModifiedAfterFileListFilter implements DiscardAwareFileListFilter<BlobInfo> {

	private final long acceptAfterCutoffTimestamp;

	@Nullable
	private Consumer<BlobInfo> discardCallback;

	public GcsAcceptModifiedAfterFileListFilter() {
		acceptAfterCutoffTimestamp = Instant.now(Clock.systemUTC()).toEpochMilli();
	}

	public GcsAcceptModifiedAfterFileListFilter(Instant instant) {
		acceptAfterCutoffTimestamp = instant.toEpochMilli();
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
		return file.getUpdateTime() >= acceptAfterCutoffTimestamp;
	}

	@Override
	public boolean supportsSingleFileFiltering() {
		return true;
	}
}
