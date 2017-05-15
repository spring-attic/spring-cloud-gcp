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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;

/**
 * @author Vinicius Carvalho
 */
public class GoogleStorageResource implements Resource {

	private final Storage storage;

	private final String location;

	private Blob blob;

	private final Object monitor = new Object();

	public GoogleStorageResource(Storage storage, String location) {
		Assert.notNull(storage, "Storage object can not be null");
		this.storage = storage;
		this.location = location;
	}

	@Override
	public boolean exists() {
		try {
			return resolve().exists();
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

	private Blob resolve() throws IOException {
		synchronized (this.monitor) {
			if (this.blob == null) {
				try {
					URI uri = getURI();
					BlobId blobId = BlobId.of(uri.getHost(), uri.getPath().substring(1, uri.getPath().length()));
					this.blob = this.storage.get(blobId);
					return this.blob;
				}
				catch (Exception e) {
					throw new IOException("Failed to open remote connection to " + location, e);
				}
			}
		}
		return this.blob;
	}

	@Override
	public File getFile() throws IOException {
		throw new UnsupportedOperationException(getDescription() + " cannot be resolved to absolute file path");
	}

	@Override
	public long contentLength() throws IOException {
		return resolve().getSize();
	}

	@Override
	public long lastModified() throws IOException {
		return resolve().getUpdateTime();
	}

	@Override
	public Resource createRelative(String relativePath) throws IOException {
		throw new UnsupportedOperationException("Directory creation not supported");
	}

	@Override
	public String getFilename() {
		try {
			return resolve().getName();
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
		return Channels.newInputStream(resolve().reader());
	}
}
