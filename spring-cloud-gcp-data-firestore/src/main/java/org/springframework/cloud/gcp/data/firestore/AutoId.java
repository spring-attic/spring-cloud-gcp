/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.cloud.gcp.data.firestore;

import java.security.SecureRandom;
import java.util.Random;

/**
 * This class is used to generate Id values.
 * Copied from com.google.cloud.firestore.FirestoreImpl
 *
 * @author Dmitry Solomakha
 * @since 1.2.4
 */

final class AutoId {
	private static final String AUTO_ID_ALPHABET =
					"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	private static final Random RANDOM = new SecureRandom();

	private static final int AUTO_ID_LENGTH = 20;

	private AutoId() {
	}

	//Creates a pseudo-random 20-character ID that can be used for Firestore documents.
	static String autoId() {
		StringBuilder builder = new StringBuilder();
		int maxRandom = AUTO_ID_ALPHABET.length();
		for (int i = 0; i < AUTO_ID_LENGTH; i++) {
			builder.append(AUTO_ID_ALPHABET.charAt(RANDOM.nextInt(maxRandom)));
		}
		return builder.toString();
	}
}
