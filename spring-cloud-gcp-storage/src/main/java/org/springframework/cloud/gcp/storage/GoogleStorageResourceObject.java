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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.concurrent.TimeUnit;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;

/**
 * Implements {@link WritableResource} for reading and writing objects in Google Cloud
 * Storage.
 *
 * @author Vinicius Carvalho
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 * @author João André Martins
 */
public class GoogleStorageResourceObject implements WritableResource {

	private static final Log LOGGER = LogFactory.getLog(GoogleStorageResourceObject.class);

	private final Storage storage;

	private final String location;

	private final boolean createBlobIfNotExists;

	/**
	 * Constructs the resource representation of a file in Google Cloud Storage.
	 * @param storage the Google Cloud Storage client
	 * @param location the URL of the file, e.g., gs://your-bucket/your-file-name
	 * @param createBlobIfNotExists determines the auto-creation of the file in Google Cloud Storage
	 *                              if an operation that depends on its existence is triggered
	 *                              (e.g., getting the output stream of a file)
	 */
	public GoogleStorageResourceObject(Storage storage, String location,
			boolean createBlobIfNotExists) {
		Assert.notNull(storage, "Storage object can not be null");
		this.storage = storage;
		this.location = location;
		this.createBlobIfNotExists = createBlobIfNotExists;
	}

	public GoogleStorageResourceObject(Storage storage, String location) {
		this(storage, location, true);
	}

	public boolean isCreateBlobIfNotExists() {
		return this.createBlobIfNotExists;
	}

	@Override
	public boolean exists() {
		try {
			return getGcsBlob() != null;
		}
		catch (IOException e) {
			return false;
		}
	}

	@Override
	public boolean isReadable() {
		return true;
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

	private BlobId getBlobId() throws IOException {
		URI uri = getURI();
		return BlobId.of(uri.getAuthority(),
				uri.getPath().substring(1, uri.getPath().length()));
	}

	/**
	 * Gets the underlying storage object in Google Cloud Storage.
	 * @return The storage object, will be null if it does not exist in Google Cloud Storage.
	 * @throws IOException
	 */
	public Blob getGcsBlob() throws IOException {
		return this.storage.get(getBlobId());
	}

	/**
	 * Creates a signed URL to an object, if it exists. This method will fail if this storage
	 * resource was not created using service account credentials.
	 * @param timeUnit the time unit used to determine how long the URL is valid.
	 * @param timePeriods the number of periods to determine how long the URL is valid.
	 * @param options specifies additional options for signing URLs
	 * @return the URL if the object exists, and null if it does not.
	 * @throws IOException
	 */
	public URL createSignedUrl(TimeUnit timeUnit, long timePeriods,
			Storage.SignUrlOption... options) throws IOException {
		Assert.notNull(options, "options can't be null.");

		Blob blob = this.getGcsBlob();

		if (blob == null && LOGGER.isWarnEnabled()) {
			LOGGER.warn("Creating signed URL for non-existing GCS object " + this.location);
		}

		return this.storage.signUrl(
				BlobInfo.newBuilder(getBlobId()).build(), timePeriods, timeUnit, options);
	}

	/**
	 * Creates the blob that this {@link GoogleStorageResourceObject} represents in Google Cloud
	 * Storage.
	 * @return the created blob object; or {@code null} if the object already exists, cannot be
	 * created or its bucket doesn't exist and cannot be created
	 * @throws IOException
	 */
	public Blob create() throws IOException {
		GoogleStorageResourceBucket bucket = getResourceBucket();

		if (!bucket.exists()) {
			if (!bucket.isCreateBucketIfNotExists()) {
				throw new IllegalStateException("The bucket " + bucket.getFilename()
						+ " doesn't exist and couldn't be created");
			}

			bucket.create();
		}

		return this.storage.create(BlobInfo.newBuilder(getBlobId()).build());
	}

	private Blob throwExceptionForNullBlob(Blob blob) throws IOException {
		if (blob == null) {
			throw new FileNotFoundException("The blob was not found: " + this.location);
		}
		return blob;
	}

	@Override
	public File getFile() throws IOException {
		throw new UnsupportedOperationException(
				getDescription() + " cannot be resolved to absolute file path");
	}

	@Override
	public long contentLength() throws IOException {
		return throwExceptionForNullBlob(getGcsBlob()).getSize();
	}

	@Override
	public long lastModified() throws IOException {
		return throwExceptionForNullBlob(getGcsBlob()).getUpdateTime();
	}

	/**
	 * Creates a file in the same bucket as this one. It inherits {@code createBlobIfNotExists}
	 * from this object.
	 *
	 * <p>The file is created if it didn't exist in Google Cloud Storage.
	 * @param relativePath the URL to a Google Cloud Storage file
	 * @return the {@link GoogleStorageResourceObject} object for the created file
	 * @throws IOException
	 */
	@Override
	public GoogleStorageResourceObject createRelative(String relativePath) throws IOException {
		int lastSlashIndex = this.location.lastIndexOf("/");
		String absolutePath = this.location.substring(0, lastSlashIndex + 1) + relativePath;

		GoogleStorageResourceObject blobResource = new GoogleStorageResourceObject(
				this.storage, absolutePath, this.createBlobIfNotExists);

		if (!blobResource.exists()) {
			blobResource.create();
		}

		return blobResource;
	}

	@Override
	public String getFilename() {
		try {
			return throwExceptionForNullBlob(getGcsBlob()).getName();
		}
		catch (IOException e) {
			return null;
		}
	}

	@Override
	public String getDescription() {
		return this.location;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return Channels.newInputStream(throwExceptionForNullBlob(getGcsBlob()).reader());
	}

	@Override
	public boolean isWritable() {
		return true;
	}

	/**
	 * Returns the output stream for a Google Cloud Storage file.
	 * @return the object's output stream or {@code null} if the object doesn't exist and cannot be
	 * created
	 * @throws IOException
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		Blob blob = getGcsBlob();

		if (blob == null || !blob.exists()) {
			if (!this.createBlobIfNotExists) {
				throw new FileNotFoundException("The blob was not found: " + this.location);
			}

			blob = create();
		}

		return Channels.newOutputStream(blob.writer());
	}

	GoogleStorageResourceBucket getResourceBucket() throws IOException {
		return new GoogleStorageResourceBucket(
				this.storage, getBlobId().getBucket(), this.createBlobIfNotExists);
	}
}
