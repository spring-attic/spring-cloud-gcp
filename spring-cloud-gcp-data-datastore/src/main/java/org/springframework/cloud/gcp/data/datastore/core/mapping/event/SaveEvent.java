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

import org.springframework.context.ApplicationEvent;

/**
 * An event published when entities are saved to Cloud Datastore.
 *
 * @author Chengyuan Zhao
 */
public class SaveEvent extends ApplicationEvent {

	/**
	 * Constructor.
	 *
	 * @param entities The original Java entities being saved. Each entity may result in
	 *     multiple Datastore entities being saved due to relationships.
	 */
	public SaveEvent(List entities) {
		super(entities);
	}

	/**
	 * Get the original Java objects that were saved.
	 * @return The original Java objects that were saved to Cloud Datastore.
	 */
	public List getTargetEntities() {
		return (List) getSource();
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
		return Objects.equals(getTargetEntities(), that.getTargetEntities());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getTargetEntities());
	}
}
