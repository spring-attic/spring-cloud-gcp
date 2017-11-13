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

package org.springframework.cloud.gcp.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;

import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;

/**
 * Implements {@link WritableResource} for buckets in Google Cloud Storage.
 */
public class GoogleStorageResourceBucket implements WritableResource {

	private final Storage storage;

	private final String location;

	public GoogleStorageResourceBucket(Storage storage, String location,
			boolean createBucketIfNotExists) {
		Assert.notNull(storage, "Storage object can not be null");
		Assert.notNull(storage, "Path to bucket can not be null");

		this.storage = storage;

		// Ignore trailing slash
		int trailingSlashIndex = location.indexOf('/',
				GoogleStorageProtocolResolver.PROTOCOL.length());
		this.location = location.substring(0,
				trailingSlashIndex < 0 ? location.length() : trailingSlashIndex);

		if (!exists() && createBucketIfNotExists) {
			this.storage.create(BucketInfo.newBuilder(this.location).build());
		}
	}

	@Override
	public boolean exists() {
		return this.storage.get(this.location) != null;
	}

	@Override
	public boolean isReadable() {
		return false;
	}

	@Override
	public boolean isOpen() {
		return false;
	}

	@Override
	public URL getURL() throws IOException {
		return getURI().toURL();
	}

	@Override
	public URI getURI() throws IOException {
		URI uri = null;
		try {
			uri = new URI(this.location);
		}
		catch (URISyntaxException e) {
			throw new IOException("Invalid URI syntax", e);
		}
		return uri;
	}

	@Override
	public File getFile() throws IOException {
		throw new UnsupportedOperationException(
				"Cannot get a file from a storage bucket.");
	}

	@Override
	public long contentLength() throws IOException {
		throw new UnsupportedOperationException(
				"Cannot get content length for a storage bucket.");
	}

	@Override
	public long lastModified() throws IOException {
		throw new UnsupportedOperationException(
				"Cannot get last-modified for a storage bucket.");
	}

	@Override
	public Resource createRelative(String relativePath) throws IOException {
		return new GoogleStorageResourceObject(this.storage,
				this.location + (relativePath.startsWith("/") ? "" : "/") + relativePath);
	}

	@Override
	public String getFilename() {
		return this.location;
	}

	@Override
	public String getDescription() {
		return this.location;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		throw new UnsupportedOperationException(
				"Cannot get input stream for a storage bucket.");
	}

	@Override
	public boolean isWritable() {
		return false;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException(
				"Cannot get output stream for a storage bucket.");
	}
}
