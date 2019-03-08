/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.context.ApplicationEvent;
import org.springframework.util.Assert;

/**
 * An event published when entities are saved to Cloud Datastore.
 *
 * @author Chengyuan Zhao
 */
public class SaveEvent extends ApplicationEvent {

	private final List javaEntities;

	/**
	 * Constructor.
	 *
	 * @param datastoreEntities The Cloud Datastore entities that are being saved. These
	 *     include any referenced or descendant entities of the original entities being saved.
	 * @param javaEntities The original Java entities being saved. Each entity may result in
	 *     multiple Datastore entities being saved due to relationships.
	 */
	public SaveEvent(List<Entity> datastoreEntities, List javaEntities) {
		super(datastoreEntities);
		Assert.notNull(javaEntities, "The entities being saved must not be null.");
		this.javaEntities = javaEntities;
	}

	/**
	 * Get the Cloud Datastore entities that were saved.
	 * @return The entities that were saved in Cloud Datastore.
	 */
	public List<Entity> getDatastoreEntities() {
		return (List<Entity>) getSource();
	}

	/**
	 * Get the original Java objects that were saved.
	 * @return The original Java objects that were saved to Cloud Datastore.
	 */
	public List getJavaEntities() {
		return this.javaEntities;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SaveEvent that = (SaveEvent) o;
		return getDatastoreEntities().containsAll(that.getDatastoreEntities())
				&& Objects.equals(getJavaEntities(), that.getJavaEntities());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getDatastoreEntities(), getJavaEntities());
	}
}
