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

package org.springframework.integration.gcp.storage.outbound;

import com.google.cloud.storage.BlobInfo;

import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;

/**
 * @author João André Martins
 */
public class GcsMessageHandler extends FileTransferringMessageHandler<BlobInfo> {

	public GcsMessageHandler(SessionFactory<BlobInfo> sessionFactory) {
		super(sessionFactory);
	}

	public GcsMessageHandler(RemoteFileTemplate<BlobInfo> remoteFileTemplate) {
		super(remoteFileTemplate);
	}

	public GcsMessageHandler(RemoteFileTemplate<BlobInfo> remoteFileTemplate, FileExistsMode mode) {
		super(remoteFileTemplate, mode);
	}

	@Override
	public void setRemoteFileSeparator(String remoteFileSeparator) {
		throw new UnsupportedOperationException("Google Cloud Storage doesn't support separators other than '/'.");
	}
}
