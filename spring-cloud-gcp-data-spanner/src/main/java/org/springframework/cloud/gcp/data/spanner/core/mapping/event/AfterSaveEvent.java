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

package org.springframework.cloud.gcp.data.spanner.core.mapping.event;

import java.util.List;
import java.util.Set;

import com.google.cloud.spanner.Mutation;

/**
 * An event that is published after a save operation to Cloud Spanner.
 *
 * @author Chengyuan Zhao
 */
public class AfterSaveEvent extends SaveEvent {
	/**
	 * Constructor.
	 *
	 * @param source the mutations for the event initially occurred. (never {@code null})
	 * @param targetEntities the target entities that need to be mutated. This may be
	 *     {@code null} depending on the original request.
	 * @param includeProperties the set of properties to include in the save operation.
	 */
	public AfterSaveEvent(List<Mutation> source, Iterable targetEntities, Set<String> includeProperties) {
		super(source, targetEntities, includeProperties);
	}
}
