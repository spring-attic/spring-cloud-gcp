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

package org.springframework.cloud.gcp.data.datastore.core;

import java.util.ArrayList;
import java.util.List;

import com.google.cloud.datastore.ReadOption;

import org.springframework.util.Assert;

/**
 * Options for performing reads and queries in Cloud Datastore.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class DatastoreReadOptions {

	private final List<ReadOption> readOptions = new ArrayList<>();

	/**
	 * Determine if there are read options.
	 * @return true if there are read options. false otherwise.
	 */
	public boolean hasReadOptions() {
		return !this.readOptions.isEmpty();
	}

	/**
	 * Get the read options.
	 * @return an array of the read options.
	 */
	public ReadOption[] getReadOptions() {
		return this.readOptions.toArray(new ReadOption[0]);
	}

	/**
	 * Add a single read option.
	 * @param readOption the read option to add.
	 */
	public DatastoreReadOptions addReadOption(ReadOption readOption) {
		Assert.notNull(readOption, "Cannot add a null ReadOption.");
		this.readOptions.add(readOption);
		return this;
	}
}
