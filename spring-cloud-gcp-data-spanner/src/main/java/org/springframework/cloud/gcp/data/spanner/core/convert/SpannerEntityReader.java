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

package org.springframework.cloud.gcp.data.spanner.core.convert;

import java.util.Set;

import com.google.cloud.spanner.Struct;

import org.springframework.data.convert.EntityReader;

/**
 * @author Balint Pato
 *
 * @since 1.1
 */
interface SpannerEntityReader extends EntityReader<Object, Struct> {

	<R> R read(Class<R> type, Struct source, Set<String> includeColumns,
			boolean allowMissingColumns);

	@Override
	default <R> R read(Class<R> type, Struct source) {
		return read(type, source, null, false);
	}
}
