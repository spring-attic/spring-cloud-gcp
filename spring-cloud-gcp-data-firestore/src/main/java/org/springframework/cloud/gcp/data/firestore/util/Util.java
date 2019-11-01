/*
 * Copyright 2019-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.firestore.util;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Dmitry Solomakha
 */
public final class Util {

	private Util() {
	}

	public static String extractDatabasePath(String parent) {
		return parent.substring(0, StringUtils.ordinalIndexOf(parent, "/", 4));
	}

}
