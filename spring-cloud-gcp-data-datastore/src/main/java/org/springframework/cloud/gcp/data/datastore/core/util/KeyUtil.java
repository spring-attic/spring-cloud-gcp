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

package org.springframework.cloud.gcp.data.datastore.core.util;

import com.google.cloud.datastore.Key;

/**
 * Utility class containing methods for transforming and manipulating Datastore keys.
 *
 * @author Daniel Zou
 *
 * @since 1.2
 */
public final class KeyUtil {

	private KeyUtil() {
	}

	/**
	 * Returns a copy of the provided {@code entityKey} with its ancestors removed.
	 *
	 * <p>This is useful for performing HAS_ANCESTOR queries in Datastore which do not
	 * expect a fully qualified ancestors key list in the provided key.
	 *
	 * @param entityKey The Datastore entity key to transform.
	 * @return A copy of the {@code entityKey} with ancestors removed.
	 */
	public static Key getKeyWithoutAncestors(Key entityKey) {
		Key.Builder ancestorLookupKey;
		if (entityKey.hasName()) {
			ancestorLookupKey = Key.newBuilder(entityKey.getProjectId(), entityKey.getKind(), entityKey.getName());
		}
		else {
			ancestorLookupKey = Key.newBuilder(entityKey.getProjectId(), entityKey.getKind(), entityKey.getId());
		}
		ancestorLookupKey.setNamespace(entityKey.getNamespace());

		return ancestorLookupKey.build();
	}
}
