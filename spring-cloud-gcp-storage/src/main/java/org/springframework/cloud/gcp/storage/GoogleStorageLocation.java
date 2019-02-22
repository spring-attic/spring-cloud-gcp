/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.storage;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Represents a Google Cloud Storage location provided by the user.
 *
 * @author Daniel Zou
 *
 * @since 1.2
 */
public class GoogleStorageLocation {

	private static final String GCS_URI_FORMAT = "gs://%s/%s";

	private final String bucketName;

	private final String blobName;

	private final URI uri;

	/**
	 * Constructs a {@link GoogleStorageLocation} based on the provided Google Storage URI string.
	 * The URI string is of the form: {@code gs://<BUCKET_NAME>/<PATH_TO_FILE>}
	 *
	 * @param gcsLocationUriString a Google Storage URI string to a bucket/folder/file.
	 */
	public GoogleStorageLocation(String gcsLocationUriString) {
		try {
			Assert.isTrue(
					gcsLocationUriString.startsWith("gs://"),
					"A Google Storage URI must start with gs://");

			URI locationUri = new URI(gcsLocationUriString);

			Assert.isTrue(
					locationUri.getAuthority() != null,
					"No bucket specified in the location: " + gcsLocationUriString);

			this.bucketName = locationUri.getAuthority();
			this.blobName = getBlobPathFromUri(locationUri);

			// ensure that if it's a bucket handle, location ends with a '/'
			if (this.blobName == null && !gcsLocationUriString.endsWith("/")) {
				locationUri = new URI(gcsLocationUriString + "/");
			}
			this.uri = locationUri;
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid location: " + gcsLocationUriString, e);
		}
	}

	/**
	 * Constructs a {@link GoogleStorageLocation} based on the provided {@code bucketName} and
	 * {@code pathToFile}.
	 *
	 * @param bucketName name of the Google Storage bucket
	 * @param pathToFile path to the file or folder in the bucket; set as null if
	 *    the location is to the bucket itself.
	 */
	public GoogleStorageLocation(String bucketName, @Nullable String pathToFile) {
		try {
			this.bucketName = bucketName;
			this.blobName = pathToFile;

			pathToFile = (pathToFile == null) ? "" : pathToFile;
			this.uri = new URI(String.format(GCS_URI_FORMAT, bucketName, pathToFile));
		}
		catch (URISyntaxException e) {
			String errorMessage = String.format(
					"Invalid location provided. bucketName: %s, pathToFile: %s", bucketName, pathToFile);
			throw new IllegalArgumentException(errorMessage, e);
		}
	}

	/**
	 * Check if the location references a bucket and not a blob.
	 * @return if the location describes a bucket
	 */
	public boolean isBucket() {
		return this.blobName == null;
	}

	/**
	 * Returns whether this {@link GoogleStorageLocation} represents a file or not.
	 * @return true if the location describes a file
	 */
	public boolean isFile() {
		return this.blobName != null && !this.blobName.endsWith("/");
	}

	/**
	 * Returns the Google Storage bucket name.
	 *
	 * @return the name of the Google Storage bucket
	 */
	public String getBucketName() {
		return bucketName;
	}

	/**
	 * Returns the path to the blob/folder relative from the bucket root. Returns null
	 * if the {@link GoogleStorageLocation} specifies a bucket itself.
	 *
	 * @return a path to the blob or folder; null if the location is to a bucket.
	 */
	public String getBlobName() {
		return blobName;
	}

	/**
	 * Returns the GCS URI of the location.
	 *
	 * @return the URI object of the Google Storage location.
	 */
	public URI uri() {
		return this.uri;
	}

	/**
	 * Returns the Google Storage URI string for the location.
	 *
	 * @return the URI string of the Google Storage location.
	 */
	public String uriString() {
		String processedBlobName = (blobName == null) ? "" : blobName;
		return String.format(GCS_URI_FORMAT, bucketName, processedBlobName);
	}

	/**
	 * Returns a {@link GoogleStorageLocation} to a bucket.
	 * @param bucketName name of the GCS bucket
	 * @return the {@link GoogleStorageLocation} to the location.
	 */
	public static GoogleStorageLocation forBucket(String bucketName) {
		return new GoogleStorageLocation(bucketName, null);
	}

	/**
	 * Returns a {@link GoogleStorageLocation} for a file within a bucket.
	 * @param bucketName name of the GCS bucket
	 * @param pathToFile path to the file within the bucket
	 * @return the {@link GoogleStorageLocation} to the location.
	 */
	public static GoogleStorageLocation forFile(String bucketName, String pathToFile) {
		return new GoogleStorageLocation(bucketName, pathToFile);
	}

	/**
	 * Returns a {@link GoogleStorageLocation} to a folder whose path is relative to the
	 * bucket.
	 * @param bucketName name of the GCS bucket.
	 * @param pathToFolder path to the folder within the bucket.
	 * @return the {@link GoogleStorageLocation} to the location.
	 */
	public static GoogleStorageLocation forFolder(String bucketName, String pathToFolder) {
		if (!pathToFolder.endsWith("/")) {
			pathToFolder += "/";
		}
		return new GoogleStorageLocation(bucketName, pathToFolder);
	}

	@Override
	public String toString() {
		return uriString();
	}

	private static String getBlobPathFromUri(URI gcsUri) {
		String uriPath = gcsUri.getPath();
		if (uriPath.isEmpty() || uriPath.equals("/")) {
			// This indicates that the path specifies the root of the bucket
			return null;
		}
		else {
			return uriPath.substring(1);
		}
	}
}
