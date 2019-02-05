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

import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;

/**
 * An event that is published after a delete operation.
 *
 * @author Chengyuan Zhao
 */
public class AfterDeleteEvent extends DeleteEvent {

	/**
	 * Constructor.
	 *
	 * @param source the mutations for the event initially occurred. (never {@code null})
	 * @param targetEntities the target entities that need to be deleted. This may be
	 *     {@code null} depending on the type of delete request.
	 * @param targetKeys the target keys that need to be deleted. This may be {@code null}
	 *     depending on the type of delete request.
	 * @param targetType the target entity type that needs to be deleted. This may be
	 *     {@code null} depending on the
	 */
	public AfterDeleteEvent(List<Mutation> source, Iterable targetEntities, KeySet targetKeys, Class targetType) {
		super(source, targetEntities, targetKeys, targetType);
	}
}
