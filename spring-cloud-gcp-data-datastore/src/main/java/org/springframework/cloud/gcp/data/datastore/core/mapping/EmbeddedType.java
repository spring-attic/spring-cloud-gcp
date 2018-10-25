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

package org.springframework.cloud.gcp.data.datastore.core.mapping;

import org.springframework.data.util.TypeInformation;

/**
 * The various types of properties with respect to their storage as embedded entities.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public enum EmbeddedType {

	/**
	 * These are properties that stored as singualr or arrays of Cloud Datastore native
	 * field types. This excludes the embedded entity field type.
	 */
	NOT_EMBEDDED,

	/**
	 * These are properties that are POJOs or collections of POJOs stored as a singular
	 * embedded entity or arrays of embedded entities in the field.
	 */
	EMBEDDED_ENTITY,

	/**
	 * These are {@code Map}s that are stored as a single embedded entity in the field.
	 */
	EMBEDDED_MAP;

	/**
	 * Get the {@link EmbeddedType} of a given {@link TypeInformation}.
	 * @param typeInformation the given type metadata to check for embedded type.
	 * @return the embedded type.
	 */
	public static EmbeddedType of(TypeInformation typeInformation) {
		EmbeddedType embeddedType;
		if (typeInformation.isMap()) {
			embeddedType = EmbeddedType.EMBEDDED_MAP;
		}
		else if ((typeInformation.isCollectionLike()
				&& typeInformation.getComponentType().getType().isAnnotationPresent(
						org.springframework.cloud.gcp.data.datastore.core.mapping.Entity.class))
				|| typeInformation.getType().isAnnotationPresent(
						org.springframework.cloud.gcp.data.datastore.core.mapping.Entity.class)) {
			embeddedType = EmbeddedType.EMBEDDED_ENTITY;
		}
		else {
			embeddedType = EmbeddedType.NOT_EMBEDDED;
		}
		return embeddedType;
	}

}
