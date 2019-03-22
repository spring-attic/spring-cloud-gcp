/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.storage.integration.inbound;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

import org.springframework.cloud.gcp.storage.integration.GcsSessionFactory;
import org.springframework.cloud.gcp.storage.integration.filters.GcsPersistentAcceptOnceFileListFilter;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.metadata.SimpleMetadataStore;

/**
 * An inbound file synchronizer for Google Cloud Storage.
 *
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 */
public class GcsInboundFileSynchronizer extends AbstractInboundFileSynchronizer<BlobInfo> {

	public GcsInboundFileSynchronizer(Storage gcs) {
		super(new GcsSessionFactory(gcs));
		doSetFilter(new GcsPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "gcsMessageSource"));
	}

	@Override
	protected boolean isFile(BlobInfo file) {
		return true;
	}

	@Override
	protected String getFilename(BlobInfo file) {
		return file.getName();
	}

	@Override
	protected long getModified(BlobInfo file) {
		return file.getUpdateTime();
	}

}
