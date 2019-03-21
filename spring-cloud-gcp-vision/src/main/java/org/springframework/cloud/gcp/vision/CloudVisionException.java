/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.vision;

import org.springframework.core.NestedRuntimeException;

/**
 * Describes error conditions that can occur when interfacing with Cloud Vision APIs.
 *
 * @author Daniel Zou
 *
 * @since 1.1
 */
public final class CloudVisionException extends NestedRuntimeException {

	private static final long serialVersionUID = 1L;

	CloudVisionException(String message, Exception errorCause) {
		super(message, errorCause);
	}

	CloudVisionException(String message) {
		super(message);
	}
}
