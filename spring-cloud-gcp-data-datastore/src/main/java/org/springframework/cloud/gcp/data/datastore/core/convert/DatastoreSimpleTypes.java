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

package org.springframework.cloud.gcp.data.datastore.core.convert;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.LatLng;

import org.springframework.data.mapping.model.SimpleTypeHolder;


/**
 * Simple constant holder for a {@link SimpleTypeHolder} enriched with Mongo specific simple types.
 *
 * @author Dmitry Solomakha
 */
public abstract class DatastoreSimpleTypes {

	public static final Set<Class<?>> ID_TYPES;

	static {
		Set<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(String.class);
		classes.add(Long.class);
		ID_TYPES = Collections.unmodifiableSet(classes);

		Set<Class<?>> simpleTypes = new HashSet<Class<?>>();
		simpleTypes.add(String.class);
		simpleTypes.add(Boolean.class);
		simpleTypes.add(Double.class);
		simpleTypes.add(Long.class);
		simpleTypes.add(LatLng.class);
		simpleTypes.add(Timestamp.class);
		simpleTypes.add(Blob.class);


		DATASTORE_SIMPLE_TYPES = Collections.unmodifiableSet(simpleTypes);
	}

	public static final Set<Class<?>> DATASTORE_SIMPLE_TYPES;

	public static final SimpleTypeHolder HOLDER = new SimpleTypeHolder(DATASTORE_SIMPLE_TYPES, true);

	private DatastoreSimpleTypes() { }
}
