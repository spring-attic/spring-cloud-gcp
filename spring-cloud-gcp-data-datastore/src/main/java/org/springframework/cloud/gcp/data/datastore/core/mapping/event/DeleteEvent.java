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

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import com.google.cloud.datastore.Key;

import org.springframework.context.ApplicationEvent;

/**
 * An event published when Spring Data Cloud Datastore performs a delete operation.
 *
 * @author Chengyuan Zhao
 */
public class DeleteEvent extends ApplicationEvent {

	private final Optional<Class> targetEntityClass;

	private final Optional<Iterable> targetIds;

	private final Optional<Iterable> targetEntities;

	/**
	 * Constructor.
	 *
	 * @param keysToDelete The keys that are deleted in this operation (never {@code null}).
	 * @param targetEntityClass The target entity type deleted. This may be {@code null}
	 *     depending on the specific delete operation.
	 * @param targetIds The target entity ID values deleted. This may be {@code null}
	 *     depending on the specific delete operation.
	 * @param targetEntities The target entity objects deleted. This may be {@code null}
	 *     depending on the specific delete operation.
	 */
	public DeleteEvent(Key[] keysToDelete, Class targetEntityClass, Iterable targetIds, Iterable targetEntities) {
		super(keysToDelete);
		this.targetEntityClass = Optional.ofNullable(targetEntityClass);
		this.targetIds = Optional.ofNullable(targetIds);
		this.targetEntities = Optional.ofNullable(targetEntities);
	}

	/**
	 * Get the keys that were deleted in this operation.
	 * @return the array of keys.
	 */
	public Key[] getKeys() {
		return (Key[]) getSource();
	}

	/**
	 * Get the target entity type deleted.
	 * @return This may be empty depending on the specific delete operation.
	 */
	public Optional<Class> getOptionalTargetEntityClass() {
		return this.targetEntityClass;
	}

	/**
	 * Get the target entity ID values deleted.
	 * @return This may be empty depending on the specific delete operation.
	 */
	public Optional<Iterable> getOptionalTargetIds() {
		return this.targetIds;
	}

	/**
	 * Get thetarget entity objects deleted.
	 * @return This may be empty depending on the specific delete operation.
	 */
	public Optional<Iterable> getOptionalTargetEntities() {
		return this.targetEntities;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DeleteEvent that = (DeleteEvent) o;
		return Arrays.equals(getKeys(), that.getKeys())
				&& Objects.equals(getOptionalTargetEntities(), that.getOptionalTargetEntities())
				&& Objects.equals(getOptionalTargetIds(), that.getOptionalTargetIds())
				&& Objects.equals(getOptionalTargetEntityClass(), that.getOptionalTargetEntityClass());
	}

	@Override
	public int hashCode() {
		return Objects.hash(Arrays.hashCode(getKeys()), getOptionalTargetEntities(), getOptionalTargetIds(),
				getOptionalTargetEntityClass());
	}
}
