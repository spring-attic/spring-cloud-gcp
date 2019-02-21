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

package org.springframework.cloud.gcp.vision;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.util.Assert;

/**
 * Represents a Google Cloud Storage location provided by the user.
 *
 * @author Daniel Zou
 */
public final class GoogleStorageLocation {

	private static final String BUCKET_URI_FORMAT = "gs://%s/";

	private static final String FILE_URI_FORMAT = "gs://%s/%s";

	private static final String FOLDER_URI_FORMAT = "gs://%s/%s/";

	private final String bucketName;

	private final String gcsPath;

	GoogleStorageLocation(String gcsLocationUriString) {
		try {
			URI locationUri = new URI(gcsLocationUriString);

			Assert.isTrue(
					gcsLocationUriString.startsWith("gs://"),
					"A Google Storage URI must start with gs://");

			Assert.isTrue(
					locationUri.getAuthority() != null,
					"The Google Storage URI is missing a bucket: " + gcsLocationUriString);

			this.bucketName = locationUri.getAuthority();
			this.gcsPath = getBlobPathFromUri(locationUri);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(
					"Unable to parse GCS location URI: " + gcsLocationUriString);
		}
	}

	/**
	 * Returns the Google Storage bucket name.
	 */
	public String getBucketName() {
		return bucketName;
	}

	/**
	 * Returns the path to the blob/folder relative from the bucket root. Returns the empty String
	 * if the {@link GoogleStorageLocation} specifies a bucket itself.
	 */
	public String getGcsPath() {
		return gcsPath;
	}

	/**
	 * Returns the Google Storage URI string for the location.
	 */
	public String getUriString() {
		return String.format(FILE_URI_FORMAT, bucketName, gcsPath);
	}

	/**
	 * Returns whether the Google Storage location is a file.
	 *
	 * @return true if the {@link GoogleStorageLocation} is the location of a file.
	 */
	public boolean isFile() {
		return !gcsPath.isEmpty() && !gcsPath.endsWith("/");
	}

	/**
	 * Returns a {@link GoogleStorageLocation} to a bucket.
	 * @param bucketName name of the GCS bucket
	 * @return the {@link GoogleStorageLocation} to the location.
	 */
	public static GoogleStorageLocation forBucket(String bucketName) {
		String uri = String.format(BUCKET_URI_FORMAT, bucketName);
		return new GoogleStorageLocation(uri);
	}

	/**
	 * Returns a {@link GoogleStorageLocation} for a file within a bucket.
	 * @param bucketName name of the GCS bucket
	 * @param filePath path to the file within the bucket
	 * @return the {@link GoogleStorageLocation} to the location.
	 */
	public static GoogleStorageLocation forFile(String bucketName, String filePath) {
		String uri = String.format(FILE_URI_FORMAT, bucketName, filePath);
		return new GoogleStorageLocation(uri);
	}

	/**
	 * Returns a {@link GoogleStorageLocation} to a folder whose path is relative to the bucket.
	 * @param bucketName name of the GCS bucket.
	 * @param folderPath path to the folder within the bucket.
	 * @return the {@link GoogleStorageLocation} to the location.
	 */
	public static GoogleStorageLocation forFolder(String bucketName, String folderPath) {
		String uri = String.format(FOLDER_URI_FORMAT, bucketName, folderPath);
		return new GoogleStorageLocation(uri);
	}

	@Override
	public String toString() {
		return "GoogleStorageLocation{" +
				"bucketName='" + bucketName + '\'' +
				", blobPath='" + gcsPath + '\'' +
				'}';
	}

	private static String getBlobPathFromUri(URI gcsUri) {
		String uriPath = gcsUri.getPath();
		if (uriPath.isEmpty()) {
			// This indicates that the path specifies the root of the bucket
			return "";
		} else {
			return uriPath.substring(1);
		}
	}
}
