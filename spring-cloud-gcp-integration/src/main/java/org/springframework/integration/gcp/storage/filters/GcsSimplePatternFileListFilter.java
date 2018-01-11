/*
 *  Copyright 2017 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.integration.gcp.storage.filters;

import com.google.cloud.storage.BlobInfo;

import org.springframework.integration.file.filters.AbstractSimplePatternFileListFilter;

/**
 * @author João André Martins
 */
public class GcsSimplePatternFileListFilter extends AbstractSimplePatternFileListFilter<BlobInfo> {

	public GcsSimplePatternFileListFilter(String path) {
		super(path);
	}

	@Override
	protected String getFilename(BlobInfo blobInfo) {
		return blobInfo != null ? blobInfo.getName() : null;
	}

	@Override
	protected boolean isDirectory(BlobInfo blobInfo) {
		return blobInfo.isDirectory();
	}
}
