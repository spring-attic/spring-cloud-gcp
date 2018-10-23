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

/**
 * The various types of properties with respect to their storage as embedded entities.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class EmbeddedStatus {

	private final int embeddedMapDepthAllowance;

	private final EmbeddedType embeddedType;

	public EmbeddedStatus(int embeddedMapDepthAllowance, EmbeddedType embeddedType) {
		this.embeddedMapDepthAllowance = Math.max(0, embeddedMapDepthAllowance);
		this.embeddedType = embeddedType;
	}

	/**
	 * Get the maximum depth of nested embedded maps of the property this object
	 * describes.
	 * @return the maximum depth of nested embedded maps.
	 */
	public int getEmbeddedMapDepthAllowance() {
		return this.embeddedMapDepthAllowance;
	}

	/**
	 * Get the embedded type of the property this object describes.
	 * @return the embedded type.
	 */
	public EmbeddedType getEmbeddedType() {
		return this.embeddedType;
	}

	public enum EmbeddedType {
		/**
		 * These are properties that stored as singualr or arrays of Cloud Datastore
		 * native field types. This excludes the embedded entity field type.
		 */
		NOT_EMBEDDED,

		/**
		 * These are properties that are stored as a singular embedded entity in the
		 * field.
		 */
		SINGULAR_EMBEDDED,

		/**
		 * These are properties stored as arrays of embedded entities in the field.
		 */
		COLLECTION_EMBEDDED_ELEMENTS,

		/**
		 * These are {@code Map}s that are stored as a single embedded entity in the
		 * field.
		 */
		EMBEDDED_MAP;
	}
}
