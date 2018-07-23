/*
 *  Copyright 2018 original author or authors.
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

package org.springframework.cloud.gcp.data.datastore.core.convert;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.KeyFactory;

import org.springframework.util.Assert;

/**
 * Implementation of {@link ObjectToKeyFactory} where the key factories are provided by
 * the Datastore Service.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class DatastoreServiceObjectToKeyFactory implements ObjectToKeyFactory {

	private final Datastore datastore;

	public DatastoreServiceObjectToKeyFactory(Datastore datastore) {
		Assert.notNull(datastore, "A non-null Datastore service is required.");
		this.datastore = datastore;
	}

	@Override
	public KeyFactory getKeyFactory() {
		return this.datastore.newKeyFactory();
	}
}
