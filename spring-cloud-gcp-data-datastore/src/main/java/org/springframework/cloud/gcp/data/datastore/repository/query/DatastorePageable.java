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

package org.springframework.cloud.gcp.data.datastore.repository.query;

import com.google.cloud.datastore.Cursor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * A pageable implementation for Cloud Datastore that uses the cursor for efficient reads.
 *
 * @author Dmitry Solomakha
 * @author Chengyuan Zhao
 */
public class DatastorePageable extends PageRequest {
	private final String urlSafeCursor;

	private final Long totalCount;

	private DatastorePageable(Pageable pageable, String urlSafeCursor, Long totalCount) {
		super(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
		this.urlSafeCursor = urlSafeCursor;
		this.totalCount = totalCount;
	}

	DatastorePageable(Pageable pageable, Cursor cursor, Long totalCount) {
		this(pageable, cursor.toUrlSafe(), totalCount);
	}

	public static Pageable from(Pageable pageable, Cursor cursor, Long totalCount) {
		return from(pageable, cursor == null ? null : cursor.toUrlSafe(), totalCount);
	}

	public static Pageable from(Pageable pageable, String urlSafeCursor, Long totalCount) {
		if (pageable.isUnpaged()) {
			return pageable;
		}
		return new DatastorePageable(pageable, urlSafeCursor, totalCount);
	}

	public String getUrlSafeCursor() {
		return this.urlSafeCursor;
	}

	@Override
	public Pageable next() {
		return from(super.next(), this.urlSafeCursor, this.totalCount);
	}

	public Cursor toCursor() {
		return this.urlSafeCursor == null ? null : Cursor.fromUrlSafe(this.urlSafeCursor);
	}

	public Long getTotalCount() {
		return this.totalCount;
	}
}
