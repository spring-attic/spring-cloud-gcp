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

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;

import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;

/**
 * Implements {@link WritableResource} for buckets in Google Cloud Storage.
 *
 * @author Chengyuan Zhao
 */
public class GoogleStorageResourceBucket implements WritableResource {

	private final Storage storage;

	private final String bucketName;

	private final boolean createBucketIfRequired;

	public GoogleStorageResourceBucket(Storage storage, String bucketName,
			boolean createBucketIfNotExists) {
		Assert.notNull(storage, "Storage object can not be null");
		Assert.notNull(bucketName, "Bucket name can not be null");

		this.storage = storage;
		this.bucketName = bucketName;
		this.createBucketIfRequired = createBucketIfNotExists;
	}

	public void createBucket() {
		this.storage.create(BucketInfo.newBuilder(this.bucketName).build());
	}

	@Override
	public boolean exists() {
		return resolve() != null;
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
			uri = new URI(GoogleStorageProtocolResolver.PROTOCOL + getFilename());
		}
		catch (URISyntaxException e) {
			throw new IOException("Invalid URI syntax", e);
		}
		return uri;
	}

	@Override
	public File getFile() {
		throw new UnsupportedOperationException(
				"GCS bucket is not in the local filesystem.");
	}

	@Override
	public long contentLength() {
		throw new UnsupportedOperationException(
				"Cannot get content length for a storage bucket.");
	}

	@Override
	public long lastModified() {
		throw new UnsupportedOperationException(
				"Cannot get last-modified for a storage bucket.");
	}

	@Override
	public Resource createRelative(String relativePath) throws IOException {
		return new GoogleStorageResourceObject(this.storage,
				getURI().toString() + (relativePath.startsWith("/") ? "" : "/")
						+ relativePath,
				this.createBucketIfRequired);
	}

	@Override
	public String getFilename() {
		return this.bucketName;
	}

	@Override
	public String getDescription() {
		return this.bucketName;
	}

	@Override
	public InputStream getInputStream() {
		throw new UnsupportedOperationException(
				"Cannot get input stream for a storage bucket.");
	}

	@Override
	public boolean isWritable() {
		return false;
	}

	@Override
	public OutputStream getOutputStream() {
		throw new UnsupportedOperationException(
				"Cannot get output stream for a storage bucket.");
	}

	private Bucket resolve() {
		return this.storage.get(this.bucketName);
	}
}
