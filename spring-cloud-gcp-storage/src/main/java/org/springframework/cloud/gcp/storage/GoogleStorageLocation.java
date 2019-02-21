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

import org.springframework.util.Assert;

/**
 * Represents a Google Cloud Storage location provided by the user.
 *
 * @author Daniel Zou
 */
public final class GoogleStorageLocation {

	private static final String GCS_URI_FORMAT = "gs://%s/%s";

	private final String bucketName;

	private final String blobName;

	private final URI uri;

	public GoogleStorageLocation(String gcsLocationUriString) {
		try {
			URI locationUri = new URI(gcsLocationUriString);

			Assert.isTrue(
					gcsLocationUriString.startsWith("gs://"),
					"A Google Storage URI must start with gs://");

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
			throw new IllegalArgumentException(
					"Invalid location: " + gcsLocationUriString);
		}
	}

	/**
	 * Returns the Google Storage bucket name.
	 */
	public String getBucketName() {
		return bucketName;
	}

	/**
	 * Returns the path to the blob/folder relative from the bucket root. Returns the empty
	 * String if the {@link GoogleStorageLocation} specifies a bucket itself.
	 */
	public String getBlobName() {
		return blobName;
	}

	/**
	 * Returns the GCS URI of the location.
	 */
	public URI uri() {
		return this.uri;
	}

	/**
	 * Returns the Google Storage URI string for the location.
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
		String uri = String.format(GCS_URI_FORMAT, bucketName, "");
		return new GoogleStorageLocation(uri);
	}

	/**
	 * Returns a {@link GoogleStorageLocation} for a file within a bucket.
	 * @param bucketName name of the GCS bucket
	 * @param pathToFile path to the file within the bucket
	 * @return the {@link GoogleStorageLocation} to the location.
	 */
	public static GoogleStorageLocation forFile(String bucketName, String pathToFile) {
		String uri = String.format(GCS_URI_FORMAT, bucketName, pathToFile);
		return new GoogleStorageLocation(uri);
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

		String uri = String.format(GCS_URI_FORMAT, bucketName, pathToFolder);
		return new GoogleStorageLocation(uri);
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
