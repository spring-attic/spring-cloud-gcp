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

package org.springframework.cloud.gcp.storage;

import com.google.cloud.storage.Storage;

/**
 * Holds {@link Storage} and associated settings for use with the
 * {@link GoogleStorageResource}.
 */
public class GoogleStorageProtocolResolverContext {

	private final Storage storage;
	private final boolean autoCreateFiles;

	public GoogleStorageProtocolResolverContext(Storage storage,
			boolean autoCreateFiles) {
		this.storage = storage;
		this.autoCreateFiles = autoCreateFiles;
	}

	public Storage getStorage() {
		return this.storage;
	}

	public boolean isAutoCreateFiles() {
		return this.autoCreateFiles;
	}
}
