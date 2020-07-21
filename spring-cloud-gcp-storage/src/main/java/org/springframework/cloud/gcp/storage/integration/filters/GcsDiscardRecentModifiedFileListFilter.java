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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.cloud.storage.BlobInfo;

import org.springframework.integration.file.filters.DiscardAwareFileListFilter;
import org.springframework.lang.Nullable;

/**
 * The {@link GcsDiscardRecentModifiedFileListFilter} is a filter which excludes all files
 * that were updated less than some specified amount of time ago.
 *
 * <p>More specifically, it excludes all files whose {@link BlobInfo#getUpdateTime()} is
 * within {@link #age} of the current time.
 *
 * <p>When {@link #discardCallback} is provided, it called for all the rejected files.
 *
 * @author Hosain Al Ahmad
 */
public class GcsDiscardRecentModifiedFileListFilter implements DiscardAwareFileListFilter<BlobInfo> {

	private final Duration age;

	@Nullable
	private Consumer<BlobInfo> discardCallback;

	/**
	 * Construct a {@link GcsDiscardRecentModifiedFileListFilter} instance with provided {@link #age}.
	 *
	 * @param age {@link Duration} describing the age of files to filter.
	 */
	public GcsDiscardRecentModifiedFileListFilter(Duration age) {
		this.age = age;
	}

	@Override
	public void addDiscardCallback(@Nullable Consumer<BlobInfo> discardCallbackToSet) {
		this.discardCallback = discardCallbackToSet;
	}

	@Override
	public List<BlobInfo> filterFiles(BlobInfo[] files) {
		List<BlobInfo> list = new ArrayList<>();
		for (BlobInfo file : files) {
			if (accept(file)) {
				list.add(file);
			}
		}
		return list;
	}

	@Override
	public boolean accept(BlobInfo file) {
		if (fileIsAged(file)) {
			return true;
		}
		else if (this.discardCallback != null) {
			this.discardCallback.accept(file);
		}
		return false;
	}

	private boolean fileIsAged(BlobInfo file) {
		return file.getUpdateTime() + this.age.toMillis() <= System.currentTimeMillis();
	}

	@Override
	public boolean supportsSingleFileFiltering() {
		return true;
	}
}
