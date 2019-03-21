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

package org.springframework.cloud.gcp.storage.integration;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.session.SharedSessionCapable;
import org.springframework.util.Assert;

/**
 * A session factory implementation for Google Cloud Storage.
 *
 * @author João André Martins
 * @author Chengyuan Zhao
 */
public class GcsSessionFactory implements SessionFactory<BlobInfo>, SharedSessionCapable {

	private Storage gcs;

	public GcsSessionFactory(Storage gcs) {
		Assert.notNull(gcs, "The GCS client can't be null.");
		this.gcs = gcs;
	}

	@Override
	public Session<BlobInfo> getSession() {
		return new GcsSession(this.gcs);
	}

	@Override
	public boolean isSharedSession() {
		return true;
	}

	@Override
	public void resetSharedSession() {

	}

}
