/*
 *  Copyright 2017 original author or authors.
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

package org.springframework.cloud.gcp.pubsub.core;

import org.springframework.core.NestedRuntimeException;

/**
 * The Spring Google Cloud Pub/Sub specific {@link NestedRuntimeException}.
 *
 * @author João André Martins
 */
public class PubSubException extends NestedRuntimeException {

	public PubSubException(String msg) {
		super(msg);
	}

	public PubSubException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
