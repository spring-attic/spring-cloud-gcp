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

package org.springframework.cloud.gcp.data.spanner.core.mapping.event;

import java.util.List;
import java.util.Objects;

import com.google.cloud.spanner.Mutation;

import org.springframework.context.ApplicationEvent;

/**
 * An event holding mutations that are submitted to Cloud Spanner.
 *
 * @author Chengyuan Zhao
 */
public class MutationEvent extends ApplicationEvent {

	private final Iterable targetEntities;

	/**
	 * Constructor.
	 * @param source the mutations for the event initially occurred. (never {@code null})
	 * @param targetEntities the target entities that need to be mutated. This may be
	 *     {@code null} depending on the type of delete request.
	 */
	public MutationEvent(List<Mutation> source, Iterable targetEntities) {
		super(source);
		this.targetEntities = targetEntities;
	}

	/**
	 * Get the mutations underlying this event.
	 * @return the list of mutations.
	 */
	public List<Mutation> getMutations() {
		return (List<Mutation>) getSource();
	}

	/**
	 * Get the list of entities that needed to be deleted.
	 * @return This may be {@code null} depending on the type of delete request.
	 */
	public Iterable getTargetEntities() {
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
		MutationEvent that = (MutationEvent) o;
		return getMutations().equals(that.getMutations())
				&& Objects.equals(getTargetEntities(), that.getTargetEntities());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getMutations().hashCode(), getTargetEntities());
	}
}
