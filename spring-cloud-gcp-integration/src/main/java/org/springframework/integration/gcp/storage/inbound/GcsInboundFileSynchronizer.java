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

package org.springframework.integration.gcp.storage.inbound;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.gcp.storage.GcsSessionFactory;

/**
 * @author João André Martins
 */
public class GcsInboundFileSynchronizer extends AbstractInboundFileSynchronizer<BlobInfo> {

	public GcsInboundFileSynchronizer(Storage gcs) {
		super(new GcsSessionFactory(gcs));
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
