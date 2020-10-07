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

package com.google.cloud.spring.data.firestore.util;

/**
 * @author Dmitry Solomakha
 */
public final class Util {

	private Util() {
	}

	public static String extractDatabasePath(String parent) {
		//the parent looks like this: projects/{project_id}/databases/{database_id}/...
		//and the database path is the first 4 segments, separated by /
		StringBuilder sb = new StringBuilder();
		int count = 0;
		for (int i = 0; i < parent.length(); i++) {
			char c = parent.charAt(i);
			if (c == '/') {
				count++;
			}
			if (count == 4) {
				break;
			}
			sb.append(c);
		}
		return sb.toString();
	}
}
