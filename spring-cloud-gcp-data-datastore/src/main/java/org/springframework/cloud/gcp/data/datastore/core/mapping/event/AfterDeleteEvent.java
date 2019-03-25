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

import com.google.cloud.datastore.Key;

/**
 * An event published just after Spring Data Cloud Datastore performs a delete operation.
 *
 * @author Chengyuan Zhao
 */
public class AfterDeleteEvent extends DeleteEvent {

	/**
	 * Constructor.
	 *
	 * @param keysToDelete The keys that are deleted in this operation (never {@code null}).
	 * @param targetEntityClass The target entity type deleted. This may be {@code null}
	 *     depending on the specific delete operation.
	 * @param targetIds The target entity ID values deleted. This may be {@code null}
	 *     depending on the specific delete operation.
	 * @param targetEntities The target entity objects deleted. This may be {@code null}
	 *     depending on the specific
	 */
	public AfterDeleteEvent(Key[] keysToDelete, Class targetEntityClass, Iterable targetIds, Iterable targetEntities) {
		super(keysToDelete, targetEntityClass, targetIds, targetEntities);
	}
}
