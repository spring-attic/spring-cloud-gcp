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

package org.springframework.integration.gcp.storage;

import com.google.cloud.storage.BlobInfo;

import org.springframework.integration.file.remote.ClientCallback;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.SessionFactory;

/**
 * @author João André Martins
 */
public class GcsRemoteFileTemplate extends RemoteFileTemplate<BlobInfo> {

	/**
	 * Construct a {@link RemoteFileTemplate} with the supplied session factory.
	 *
	 * @param sessionFactory the session factory.
	 */
	public GcsRemoteFileTemplate(SessionFactory<BlobInfo> sessionFactory) {
		super(sessionFactory);
	}

	@Override
	public <T, C> T executeWithClient(ClientCallback<C, T> callback) {
		return callback.doWithClient((C) this.sessionFactory.getSession().getClientInstance());
	}
}
