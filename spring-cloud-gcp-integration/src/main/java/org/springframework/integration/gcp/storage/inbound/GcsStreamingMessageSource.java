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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.cloud.storage.BlobInfo;

import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.AbstractRemoteFileStreamingMessageSource;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.gcp.storage.GcsFileInfo;

/**
 * @author João André Martins
 */
public class GcsStreamingMessageSource extends AbstractRemoteFileStreamingMessageSource<BlobInfo> {

	public GcsStreamingMessageSource(RemoteFileTemplate<BlobInfo> template) {
		super(template, null);
	}

	@Override
	protected List<AbstractFileInfo<BlobInfo>> asFileInfoList(Collection<BlobInfo> collection) {
		return collection.stream()
				.map(GcsFileInfo::new)
				.collect(Collectors.toList());
	}

	@Override
	public String getComponentType() {
		return "gcp:gcs-inbound-streaming-channel-adapter";
	}

	@Override
	public void setRemoteFileSeparator(String remoteFileSeparator) {
		throw new UnsupportedOperationException("Google Cloud Storage doesn't support separators other than '/'.");
	}
}
