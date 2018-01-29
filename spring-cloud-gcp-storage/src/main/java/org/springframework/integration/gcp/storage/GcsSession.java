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

package org.springframework.integration.gcp.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.file.remote.session.Session;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * @author João André Martins
 */
public class GcsSession implements Session<BlobInfo> {

	private Storage gcs;

	private static final Log LOGGER = LogFactory.getLog(GcsSession.class);

	private static final String SEPARATOR = "/";

	public GcsSession(Storage gcs) {
		Assert.notNull(gcs, "The GCS client can't be null.");
		this.gcs = gcs;
	}

	@Override
	public boolean remove(String path) throws IOException {
		String[] tokens = getBucketAndObjectFromPath(path);
		Assert.state(tokens.length == 1 || tokens.length == 2,
				"Path must be in the form of [bucket] or [bucket]/[blob name]");

		return tokens.length == 1
				? this.gcs.delete(tokens[0])
				: this.gcs.delete(tokens[0], tokens[1]);
	}

	@Override
	public BlobInfo[] list(String path) throws IOException {
		// TODO(joaomartins): Only supports listing buckets, not folders.

		Collection<BlobInfo> blobs = new ArrayList<>();

		for (Blob blob : this.gcs.list(path).iterateAll()) {
			blobs.add(blob);
		}

		return blobs.toArray(new BlobInfo[blobs.size()]);
	}

	@Override
	public void read(String source, OutputStream outputStream) throws IOException {
		String[] tokens = getBucketAndObjectFromPath(source);
		Assert.state(tokens.length == 2, "Can only read files. not buckets.");

		try (OutputStream os = outputStream) {
			os.write(this.gcs.readAllBytes(tokens[0], tokens[1]));
		}
	}

	@Override
	public void write(InputStream inputStream, String destination) throws IOException {
		String[] tokens = getBucketAndObjectFromPath(destination);
		Assert.state(tokens.length == 2, "Can only write to files, not buckets.");

		BlobInfo gcsBlobInfo = BlobInfo.newBuilder(BlobId.of(tokens[0], tokens[1])).build();

		try (InputStream is = inputStream) {
			try (WriteChannel channel = this.gcs.writer(gcsBlobInfo)) {
				channel.write(ByteBuffer.wrap(StreamUtils.copyToByteArray(is)));
			}
		}
	}

	@Override
	public void append(InputStream inputStream, String destination) throws IOException {
		// TODO(joaomartins): We could do compose here, but it assumes that InputStream is first copied to a
		// GCS object that we can then "compose" to the original object.
		throw new UnsupportedOperationException("Appending isn't supported by Google Cloud Storage.");
	}

	@Override
	public boolean mkdir(String directory) throws IOException {
		try {
			this.gcs.create(BucketInfo.of(directory));
			return true;
		}
		catch (StorageException se) {
			LOGGER.info("Error creating the GCS bucket.", se);
			return false;
		}

	}

	@Override
	public boolean rmdir(String directory) throws IOException {
		return this.gcs.delete(directory);
	}

	@Override
	public void rename(String pathFrom, String pathTo) throws IOException {
		String[] fromTokens = getBucketAndObjectFromPath(pathFrom);
		String[] toTokens = getBucketAndObjectFromPath(pathTo);

		// There is currently no way to rename/move things in GCS, so we'll have to copy and remove.
		BlobId source = BlobId.of(fromTokens[0], fromTokens[1]);
		BlobId target = BlobId.of(toTokens[0], toTokens[1]);
		Storage.CopyRequest copyRequest = Storage.CopyRequest.of(source, target);
		this.gcs.copy(copyRequest);

		this.gcs.delete(source);
	}

	@Override
	public void close() {
		// GCS session doesn't need to be closed.
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public boolean exists(String path) throws IOException {
		String[] tokens = getBucketAndObjectFromPath(path);

		return tokens.length == 1
				? this.gcs.get(path) != null
				: this.gcs.get(tokens[0], tokens[1]) != null;
	}

	@Override
	public String[] listNames(String path) throws IOException {
		List<String> names = Stream.of(list(path))
				.map(BlobInfo::getName)
				.collect(Collectors.toList());

		return names.toArray(new String[names.size()]);
	}

	@Override
	public InputStream readRaw(String source) throws IOException {
		String[] tokens = getBucketAndObjectFromPath(source);
		Assert.state(tokens.length == 2, "Can only write to files, not buckets.");
		return Channels.newInputStream(this.gcs.reader(tokens[0], tokens[1]));
	}

	@Override
	public boolean finalizeRaw() throws IOException {
		return true;
	}

	@Override
	public Object getClientInstance() {
		return this.gcs;
	}

	private String[] getBucketAndObjectFromPath(String path) throws IOException {
		// Assumes paths of the form bucket/folder/blob
		Assert.hasText(path, "Path can't be empty.");

		return path.split(SEPARATOR, 2);
	}
}
