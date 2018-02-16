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

import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;

/**
 * Implements {@link WritableResource} for buckets in Google Cloud Storage.
 *
 * @author Chengyuan Zhao
 * @author João André Martins
 */
public class GoogleStorageResourceBucket implements WritableResource {

	private final Storage storage;

	private final String bucketName;

	private final boolean createBucketIfNotExists;

	/**
	 * Constructs the resource representation of a GCS bucket.
	 * @param storage the Google Cloud Storage client
	 * @param bucketName the name of the bucket
	 * @param createBucketIfNotExists determines bucket auto-creation when another operation that
	 *                                depends on the existence of this bucket is executed (e.g.,
	 *                                creating a file in the bucket)
	 */
	public GoogleStorageResourceBucket(Storage storage, String bucketName,
			boolean createBucketIfNotExists) {
		Assert.notNull(storage, "Storage object can not be null");
		Assert.notNull(bucketName, "Bucket name can not be null");

		this.storage = storage;
		this.bucketName = bucketName;
		this.createBucketIfNotExists = createBucketIfNotExists;
	}

	/**
	 * Creates the bucket that this {@link GoogleStorageResourceBucket} represents in Google Cloud
	 * Storage.
	 * @return the {@link com.google.cloud.storage.Bucket} object for the bucket
	 * @throws com.google.cloud.storage.StorageException if any errors during bucket creation arise,
	 * such as if the bucket already exists
	 */
	public Bucket create() {
		return this.storage.create(BucketInfo.newBuilder(this.bucketName).build());
	}

	@Override
	public boolean exists() {
		return getGcsBucket() != null;
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

	/**
	 * Creates a file within a given bucket. The file resource inherits
	 * {@code createBlobIfNotExists} from this bucket's {@code createBucketIfNotExists}.
	 *
	 * <p>If the file did not exist in Google Cloud Storage, it is created by this method.
	 * @param relativePath the URL for a Google Cloud Storage bucket
	 * @return the {@link GoogleStorageResourceObject} object for the created file
	 * @throws IOException
	 */
	@Override
	public GoogleStorageResourceObject createRelative(String relativePath) throws IOException {
		GoogleStorageResourceObject resource = new GoogleStorageResourceObject(this.storage,
				getURI().toString() + (relativePath.startsWith("/") ? "" : "/")
						+ relativePath,
				this.createBucketIfNotExists);
		if (!resource.exists()) {
			resource.create();
		}

		return resource;
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

	private Bucket getGcsBucket() {
		return this.storage.get(this.bucketName);
	}

	public boolean isCreateBucketIfNotExists() {
		return this.createBucketIfNotExists;
	}
}
