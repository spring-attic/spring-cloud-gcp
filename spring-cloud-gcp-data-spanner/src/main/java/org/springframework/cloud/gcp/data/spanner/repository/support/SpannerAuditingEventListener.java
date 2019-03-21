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

package org.springframework.cloud.gcp.data.spanner.repository.support;

import org.springframework.cloud.gcp.data.spanner.core.mapping.event.BeforeSaveEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.auditing.AuditingHandler;

/**
 * Auditing event listener that listens for {@code BeforeSaveEvent}.
 *
 * @author Chengyuan Zhao
 * @since 1.2
 */
public class SpannerAuditingEventListener implements ApplicationListener<BeforeSaveEvent> {

	private final AuditingHandler handler;

	/**
	 * Constructor.
	 *
	 * @param spannerAuditingHandler the auditing handler to set auditing properties.
	 */
	public SpannerAuditingEventListener(AuditingHandler spannerAuditingHandler) {
		this.handler = spannerAuditingHandler;
	}

	@Override
	public void onApplicationEvent(BeforeSaveEvent event) {
		event.getTargetEntities().forEach(this.handler::markModified);
	}
}
