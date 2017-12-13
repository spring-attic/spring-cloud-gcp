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

import java.io.File;
import java.util.Comparator;

import com.google.cloud.storage.BlobInfo;

import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizingMessageSource;

/**
 * @author João André Martins
 */
public class GCSInboundFileSynchronizingMessageSource extends AbstractInboundFileSynchronizingMessageSource<BlobInfo> {

	public GCSInboundFileSynchronizingMessageSource(GCSInboundFileSynchronizer synchronizer) {
		super(synchronizer);
	}

	public GCSInboundFileSynchronizingMessageSource(GCSInboundFileSynchronizer synchronizer,
			Comparator<File> comparator) {
		super(synchronizer, comparator);
	}

	@Override
	public String getComponentType() {
		return "gcp:gcs-inbound-streaming-channel-adapter";
	}
}
