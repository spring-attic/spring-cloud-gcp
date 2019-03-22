/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.datastore.core.mapping.event;

import java.util.List;
import java.util.Objects;

import com.google.cloud.datastore.Entity;

/**
 * An event published immediately after a save operation to Cloud Datastore.
 *
 * @author Chengyuan Zhao
 */
public class AfterSaveEvent extends SaveEvent {

	private final List<Entity> datastoreEntities;

	/**
	 * Constructor.
	 *
	 * @param datastoreEntities The Cloud Datastore entities that are being saved. These
	 *     include any referenced or descendant entities of the original entities being saved.
	 * @param javaEntities The original Java entities being saved. Each entity may result in
	 */
	public AfterSaveEvent(List<Entity> datastoreEntities, List javaEntities) {
		super(javaEntities);
		this.datastoreEntities = datastoreEntities;
	}

	/**
	 * Get the Cloud Datastore entities that were saved.
	 * @return The entities that were saved in Cloud Datastore.
	 */
	public List<Entity> getDatastoreEntities() {
		return this.datastoreEntities;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AfterSaveEvent that = (AfterSaveEvent) o;
		return getDatastoreEntities().containsAll(that.getDatastoreEntities())
				&& Objects.equals(getTargetEntities(), that.getTargetEntities());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getDatastoreEntities(), getTargetEntities());
	}
}
